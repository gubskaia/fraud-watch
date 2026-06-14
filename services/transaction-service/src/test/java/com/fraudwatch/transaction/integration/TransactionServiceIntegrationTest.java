package com.fraudwatch.transaction.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.transaction.config.RabbitConfig;
import com.fraudwatch.transaction.dto.CreateTransactionRequest;
import com.fraudwatch.transaction.dto.TransactionResponse;
import com.fraudwatch.transaction.repository.IdempotencyRecordRepository;
import com.fraudwatch.transaction.repository.TransactionRepository;
import com.fraudwatch.transaction.service.TransactionService;
import com.fraudwatch.test.InfrastructureContainers;
import java.math.BigDecimal;
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

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitMq::getHost);
        registry.add("spring.rabbitmq.port", rabbitMq::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "fraudwatch");
        registry.add("spring.rabbitmq.password", () -> "fraudwatch");
    }

    @BeforeEach
    void purgeQueue() {
        rabbitTemplate.execute(channel -> {
            channel.queuePurge(RabbitConfig.TRANSACTION_CREATED_QUEUE);
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
}
