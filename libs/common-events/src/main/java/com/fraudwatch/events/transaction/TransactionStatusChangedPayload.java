package com.fraudwatch.events.transaction;

import java.time.Instant;

public record TransactionStatusChangedPayload(
    Long transactionId,
    String transactionReference,
    Long accountId,
    String previousStatus,
    String newStatus,
    String reason,
    Instant changedAt
) {
}
