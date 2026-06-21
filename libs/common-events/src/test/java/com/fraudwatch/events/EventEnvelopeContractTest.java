package com.fraudwatch.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fraudwatch.events.fraud.FraudDecisionPayload;
import com.fraudwatch.events.review.ReviewDecisionMadePayload;
import com.fraudwatch.events.transaction.TransactionCreatedPayload;
import com.fraudwatch.events.transaction.TransactionStatusChangedPayload;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EventEnvelopeContractTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .build();

    @Test
    void shouldSerializeAndDeserializeTransactionCreatedEvent() throws Exception {
        EventEnvelope<TransactionCreatedPayload> event = new EventEnvelope<>(
            "evt-created-1",
            "TransactionCreated",
            "v1",
            Instant.parse("2026-06-21T10:00:00Z"),
            "corr-created-1",
            Map.of("service", "transaction-service"),
            new TransactionCreatedPayload(
                101L,
                "tx-101",
                5001L,
                "FW-ACC-5001",
                new BigDecimal("250.00"),
                "USD",
                "Merchant One",
                "ECOM",
                "DEBIT",
                "PENDING_REVIEW",
                Instant.parse("2026-06-21T10:00:00Z"),
                "device-1",
                "127.0.0.1"
            )
        );

        String json = objectMapper.writeValueAsString(event);
        EventEnvelope<TransactionCreatedPayload> restored = objectMapper.readValue(
            json,
            new TypeReference<>() {
            }
        );

        assertThat(json).contains("\"eventType\":\"TransactionCreated\"");
        assertThat(json).contains("\"eventVersion\":\"v1\"");
        assertThat(json).contains("\"transactionReference\":\"tx-101\"");
        assertThat(restored).isEqualTo(event);
    }

    @Test
    void shouldSerializeAndDeserializeFraudDecisionEvent() throws Exception {
        EventEnvelope<FraudDecisionPayload> event = new EventEnvelope<>(
            "evt-fraud-1",
            "TransactionReviewRequired",
            "v1",
            Instant.parse("2026-06-21T10:05:00Z"),
            "corr-fraud-1",
            Map.of("service", "fraud-service"),
            new FraudDecisionPayload(
                101L,
                "tx-101",
                5001L,
                48,
                "UNDER_REVIEW",
                List.of("RULE_A", "RULE_B"),
                List.of("Velocity threshold exceeded", "New device detected"),
                Instant.parse("2026-06-21T10:05:00Z")
            )
        );

        String json = objectMapper.writeValueAsString(event);
        EventEnvelope<FraudDecisionPayload> restored = objectMapper.readValue(
            json,
            new TypeReference<>() {
            }
        );

        assertThat(json).contains("\"decision\":\"UNDER_REVIEW\"");
        assertThat(json).contains("\"triggeredRules\":[\"RULE_A\",\"RULE_B\"]");
        assertThat(restored).isEqualTo(event);
    }

    @Test
    void shouldSerializeAndDeserializeReviewDecisionEvent() throws Exception {
        EventEnvelope<ReviewDecisionMadePayload> event = new EventEnvelope<>(
            "evt-review-1",
            "ReviewDecisionMade",
            "v1",
            Instant.parse("2026-06-21T10:10:00Z"),
            "corr-review-1",
            Map.of("service", "review-service"),
            new ReviewDecisionMadePayload(
                77L,
                101L,
                "tx-101",
                "BLOCKED",
                "CONFIRMED_FRAUD",
                "analyst-1",
                Instant.parse("2026-06-21T10:10:00Z")
            )
        );

        String json = objectMapper.writeValueAsString(event);
        EventEnvelope<ReviewDecisionMadePayload> restored = objectMapper.readValue(
            json,
            new TypeReference<>() {
            }
        );

        assertThat(json).contains("\"finalDecision\":\"BLOCKED\"");
        assertThat(json).contains("\"reasonCode\":\"CONFIRMED_FRAUD\"");
        assertThat(restored).isEqualTo(event);
    }

    @Test
    void shouldSerializeAndDeserializeTransactionStatusChangedEvent() throws Exception {
        EventEnvelope<TransactionStatusChangedPayload> event = new EventEnvelope<>(
            "evt-status-1",
            "TransactionStatusChanged",
            "v1",
            Instant.parse("2026-06-21T10:15:00Z"),
            "corr-status-1",
            Map.of("service", "transaction-service"),
            new TransactionStatusChangedPayload(
                101L,
                "tx-101",
                5001L,
                "UNDER_REVIEW",
                "BLOCKED",
                "Manual review decision: BLOCKED",
                Instant.parse("2026-06-21T10:15:00Z")
            )
        );

        String json = objectMapper.writeValueAsString(event);
        EventEnvelope<TransactionStatusChangedPayload> restored = objectMapper.readValue(
            json,
            new TypeReference<>() {
            }
        );

        assertThat(json).contains("\"previousStatus\":\"UNDER_REVIEW\"");
        assertThat(json).contains("\"newStatus\":\"BLOCKED\"");
        assertThat(restored).isEqualTo(event);
    }

    @Test
    void shouldOmitNullMetadataFieldsFromEnvelope() throws Exception {
        EventEnvelope<ReviewDecisionMadePayload> event = new EventEnvelope<>(
            "evt-review-2",
            "ReviewDecisionMade",
            "v1",
            Instant.parse("2026-06-21T10:20:00Z"),
            null,
            null,
            new ReviewDecisionMadePayload(
                78L,
                102L,
                "tx-102",
                "APPROVED",
                null,
                "analyst-2",
                Instant.parse("2026-06-21T10:20:00Z")
            )
        );

        String json = objectMapper.writeValueAsString(event);

        assertThat(json).doesNotContain("correlationId");
        assertThat(json).doesNotContain("metadata");
        assertThat(json).contains("\"eventVersion\":\"v1\"");
    }
}
