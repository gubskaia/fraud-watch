package com.fraudwatch.notification.dto;

import java.time.Instant;

public record DeliveryAttemptResponse(
    Long id,
    int attemptNumber,
    String status,
    String details,
    Instant createdAt
) {
}

