package com.fraudwatch.auth.dto;

import java.util.Set;

public record UserResponse(
    Long id,
    String username,
    String email,
    String firstName,
    String lastName,
    String status,
    Set<String> roles,
    Set<String> permissions
) {
}

