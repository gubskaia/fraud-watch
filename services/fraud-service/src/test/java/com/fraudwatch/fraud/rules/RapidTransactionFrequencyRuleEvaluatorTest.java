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
class RapidTransactionFrequencyRuleEvaluatorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RapidTransactionFrequencyRuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new RapidTransactionFrequencyRuleEvaluator(stringRedisTemplate);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldMatchWhenThresholdIsExceeded() {
        FraudRule rule = rule("RAPID_TRANSACTION_FREQUENCY", 30);
        when(valueOperations.increment("fraud:velocity:account:10")).thenReturn(4L);

        assertThat(evaluator.evaluate(rule, context()))
            .isPresent()
            .get()
            .extracting(RuleMatch::ruleCode, RuleMatch::scoreContribution)
            .containsExactly("RAPID_TRANSACTION_FREQUENCY", 30);

        verify(stringRedisTemplate).expire(eq("fraud:velocity:account:10"), any(Duration.class));
    }

    @Test
    void shouldNotMatchWhenThresholdIsNotExceeded() {
        FraudRule rule = rule("RAPID_TRANSACTION_FREQUENCY", 30);
        when(valueOperations.increment("fraud:velocity:account:10")).thenReturn(2L);

        assertThat(evaluator.evaluate(rule, context())).isEmpty();
    }

    private FraudRule rule(String code, int weight) {
        FraudRule rule = new FraudRule();
        rule.setCode(code);
        rule.setWeight(weight);
        rule.setEnabled(true);
        return rule;
    }

    private RuleContext context() {
        return new RuleContext(new TransactionCreatedPayload(
            1L,
            "tx-1",
            10L,
            "ACC-1",
            new BigDecimal("120.00"),
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
