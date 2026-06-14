package com.fraudwatch.review.dto;

import java.time.Instant;

public record ReviewCommentResponse(
    Long id,
    String analyst,
    String comment,
    Instant createdAt
) {
}

