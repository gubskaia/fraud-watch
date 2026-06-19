package com.fraudwatch.fraud.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fraudwatch.events.transaction.TransactionStatusChangedPayload;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class FailedAttemptTrackingServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private FailedAttemptTrackingService failedAttemptTrackingService;

    @BeforeEach
    void setUp() {
        failedAttemptTrackingService = new FailedAttemptTrackingService(stringRedisTemplate);
    }

    @Test
    void shouldTrackBlockedStatusTransitions() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        failedAttemptTrackingService.recordStatusChange(payload("BLOCKED"));

        verify(valueOperations).increment("fraud:failed-attempts:account:44");
        verify(stringRedisTemplate).expire("fraud:failed-attempts:account:44", Duration.ofHours(24));
    }

    @Test
    void shouldIgnoreNonBlockedTransitions() {
        failedAttemptTrackingService.recordStatusChange(payload("APPROVED"));

        verify(stringRedisTemplate, never()).opsForValue();
    }

    private TransactionStatusChangedPayload payload(String newStatus) {
        return new TransactionStatusChangedPayload(
            11L,
            "tx-11",
            44L,
            "PENDING_REVIEW",
            newStatus,
            "Fraud decision: " + newStatus,
            Instant.parse("2026-06-19T08:00:00Z")
        );
    }
}
