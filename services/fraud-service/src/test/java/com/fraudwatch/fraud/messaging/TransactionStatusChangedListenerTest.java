package com.fraudwatch.fraud.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.transaction.TransactionStatusChangedPayload;
import com.fraudwatch.fraud.service.FailedAttemptTrackingService;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionStatusChangedListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FailedAttemptTrackingService failedAttemptTrackingService;

    private TransactionStatusChangedListener listener;

    @BeforeEach
    void setUp() {
        listener = new TransactionStatusChangedListener(objectMapper, failedAttemptTrackingService);
    }

    @Test
    void shouldForwardStatusChangedPayloadToTrackingService() throws Exception {
        EventEnvelope<TransactionStatusChangedPayload> event = new EventEnvelope<>(
            "evt-status-1",
            "TransactionStatusChanged",
            "v1",
            Instant.parse("2026-06-19T07:00:00Z"),
            "corr-status",
            Map.of(),
            new TransactionStatusChangedPayload(
                101L,
                "tx-101",
                5001L,
                "UNDER_REVIEW",
                "BLOCKED",
                "Manual review decision: BLOCKED",
                Instant.parse("2026-06-19T07:00:00Z")
            )
        );
        when(objectMapper.readValue(any(byte[].class), any(TypeReference.class))).thenReturn(event);

        listener.handle(new byte[] {1, 2, 3});

        verify(failedAttemptTrackingService).recordStatusChange(event.payload());
    }

    @Test
    void shouldTranslateParsingFailure() throws Exception {
        when(objectMapper.readValue(any(byte[].class), any(TypeReference.class))).thenThrow(new IOException("bad payload"));

        assertThatThrownBy(() -> listener.handle(new byte[] {9}))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unable to parse TransactionStatusChanged event");
    }
}
