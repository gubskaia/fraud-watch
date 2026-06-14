package com.fraudwatch.fraud.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.fraudwatch.events.transaction.TransactionCreatedPayload;
import com.fraudwatch.fraud.domain.FraudRule;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LargeAmountDeviationRuleEvaluatorTest {

    private final LargeAmountDeviationRuleEvaluator evaluator = new LargeAmountDeviationRuleEvaluator();

    @Test
    void shouldMatchWhenAmountExceedsThreshold() {
        FraudRule rule = rule("LARGE_AMOUNT_DEVIATION", 45);
        RuleContext context = context(new BigDecimal("15000.00"));

        assertThat(evaluator.evaluate(rule, context))
            .isPresent()
            .get()
            .extracting(RuleMatch::ruleCode, RuleMatch::scoreContribution)
            .containsExactly("LARGE_AMOUNT_DEVIATION", 45);
    }

    @Test
    void shouldNotMatchWhenAmountIsBelowThreshold() {
        FraudRule rule = rule("LARGE_AMOUNT_DEVIATION", 45);
        RuleContext context = context(new BigDecimal("250.00"));

        assertThat(evaluator.evaluate(rule, context)).isEmpty();
    }

    private FraudRule rule(String code, int weight) {
        FraudRule rule = new FraudRule();
        rule.setCode(code);
        rule.setWeight(weight);
        rule.setEnabled(true);
        return rule;
    }

    private RuleContext context(BigDecimal amount) {
        return new RuleContext(new TransactionCreatedPayload(
            1L,
            "tx-1",
            10L,
            "ACC-1",
            amount,
            "USD",
            "Merchant",
            "ECOM",
            "DEBIT",
            "PENDING_REVIEW",
            Instant.now(),
            "device-1",
            "127.0.0.1"
        ));
    }
}
