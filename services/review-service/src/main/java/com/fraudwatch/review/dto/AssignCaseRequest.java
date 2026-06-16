package com.fraudwatch.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignCaseRequest(
    @NotBlank @Size(max = 120) String analyst,
    @Size(max = 1000) String details
) {
}
