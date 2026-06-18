package com.fraudwatch.fraud.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fraudwatch.events.transaction.TransactionCreatedPayload;
import com.fraudwatch.fraud.domain.FraudRule;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RiskAccumulationShortPeriodRuleEvaluatorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RiskAccumulationShortPeriodRuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new RiskAccumulationShortPeriodRuleEvaluator(stringRedisTemplate);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldMatchWhenAccumulatedAmountExceedsThreshold() {
        FraudRule rule = rule("RISK_ACCUMULATION_SHORT_PERIOD", 35);
        when(valueOperations.increment("fraud:amount:account:10", 1_500_000L)).thenReturn(2_600_000L);

        assertThat(evaluator.evaluate(rule, context(new BigDecimal("15000.00"))))
            .isPresent()
            .get()
            .extracting(RuleMatch::ruleCode, RuleMatch::scoreContribution)
            .containsExactly("RISK_ACCUMULATION_SHORT_PERIOD", 35);

        verify(stringRedisTemplate).expire(eq("fraud:amount:account:10"), any(Duration.class));
    }

    @Test
    void shouldNotMatchWhenAccumulatedAmountIsBelowThreshold() {
        FraudRule rule = rule("RISK_ACCUMULATION_SHORT_PERIOD", 35);
        when(valueOperations.increment("fraud:amount:account:10", 9_000_00L)).thenReturn(2_000_000L);

        assertThat(evaluator.evaluate(rule, context(new BigDecimal("9000.00")))).isEmpty();
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
