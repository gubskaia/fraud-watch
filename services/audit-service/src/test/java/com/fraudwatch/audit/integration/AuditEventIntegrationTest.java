package com.fraudwatch.audit.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.audit.domain.AuditRecord;
import com.fraudwatch.audit.messaging.AuditEventListener;
import com.fraudwatch.audit.repository.AuditRecordRepository;
import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.review.ReviewDecisionMadePayload;
import com.fraudwatch.events.transaction.TransactionCreatedPayload;
import com.fraudwatch.test.InfrastructureContainers;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
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
class AuditEventIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = InfrastructureContainers.postgres("audit_db_integration_test");

    @Container
    static final RabbitMQContainer rabbitMq = InfrastructureContainers.rabbitMq();

    @Autowired
    private AuditEventListener auditEventListener;

    @Autowired
    private AuditRecordRepository auditRecordRepository;

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
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> false);
        registry.add("spring.rabbitmq.listener.direct.auto-startup", () -> false);
    }

    @Test
    void shouldPersistTransactionCreatedEventOnlyOnce() throws Exception {
        EventEnvelope<TransactionCreatedPayload> event = transactionCreatedEvent();
        byte[] payload = objectMapper.writeValueAsBytes(event);

        auditEventListener.handleTransactionCreated(payload);
        auditEventListener.handleTransactionCreated(payload);

        assertThat(auditRecordRepository.findAll()).hasSize(1);

        AuditRecord auditRecord = auditRecordRepository.findByEventId(event.eventId()).orElseThrow();
        assertThat(auditRecord.getEventType()).isEqualTo("TransactionCreated");
        assertThat(auditRecord.getAggregateType()).isEqualTo("TRANSACTION");
        assertThat(auditRecord.getAggregateId()).isEqualTo("101");
        assertThat(auditRecord.getCorrelationId()).isEqualTo("corr-audit-tx");
        assertThat(auditRecord.getSource()).isEqualTo("transaction-service");
        assertThat(auditRecord.getSummary()).isEqualTo("Transaction created and submitted for fraud evaluation");
        assertThat(auditRecord.getPayloadJson()).contains("\"transactionReference\":\"tx-audit-101\"");
    }

    @Test
    void shouldPersistReviewDecisionAuditRecord() throws Exception {
        EventEnvelope<ReviewDecisionMadePayload> event = reviewDecisionEvent();

        auditEventListener.handleReviewDecisionMade(objectMapper.writeValueAsBytes(event));

        AuditRecord auditRecord = auditRecordRepository.findByEventId(event.eventId()).orElseThrow();
        assertThat(auditRecord.getEventType()).isEqualTo("ReviewDecisionMade");
        assertThat(auditRecord.getAggregateType()).isEqualTo("REVIEW_CASE");
        assertThat(auditRecord.getAggregateId()).isEqualTo("77");
        assertThat(auditRecord.getSource()).isEqualTo("review-service");
        assertThat(auditRecord.getSummary()).isEqualTo("Analyst finalized manual review decision");
        assertThat(auditRecord.getPayloadJson()).contains("\"finalDecision\":\"BLOCKED\"");
        assertThat(auditRecord.getPayloadJson()).contains("\"reasonCode\":\"CONFIRMED_FRAUD\"");
    }

    private EventEnvelope<TransactionCreatedPayload> transactionCreatedEvent() {
        return new EventEnvelope<>(
            UUID.randomUUID().toString(),
            "TransactionCreated",
            "v1",
            Instant.parse("2026-06-19T10:00:00Z"),
            "corr-audit-tx",
            Map.of("service", "transaction-service"),
            new TransactionCreatedPayload(
                101L,
                "tx-audit-101",
                5001L,
                "FW-ACC-5001",
                new BigDecimal("250.00"),
                "USD",
                "Merchant Audit",
                "ECOM",
                "DEBIT",
                "PENDING_REVIEW",
                Instant.parse("2026-06-19T10:00:00Z"),
                "device-audit-1",
                "127.0.0.1"
            )
        );
    }

    private EventEnvelope<ReviewDecisionMadePayload> reviewDecisionEvent() {
        return new EventEnvelope<>(
            UUID.randomUUID().toString(),
            "ReviewDecisionMade",
            "v1",
            Instant.parse("2026-06-19T10:15:00Z"),
            "corr-audit-review",
            Map.of("service", "review-service"),
            new ReviewDecisionMadePayload(
                77L,
                101L,
                "tx-audit-101",
                "BLOCKED",
                "CONFIRMED_FRAUD",
                "analyst-audit",
                Instant.parse("2026-06-19T10:15:00Z")
            )
        );
    }
}
