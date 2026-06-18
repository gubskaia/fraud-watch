package com.fraudwatch.audit.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.audit.service.AuditRecordService;
import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.fraud.FraudDecisionPayload;
import com.fraudwatch.events.review.ReviewDecisionMadePayload;
import com.fraudwatch.events.transaction.TransactionCreatedPayload;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AuditRecordService auditRecordService;

    private AuditEventListener auditEventListener;

    @BeforeEach
    void setUp() {
        auditEventListener = new AuditEventListener(objectMapper, auditRecordService);
    }

    @Test
    void shouldStoreTransactionCreatedEvent() throws Exception {
        EventEnvelope<TransactionCreatedPayload> event = transactionCreatedEvent();
        when(objectMapper.readValue(any(byte[].class), any(TypeReference.class))).thenReturn(event);

        auditEventListener.handleTransactionCreated(new byte[] {1, 2, 3});

        verify(auditRecordService).storeEvent(
            eq(event),
            eq("TRANSACTION"),
            eq("101"),
            eq("transaction-service"),
            eq("Transaction created and submitted for fraud evaluation")
        );
    }

    @Test
    void shouldStoreApprovedFraudDecisionEvent() throws Exception {
        EventEnvelope<FraudDecisionPayload> event = fraudDecisionEvent("APPROVE");
        when(objectMapper.readValue(any(byte[].class), any(TypeReference.class))).thenReturn(event);

        auditEventListener.handleTransactionApproved(new byte[] {1});

        verify(auditRecordService).storeEvent(
            eq(event),
            eq("TRANSACTION"),
            eq("202"),
            eq("fraud-service"),
            eq("Fraud engine approved transaction")
        );
    }

    @Test
    void shouldStoreBlockedFraudDecisionEvent() throws Exception {
        EventEnvelope<FraudDecisionPayload> event = fraudDecisionEvent("BLOCK");
        when(objectMapper.readValue(any(byte[].class), any(TypeReference.class))).thenReturn(event);

        auditEventListener.handleTransactionBlocked(new byte[] {2});

        verify(auditRecordService).storeEvent(
            eq(event),
            eq("TRANSACTION"),
            eq("202"),
            eq("fraud-service"),
            eq("Fraud engine blocked transaction")
        );
    }

    @Test
    void shouldStoreReviewRequiredFraudDecisionEvent() throws Exception {
        EventEnvelope<FraudDecisionPayload> event = fraudDecisionEvent("REVIEW_REQUIRED");
        when(objectMapper.readValue(any(byte[].class), any(TypeReference.class))).thenReturn(event);

        auditEventListener.handleTransactionReviewRequired(new byte[] {3});

        verify(auditRecordService).storeEvent(
            eq(event),
            eq("TRANSACTION"),
            eq("202"),
            eq("fraud-service"),
            eq("Fraud engine escalated transaction for manual review")
        );
    }

    @Test
    void shouldStoreReviewDecisionEvent() throws Exception {
        EventEnvelope<ReviewDecisionMadePayload> event = reviewDecisionEvent();
        when(objectMapper.readValue(any(byte[].class), any(TypeReference.class))).thenReturn(event);

        auditEventListener.handleReviewDecisionMade(new byte[] {4});

        verify(auditRecordService).storeEvent(
            eq(event),
            eq("REVIEW_CASE"),
            eq("77"),
            eq("review-service"),
            eq("Analyst finalized manual review decision")
        );
    }

    @Test
    void shouldTranslateParsingFailure() throws Exception {
        when(objectMapper.readValue(any(byte[].class), any(TypeReference.class))).thenThrow(new IOException("bad payload"));

        assertThatThrownBy(() -> auditEventListener.handleTransactionCreated(new byte[] {9}))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unable to parse audit event");
    }

    private EventEnvelope<TransactionCreatedPayload> transactionCreatedEvent() {
        return new EventEnvelope<>(
            "evt-tx",
            "transaction.created",
            "v1",
            Instant.parse("2026-06-18T10:00:00Z"),
            "corr-tx",
            Map.of(),
            new TransactionCreatedPayload(
                101L,
                "tx-101",
                5001L,
                "FW-ACC-5001",
                new BigDecimal("250.00"),
                "USD",
                "Merchant",
                "ECOM",
                "DEBIT",
                "PENDING_REVIEW",
                Instant.parse("2026-06-18T10:00:00Z"),
                "device-1",
                "127.0.0.1"
            )
        );
    }

    private EventEnvelope<FraudDecisionPayload> fraudDecisionEvent(String decision) {
        return new EventEnvelope<>(
            "evt-fraud-" + decision,
            "fraud.decision.made",
            "v1",
            Instant.parse("2026-06-18T10:05:00Z"),
            "corr-fraud",
            Map.of(),
            new FraudDecisionPayload(
                202L,
                "tx-202",
                6002L,
                85,
                decision,
                List.of("RULE_A"),
                List.of("Explanation A"),
                Instant.parse("2026-06-18T10:05:00Z")
            )
        );
    }

    private EventEnvelope<ReviewDecisionMadePayload> reviewDecisionEvent() {
        return new EventEnvelope<>(
            "evt-review",
            "review.decision.made",
            "v1",
            Instant.parse("2026-06-18T10:10:00Z"),
            "corr-review",
            Map.of(),
            new ReviewDecisionMadePayload(
                77L,
                202L,
                "tx-202",
                "BLOCKED",
                "CONFIRMED_FRAUD",
                "analyst-1",
                Instant.parse("2026-06-18T10:10:00Z")
            )
        );
    }
}
