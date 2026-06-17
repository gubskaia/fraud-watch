package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.domain.FraudRule;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UnusualTransactionHourRuleEvaluator implements FraudRuleEvaluator {

    private static final int START_HOUR_INCLUSIVE = 0;
    private static final int END_HOUR_EXCLUSIVE = 5;

    @Override
    public String supportedRuleCode() {
        return "UNUSUAL_TRANSACTION_HOUR";
    }

    @Override
    public Optional<RuleMatch> evaluate(FraudRule rule, RuleContext context) {
        int hour = context.transaction().createdAt().atZone(ZoneOffset.UTC).getHour();
        if (hour >= START_HOUR_INCLUSIVE && hour < END_HOUR_EXCLUSIVE) {
            return Optional.of(new RuleMatch(
                rule.getCode(),
                rule.getWeight(),
                "Transaction occurred during an unusual overnight hour"
            ));
        }
        return Optional.empty();
    }
}
