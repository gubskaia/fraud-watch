package com.fraudwatch.fraud.dto;

import java.time.Instant;
import java.util.List;

public record FraudDecisionResponse(
    Long id,
    Long transactionId,
    String transactionReference,
    Long accountId,
    int riskScore,
    String decision,
    List<String> triggeredRules,
    List<String> explanations,
    Instant decidedAt
) {
}

