package com.fraudwatch.fraud.mapper;

import com.fraudwatch.fraud.domain.FraudDecision;
import com.fraudwatch.fraud.domain.FraudRule;
import com.fraudwatch.fraud.dto.FraudDecisionResponse;
import com.fraudwatch.fraud.dto.FraudRuleResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FraudMapper {

    public FraudRuleResponse toRuleResponse(FraudRule rule) {
        return new FraudRuleResponse(
            rule.getId(),
            rule.getCode(),
            rule.getName(),
            rule.getDescription(),
            rule.getWeight(),
            rule.isEnabled()
        );
    }

    public FraudDecisionResponse toDecisionResponse(FraudDecision decision) {
        return new FraudDecisionResponse(
            decision.getId(),
            decision.getTransactionId(),
            decision.getTransactionReference(),
            decision.getAccountId(),
            decision.getRiskScore(),
            decision.getDecision().name(),
            splitValues(decision.getTriggeredRules()),
            splitValues(decision.getExplanations()),
            decision.getDecidedAt()
        );
    }

    private List<String> splitValues(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("\\|"))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }
}
