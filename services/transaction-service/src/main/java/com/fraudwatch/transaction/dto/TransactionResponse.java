package com.fraudwatch.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
    Long id,
    String transactionReference,
    Long accountId,
    String accountNumber,
    BigDecimal amount,
    String currency,
    String direction,
    String status,
    String merchantName,
    String merchantCategory,
    String deviceId,
    String ipAddress,
    String description,
    Instant createdAt
) {
}

