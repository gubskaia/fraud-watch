package com.fraudwatch.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateTransactionRequest(
    @NotNull Long accountId,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotBlank @Size(min = 3, max = 3) String currency,
    @NotBlank String direction,
    @NotBlank @Size(max = 200) String merchantName,
    @NotBlank @Size(max = 100) String merchantCategory,
    @Size(max = 255) String deviceId,
    @Size(max = 500) String description
) {
}

