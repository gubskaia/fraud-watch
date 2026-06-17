package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.domain.FraudRule;
import java.util.Set;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class HighRiskMerchantCategoryRuleEvaluator implements FraudRuleEvaluator {

    private static final Set<String> HIGH_RISK_CATEGORIES = Set.of(
        "CRYPTO",
        "GAMBLING",
        "GIFT_CARDS",
        "MONEY_TRANSFER"
    );

    @Override
    public String supportedRuleCode() {
        return "HIGH_RISK_MERCHANT_CATEGORY";
    }

    @Override
    public Optional<RuleMatch> evaluate(FraudRule rule, RuleContext context) {
        String merchantCategory = context.transaction().merchantCategory();
        if (merchantCategory == null || merchantCategory.isBlank()) {
            return Optional.empty();
        }

        String normalizedCategory = merchantCategory.trim().toUpperCase();
        if (HIGH_RISK_CATEGORIES.contains(normalizedCategory)) {
            return Optional.of(new RuleMatch(
                rule.getCode(),
                rule.getWeight(),
                "Transaction belongs to a high-risk merchant category"
            ));
        }
        return Optional.empty();
    }
}
