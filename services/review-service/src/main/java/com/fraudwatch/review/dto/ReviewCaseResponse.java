package com.fraudwatch.review.dto;

import java.time.Instant;
import java.util.List;

public record ReviewCaseResponse(
    Long id,
    Long transactionId,
    String transactionReference,
    Long accountId,
    int riskScore,
    List<String> triggeredRules,
    List<String> explanations,
    String status,
    String assignedTo,
    String reasonCode,
    Instant decisionAt,
    List<ReviewCommentResponse> comments,
    List<ReviewActionResponse> actions,
    Instant createdAt
) {
}

