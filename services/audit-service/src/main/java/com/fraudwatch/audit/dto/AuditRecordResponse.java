package com.fraudwatch.audit.dto;

import java.time.Instant;

public record AuditRecordResponse(
    Long id,
    String eventId,
    String eventType,
    String aggregateType,
    String aggregateId,
    String correlationId,
    String source,
    String summary,
    String payloadJson,
    Instant occurredAt,
    Instant createdAt
) {
}

