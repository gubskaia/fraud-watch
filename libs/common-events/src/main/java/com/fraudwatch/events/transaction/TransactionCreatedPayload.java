package com.fraudwatch.events.transaction;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionCreatedPayload(
    Long transactionId,
    String transactionReference,
    Long accountId,
    String accountNumber,
    BigDecimal amount,
    String currency,
    String merchantName,
    String merchantCategory,
    String direction,
    String status,
    Instant createdAt,
    String deviceId,
    String ipAddress
) {
}
