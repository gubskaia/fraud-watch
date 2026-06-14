package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.domain.FraudRule;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class LargeAmountDeviationRuleEvaluator implements FraudRuleEvaluator {

    private static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("10000.00");

    @Override
    public String supportedRuleCode() {
        return "LARGE_AMOUNT_DEVIATION";
    }

    @Override
    public Optional<RuleMatch> evaluate(FraudRule rule, RuleContext context) {
        if (context.transaction().amount().compareTo(LARGE_AMOUNT_THRESHOLD) >= 0) {
            return Optional.of(new RuleMatch(
                rule.getCode(),
                rule.getWeight(),
                "Transaction amount exceeds large-amount threshold"
            ));
        }
        return Optional.empty();
    }
}

