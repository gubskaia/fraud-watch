package com.fraudwatch.fraud.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.fraudwatch.events.transaction.TransactionCreatedPayload;
import com.fraudwatch.fraud.domain.FraudRule;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class HighRiskMerchantCategoryRuleEvaluatorTest {

    private final HighRiskMerchantCategoryRuleEvaluator evaluator = new HighRiskMerchantCategoryRuleEvaluator();

    @Test
    void shouldMatchHighRiskMerchantCategory() {
        FraudRule rule = rule("HIGH_RISK_MERCHANT_CATEGORY", 20);
        RuleContext context = context("crypto");

        assertThat(evaluator.evaluate(rule, context))
            .isPresent()
            .get()
            .extracting(RuleMatch::ruleCode, RuleMatch::scoreContribution)
            .containsExactly("HIGH_RISK_MERCHANT_CATEGORY", 20);
    }

    @Test
    void shouldIgnoreRegularMerchantCategory() {
        FraudRule rule = rule("HIGH_RISK_MERCHANT_CATEGORY", 20);
        RuleContext context = context("ECOM");

        assertThat(evaluator.evaluate(rule, context)).isEmpty();
    }

    private FraudRule rule(String code, int weight) {
        FraudRule rule = new FraudRule();
        rule.setCode(code);
        rule.setWeight(weight);
        rule.setEnabled(true);
        return rule;
    }

    private RuleContext context(String merchantCategory) {
        return new RuleContext(new TransactionCreatedPayload(
            1L,
            "tx-1",
            10L,
            "ACC-1",
            new BigDecimal("150.00"),
            "USD",
            "Merchant",
            merchantCategory,
            "DEBIT",
            "PENDING_REVIEW",
            Instant.now(),
            "device-1",
            "127.0.0.1"
        ));
    }
}
