package com.fraudwatch.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventEnvelope<T>(
    String eventId,
    String eventType,
    String eventVersion,
    Instant occurredAt,
    String correlationId,
    Map<String, String> metadata,
    T payload
) {
}

