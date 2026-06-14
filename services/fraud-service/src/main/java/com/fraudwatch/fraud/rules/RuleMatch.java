package com.fraudwatch.fraud.rules;

public record RuleMatch(
    String ruleCode,
    int scoreContribution,
    String explanation
) {
}

