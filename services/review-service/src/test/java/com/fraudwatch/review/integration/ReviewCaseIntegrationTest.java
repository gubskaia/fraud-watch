package com.fraudwatch.review.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.fraud.FraudDecisionPayload;
import com.fraudwatch.review.config.RabbitConfig;
import com.fraudwatch.review.domain.FraudCase;
import com.fraudwatch.review.domain.FraudCaseStatus;
import com.fraudwatch.review.dto.ReviewDecisionRequest;
import com.fraudwatch.review.messaging.FraudDecisionListener;
import com.fraudwatch.review.repository.FraudCaseRepository;
import com.fraudwatch.review.service.ReviewCaseService;
import com.fraudwatch.test.InfrastructureContainers;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ReviewCaseIntegrationTest {

    private static final String REVIEW_EVENT_TEST_QUEUE = "fraudwatch.review.decision.test";

    @Container
    static final PostgreSQLContainer<?> postgres = InfrastructureContainers.postgres("review_db_test");

    @Container
    static final RabbitMQContainer rabbitMq = InfrastructureContainers.rabbitMq();

    @Autowired
    private FraudDecisionListener fraudDecisionListener;

    @Autowired
    private ReviewCaseService reviewCaseService;

    @Autowired
    private FraudCaseRepository fraudCaseRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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
            channel.queuePurge(RabbitConfig.REVIEW_REQUIRED_QUEUE);
            channel.queuePurge(RabbitConfig.REVIEW_REQUIRED_DLQ);
            channel.queueDeclare(REVIEW_EVENT_TEST_QUEUE, true, false, false, null);
            channel.queueBind(
                REVIEW_EVENT_TEST_QUEUE,
                RabbitConfig.REVIEW_EXCHANGE,
                RabbitConfig.REVIEW_DECISION_ROUTING_KEY
            );
            channel.queuePurge(REVIEW_EVENT_TEST_QUEUE);
            return null;
        });
    }

    @Test
    void shouldCreateCaseFromFraudDecisionAndPublishFinalReviewDecision() throws Exception {
        fraudDecisionListener.handle(objectMapper.writeValueAsBytes(reviewRequiredEvent()));

        FraudCase fraudCase = fraudCaseRepository.findByTransactionId(101L).orElseThrow();
        assertThat(fraudCase.getStatus()).isEqualTo(FraudCaseStatus.OPEN);
        assertThat(fraudCase.getTriggeredRules()).contains("RULE_A").contains("RULE_B");

        reviewCaseService.approveCase(
            fraudCase.getId(),
            new ReviewDecisionRequest("analyst-it", "LEGIT_ACTIVITY", "Confirmed after manual review")
        );

        FraudCase updatedCase = fraudCaseRepository.findDetailedById(fraudCase.getId()).orElseThrow();
        assertThat(updatedCase.getStatus()).isEqualTo(FraudCaseStatus.APPROVED);
        assertThat(updatedCase.getAssignedTo()).isEqualTo("analyst-it");
        assertThat(updatedCase.getReasonCode().getCode()).isEqualTo("LEGIT_ACTIVITY");
        assertThat(updatedCase.getDecisionAt()).isNotNull();

        Message publishedMessage = rabbitTemplate.receive(REVIEW_EVENT_TEST_QUEUE, 5_000);
        assertThat(publishedMessage).isNotNull();

        JsonNode event = objectMapper.readTree(publishedMessage.getBody());
        assertThat(event.path("eventType").asText()).isEqualTo("ReviewDecisionMade");
        assertThat(event.path("payload").path("transactionId").asLong()).isEqualTo(101L);
        assertThat(event.path("payload").path("transactionReference").asText()).isEqualTo("tx-review-101");
        assertThat(event.path("payload").path("finalDecision").asText()).isEqualTo("APPROVED");
        assertThat(event.path("payload").path("reasonCode").asText()).isEqualTo("LEGIT_ACTIVITY");
        assertThat(event.path("payload").path("analyst").asText()).isEqualTo("analyst-it");
    }

    @Test
    void shouldDeadLetterInvalidReviewRequiredEvent() {
        rabbitTemplate.send(
            RabbitConfig.FRAUD_EXCHANGE,
            RabbitConfig.REVIEW_REQUIRED_ROUTING_KEY,
            new Message("not-json".getBytes(), new MessageProperties())
        );

        Message deadLetterMessage = receiveEventually(RabbitConfig.REVIEW_REQUIRED_DLQ, 10_000);
        assertThat(deadLetterMessage).isNotNull();
        assertThat(new String(deadLetterMessage.getBody())).isEqualTo("not-json");
        assertThat(fraudCaseRepository.findAll()).isEmpty();
    }

    private EventEnvelope<FraudDecisionPayload> reviewRequiredEvent() {
        return new EventEnvelope<>(
            UUID.randomUUID().toString(),
            "TransactionReviewRequired",
            "v1",
            Instant.parse("2026-06-19T09:00:00Z"),
            "corr-review-it",
            Map.of("service", "fraud-service"),
            new FraudDecisionPayload(
                101L,
                "tx-review-101",
                501L,
                48,
                "UNDER_REVIEW",
                List.of("RULE_A", "RULE_B"),
                List.of("Velocity threshold exceeded", "Unusual merchant category"),
                Instant.parse("2026-06-19T09:00:00Z")
            )
        );
    }

    private Message receiveEventually(String queueName, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            Message message = rabbitTemplate.receive(queueName, 500);
            if (message != null) {
                return message;
            }
        }
        return null;
    }
}
