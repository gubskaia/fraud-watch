package com.fraudwatch.events.review;

import java.time.Instant;

public record ReviewDecisionMadePayload(
    Long fraudCaseId,
    Long transactionId,
    String transactionReference,
    String finalDecision,
    String reasonCode,
    String analyst,
    Instant decidedAt
) {
}
