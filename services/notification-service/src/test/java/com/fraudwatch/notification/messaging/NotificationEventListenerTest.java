package com.fraudwatch.notification.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.fraud.FraudDecisionPayload;
import com.fraudwatch.events.review.ReviewDecisionMadePayload;
import com.fraudwatch.notification.service.NotificationService;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationService notificationService;

    private NotificationEventListener notificationEventListener;

    @BeforeEach
    void setUp() {
        notificationEventListener = new NotificationEventListener(objectMapper, notificationService);
    }

    @Test
    void shouldForwardApprovedFraudEvent() throws Exception {
        EventEnvelope<FraudDecisionPayload> event = fraudEvent("APPROVED");
        when(objectMapper.readValue(any(byte[].class), any(TypeReference.class))).thenReturn(event);

        notificationEventListener.handleApproved(new byte[] {1});

        verify(notificationService).handleFraudDecision(event);
    }

    @Test
    void shouldForwardBlockedFraudEvent() throws Exception {
        EventEnvelope<FraudDecisionPayload> event = fraudEvent("BLOCKED");
        when(objectMapper.readValue(any(byte[].class), any(TypeReference.class))).thenReturn(event);

        notificationEventListener.handleBlocked(new byte[] {2});

        verify(notificationService).handleFraudDecision(event);
    }

    @Test
    void shouldForwardReviewRequiredFraudEvent() throws Exception {
        EventEnvelope<FraudDecisionPayload> event = fraudEvent("UNDER_REVIEW");
        when(objectMapper.readValue(any(byte[].class), any(TypeReference.class))).thenReturn(event);

        notificationEventListener.handleReviewRequired(new byte[] {3});

        verify(notificationService).handleFraudDecision(event);
    }

    @Test
    void shouldForwardReviewDecisionEvent() throws Exception {
        EventEnvelope<ReviewDecisionMadePayload> event = reviewEvent();
        when(objectMapper.readValue(any(byte[].class), any(TypeReference.class))).thenReturn(event);

        notificationEventListener.handleReviewDecision(new byte[] {4});

        verify(notificationService).handleReviewDecision(event);
    }

    @Test
    void shouldTranslateFraudEventParsingFailure() throws Exception {
        when(objectMapper.readValue(any(byte[].class), any(TypeReference.class))).thenThrow(new IOException("bad fraud payload"));

        assertThatThrownBy(() -> notificationEventListener.handleApproved(new byte[] {9}))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unable to parse fraud notification event");
    }

    @Test
    void shouldTranslateReviewEventParsingFailure() throws Exception {
        when(objectMapper.readValue(any(byte[].class), any(TypeReference.class))).thenThrow(new IOException("bad review payload"));

        assertThatThrownBy(() -> notificationEventListener.handleReviewDecision(new byte[] {10}))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unable to parse review notification event");
    }

    private EventEnvelope<FraudDecisionPayload> fraudEvent(String decision) {
        return new EventEnvelope<>(
            "evt-fraud-" + decision,
            "fraud.decision.made",
            "v1",
            Instant.parse("2026-06-19T06:00:00Z"),
            "corr-fraud",
            Map.of(),
            new FraudDecisionPayload(
                101L,
                "tx-101",
                5001L,
                72,
                decision,
                List.of("RULE_A"),
                List.of("Explanation A"),
                Instant.parse("2026-06-19T06:00:00Z")
            )
        );
    }

    private EventEnvelope<ReviewDecisionMadePayload> reviewEvent() {
        return new EventEnvelope<>(
            "evt-review",
            "review.decision.made",
            "v1",
            Instant.parse("2026-06-19T06:05:00Z"),
            "corr-review",
            Map.of(),
            new ReviewDecisionMadePayload(
                77L,
                101L,
                "tx-101",
                "APPROVED",
                "LEGIT_ACTIVITY",
                "analyst-1",
                Instant.parse("2026-06-19T06:05:00Z")
            )
        );
    }
}
