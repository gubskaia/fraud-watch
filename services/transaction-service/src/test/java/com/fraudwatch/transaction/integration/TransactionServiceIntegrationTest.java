package com.fraudwatch.transaction.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.fraud.FraudDecisionPayload;
import com.fraudwatch.events.review.ReviewDecisionMadePayload;
import com.fraudwatch.transaction.config.RabbitConfig;
import com.fraudwatch.transaction.domain.TransactionStatus;
import com.fraudwatch.transaction.dto.CreateTransactionRequest;
import com.fraudwatch.transaction.dto.TransactionResponse;
import com.fraudwatch.transaction.repository.IdempotencyRecordRepository;
import com.fraudwatch.transaction.repository.TransactionRepository;
import com.fraudwatch.transaction.service.TransactionLifecycleService;
import com.fraudwatch.transaction.service.TransactionService;
import com.fraudwatch.test.InfrastructureContainers;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class TransactionServiceIntegrationTest {

    private static final String STATUS_CHANGED_TEST_QUEUE = "fraudwatch.transaction.status-changed.test";

    @Container
    static final PostgreSQLContainer<?> postgres = InfrastructureContainers.postgres("transaction_db_test");

    @Container
    static final RabbitMQContainer rabbitMq = InfrastructureContainers.rabbitMq();

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionLifecycleService transactionLifecycleService;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitMq::getHost);
        registry.add("spring.rabbitmq.port", rabbitMq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMq::getAdminPassword);
    }

    @BeforeEach
    void purgeQueues() {
        rabbitTemplate.execute(channel -> {
            channel.queuePurge(RabbitConfig.TRANSACTION_CREATED_QUEUE);
            channel.queuePurge(RabbitConfig.FRAUD_APPROVED_QUEUE);
            channel.queuePurge(RabbitConfig.FRAUD_BLOCKED_QUEUE);
            channel.queuePurge(RabbitConfig.FRAUD_REVIEW_REQUIRED_QUEUE);
            channel.queuePurge(RabbitConfig.REVIEW_DECISION_QUEUE);
            channel.queueDeclare(STATUS_CHANGED_TEST_QUEUE, true, false, false, null);
            channel.queueBind(
                STATUS_CHANGED_TEST_QUEUE,
                RabbitConfig.TRANSACTION_EXCHANGE,
                RabbitConfig.TRANSACTION_STATUS_CHANGED_ROUTING_KEY
            );
            channel.queuePurge(STATUS_CHANGED_TEST_QUEUE);
            return null;
        });
    }

    @Test
    void shouldCreateTransactionIdempotentlyAndPublishCreatedEventOnce() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
            1L,
            new BigDecimal("245.90"),
            "usd",
            "debit",
            "Steam Market",
            "DIGITAL_GOODS",
            "device-it-42",
            "Integration test purchase"
        );

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("X-Forwarded-For", "203.0.113.10");
        httpRequest.setRemoteAddr("127.0.0.1");

        TransactionResponse firstResponse = transactionService.createTransaction(
            "idem-it-1",
            "corr-it-1",
            request,
            httpRequest
        );
        TransactionResponse secondResponse = transactionService.createTransaction(
            "idem-it-1",
            "corr-it-2",
            request,
            httpRequest
        );

        assertThat(secondResponse.id()).isEqualTo(firstResponse.id());
        assertThat(secondResponse.transactionReference()).isEqualTo(firstResponse.transactionReference());
        assertThat(transactionRepository.findAll()).hasSize(1);
        assertThat(idempotencyRecordRepository.findAll()).hasSize(1);

        Message publishedMessage = rabbitTemplate.receive(RabbitConfig.TRANSACTION_CREATED_QUEUE, 5_000);
        assertThat(publishedMessage).isNotNull();

        JsonNode event = objectMapper.readTree(publishedMessage.getBody());
        assertThat(event.path("eventType").asText()).isEqualTo("TransactionCreated");
        assertThat(event.path("correlationId").asText()).isEqualTo("corr-it-1");
        assertThat(event.path("payload").path("transactionId").asLong()).isEqualTo(firstResponse.id());
        assertThat(event.path("payload").path("transactionReference").asText())
            .isEqualTo(firstResponse.transactionReference());
        assertThat(event.path("payload").path("accountId").asLong()).isEqualTo(1L);
        assertThat(event.path("payload").path("deviceId").asText()).isEqualTo("device-it-42");
        assertThat(event.path("payload").path("ipAddress").asText()).isEqualTo("203.0.113.10");

        Message duplicateMessage = rabbitTemplate.receive(RabbitConfig.TRANSACTION_CREATED_QUEUE, 250);
        assertThat(duplicateMessage).isNull();
    }

    @Test
    void shouldProcessFraudAndReviewEventsAndPublishStatusChanges() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
            1L,
            new BigDecimal("510.00"),
            "USD",
            "DEBIT",
            "Risky Merchant",
            "ECOM",
            "device-it-84",
            "Scenario transaction"
        );
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("X-Forwarded-For", "198.51.100.84");
        httpRequest.setRemoteAddr("127.0.0.1");

        TransactionResponse transaction = transactionService.createTransaction(
            "idem-it-lifecycle",
            "corr-it-lifecycle",
            request,
            httpRequest
        );

        transactionLifecycleService.applyFraudDecision(
            fraudDecisionEvent(
                transaction.id(),
                transaction.transactionReference(),
                "UNDER_REVIEW",
                "corr-fraud-review"
            )
        );

        awaitTransactionStatus(transaction.id(), TransactionStatus.UNDER_REVIEW);

        transactionLifecycleService.applyReviewDecision(
            reviewDecisionEvent(
                transaction.id(),
                transaction.transactionReference(),
                "BLOCKED",
                "corr-review-final"
            )
        );

        awaitTransactionStatus(transaction.id(), TransactionStatus.BLOCKED);

        JsonNode firstStatusEvent = readStatusChangedEvent();
        JsonNode secondStatusEvent = readStatusChangedEvent();

        assertThat(firstStatusEvent.path("payload").path("previousStatus").asText()).isEqualTo("PENDING_REVIEW");
        assertThat(firstStatusEvent.path("payload").path("newStatus").asText()).isEqualTo("UNDER_REVIEW");
        assertThat(firstStatusEvent.path("correlationId").asText()).isEqualTo("corr-fraud-review");

        assertThat(secondStatusEvent.path("payload").path("previousStatus").asText()).isEqualTo("UNDER_REVIEW");
        assertThat(secondStatusEvent.path("payload").path("newStatus").asText()).isEqualTo("BLOCKED");
        assertThat(secondStatusEvent.path("correlationId").asText()).isEqualTo("corr-review-final");
    }

    private EventEnvelope<FraudDecisionPayload> fraudDecisionEvent(
        Long transactionId,
        String transactionReference,
        String decision,
        String correlationId
    ) {
        return new EventEnvelope<>(
            UUID.randomUUID().toString(),
            "TransactionReviewRequired",
            "v1",
            Instant.parse("2026-06-19T08:00:00Z"),
            correlationId,
            Map.of("service", "fraud-service"),
            new FraudDecisionPayload(
                transactionId,
                transactionReference,
                1L,
                42,
                decision,
                List.of("RULE_A"),
                List.of("Needs analyst review"),
                Instant.parse("2026-06-19T08:00:00Z")
            )
        );
    }

    private EventEnvelope<ReviewDecisionMadePayload> reviewDecisionEvent(
        Long transactionId,
        String transactionReference,
        String finalDecision,
        String correlationId
    ) {
        return new EventEnvelope<>(
            UUID.randomUUID().toString(),
            "ReviewDecisionMade",
            "v1",
            Instant.parse("2026-06-19T08:05:00Z"),
            correlationId,
            Map.of("service", "review-service"),
            new ReviewDecisionMadePayload(
                77L,
                transactionId,
                transactionReference,
                finalDecision,
                "CONFIRMED_FRAUD",
                "analyst-it",
                Instant.parse("2026-06-19T08:05:00Z")
            )
        );
    }

    private void awaitTransactionStatus(Long transactionId, TransactionStatus expectedStatus) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            TransactionStatus currentStatus = transactionRepository.findDetailedById(transactionId)
                .map(found -> found.getStatus())
                .orElse(null);
            if (currentStatus == expectedStatus) {
                return;
            }
            Thread.sleep(100);
        }

        TransactionStatus actualStatus = transactionRepository.findDetailedById(transactionId)
            .map(found -> found.getStatus())
            .orElse(null);
        assertThat(actualStatus).isEqualTo(expectedStatus);
    }

    private JsonNode readStatusChangedEvent() throws Exception {
        Message message = rabbitTemplate.receive(STATUS_CHANGED_TEST_QUEUE, 5_000);
        assertThat(message).isNotNull();
        return objectMapper.readTree(message.getBody());
    }
}
