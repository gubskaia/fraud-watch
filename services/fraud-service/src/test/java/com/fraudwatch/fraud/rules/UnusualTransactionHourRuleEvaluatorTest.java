package com.fraudwatch.fraud.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.fraudwatch.events.transaction.TransactionCreatedPayload;
import com.fraudwatch.fraud.domain.FraudRule;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UnusualTransactionHourRuleEvaluatorTest {

    private final UnusualTransactionHourRuleEvaluator evaluator = new UnusualTransactionHourRuleEvaluator();

    @Test
    void shouldMatchOvernightTransactionHour() {
        FraudRule rule = rule("UNUSUAL_TRANSACTION_HOUR", 10);

        assertThat(evaluator.evaluate(rule, context(Instant.parse("2026-01-15T02:30:00Z"))))
            .isPresent()
            .get()
            .extracting(RuleMatch::ruleCode, RuleMatch::scoreContribution)
            .containsExactly("UNUSUAL_TRANSACTION_HOUR", 10);
    }

    @Test
    void shouldIgnoreRegularTransactionHour() {
        FraudRule rule = rule("UNUSUAL_TRANSACTION_HOUR", 10);

        assertThat(evaluator.evaluate(rule, context(Instant.parse("2026-01-15T12:30:00Z")))).isEmpty();
    }

    private FraudRule rule(String code, int weight) {
        FraudRule rule = new FraudRule();
        rule.setCode(code);
        rule.setWeight(weight);
        rule.setEnabled(true);
        return rule;
    }

    private RuleContext context(Instant createdAt) {
        return new RuleContext(new TransactionCreatedPayload(
            1L,
            "tx-1",
            10L,
            "ACC-1",
            new BigDecimal("150.00"),
            "USD",
            "Merchant",
            "ECOM",
            "DEBIT",
            "PENDING_REVIEW",
            createdAt,
            "device-1",
            "127.0.0.1"
        ));
    }
}
