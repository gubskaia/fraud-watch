package com.fraudwatch.fraud.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fraudwatch.events.transaction.TransactionCreatedPayload;
import com.fraudwatch.fraud.domain.FraudDecision;
import com.fraudwatch.fraud.domain.FraudDecisionStatus;
import com.fraudwatch.fraud.repository.FraudDecisionRepository;
import com.fraudwatch.fraud.service.FraudScoringService;
import com.fraudwatch.test.InfrastructureContainers;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class FraudScoringServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = InfrastructureContainers.postgres("fraud_db_test");

    @Container
    static final GenericContainer<?> redis = InfrastructureContainers.redis();

    @Container
    static final RabbitMQContainer rabbitMq = InfrastructureContainers.rabbitMq();

    @Autowired
    private FraudScoringService fraudScoringService;

    @Autowired
    private FraudDecisionRepository fraudDecisionRepository;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.rabbitmq.host", rabbitMq::getHost);
        registry.add("spring.rabbitmq.port", rabbitMq::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "fraudwatch");
        registry.add("spring.rabbitmq.password", () -> "fraudwatch");
    }

    @Test
    void shouldPersistBlockedDecisionWhenLargeAmountAndNewDeviceRulesTrigger() {
        TransactionCreatedPayload payload = new TransactionCreatedPayload(
            101L,
            "tx-it-101",
            501L,
            "FW-ACC-501",
            new BigDecimal("15000.00"),
            "USD",
            "Merchant A",
            "ECOM",
            "DEBIT",
            "PENDING_REVIEW",
            Instant.now(),
            "device-it-1",
            "10.0.0.1"
        );

        fraudScoringService.processTransactionCreated(payload, "corr-it-1");

        FraudDecision decision = fraudDecisionRepository.findByTransactionId(101L).orElseThrow();
        assertThat(decision.getDecision()).isEqualTo(FraudDecisionStatus.BLOCKED);
        assertThat(decision.getRiskScore()).isEqualTo(70);
        assertThat(decision.getTriggeredRules())
            .contains("LARGE_AMOUNT_DEVIATION")
            .contains("NEW_DEVICE_DETECTION");
    }
}
