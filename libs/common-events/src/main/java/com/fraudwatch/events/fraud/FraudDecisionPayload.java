package com.fraudwatch.events.fraud;

import java.time.Instant;
import java.util.List;

public record FraudDecisionPayload(
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
