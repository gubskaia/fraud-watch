package com.fraudwatch.fraud.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateFraudRuleRequest(
    @NotNull Boolean enabled,
    @NotNull @Min(0) @Max(100) Integer weight
) {
}

