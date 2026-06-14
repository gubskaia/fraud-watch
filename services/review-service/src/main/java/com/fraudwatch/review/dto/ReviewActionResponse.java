package com.fraudwatch.review.dto;

import java.time.Instant;

public record ReviewActionResponse(
    Long id,
    String actionType,
    String analyst,
    String reasonCode,
    String details,
    Instant createdAt
) {
}

