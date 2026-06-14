package com.fraudwatch.fraud.dto;

public record FraudRuleResponse(
    Long id,
    String code,
    String name,
    String description,
    int weight,
    boolean enabled
) {
}

