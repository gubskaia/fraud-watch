package com.fraudwatch.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AccountRequest(
    @NotBlank @Size(max = 50) String accountNumber,
    @NotBlank @Size(max = 100) String customerId,
    @NotBlank @Size(max = 200) String ownerName,
    @NotBlank @Size(min = 3, max = 3) String currency,
    @NotNull @DecimalMin("0.00") BigDecimal initialBalance
) {
}

