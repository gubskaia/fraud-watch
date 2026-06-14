package com.fraudwatch.notification.dto;

import java.time.Instant;
import java.util.List;

public record NotificationResponse(
    Long id,
    String eventId,
    String recipientRef,
    String channel,
    String subject,
    String body,
    String status,
    String relatedEntityType,
    String relatedEntityId,
    Instant lastAttemptAt,
    String templateCode,
    List<DeliveryAttemptResponse> attempts,
    Instant createdAt
) {
}

