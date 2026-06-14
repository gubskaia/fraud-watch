package com.fraudwatch.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank
    @Size(min = 4, max = 120)
    String username,
    @NotBlank
    @Email
    String email,
    @NotBlank
    @Size(min = 8, max = 255)
    String password,
    @NotBlank
    @Size(max = 100)
    String firstName,
    @NotBlank
    @Size(max = 100)
    String lastName
) {
}

