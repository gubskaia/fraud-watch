package com.fraudwatch.transaction.dto;

import java.math.BigDecimal;

public record AccountResponse(
    Long id,
    String accountNumber,
    String customerId,
    String ownerName,
    String currency,
    BigDecimal balance,
    String status
) {
}

