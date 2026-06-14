package com.fraudwatch.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewDecisionRequest(
    @NotBlank @Size(max = 120) String analyst,
    @NotBlank @Size(max = 100) String reasonCode,
    @Size(max = 1000) String details
) {
}

