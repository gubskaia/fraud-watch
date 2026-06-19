package com.fraudwatch.fraud.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fraudwatch.events.transaction.TransactionCreatedPayload;
import com.fraudwatch.fraud.domain.FraudRule;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RepeatedFailedAttemptsRuleEvaluatorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RepeatedFailedAttemptsRuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new RepeatedFailedAttemptsRuleEvaluator(stringRedisTemplate);
    }

    @Test
    void shouldMatchWhenBlockedAttemptThresholdWasReached() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("fraud:failed-attempts:account:10")).thenReturn("2");

        assertThat(evaluator.evaluate(rule(), context())).isPresent();
    }

    @Test
    void shouldIgnoreWhenBlockedAttemptCountIsBelowThreshold() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("fraud:failed-attempts:account:10")).thenReturn("1");

        assertThat(evaluator.evaluate(rule(), context())).isEmpty();
    }

    @Test
    void shouldIgnoreWhenStoredCounterIsInvalid() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("fraud:failed-attempts:account:10")).thenReturn("not-a-number");

        assertThat(evaluator.evaluate(rule(), context())).isEmpty();
    }

    private FraudRule rule() {
        FraudRule rule = new FraudRule();
        rule.setCode("REPEATED_FAILED_ATTEMPTS");
        rule.setWeight(20);
        rule.setEnabled(true);
        return rule;
    }

    private RuleContext context() {
        return new RuleContext(new TransactionCreatedPayload(
            1L,
            "tx-1",
            10L,
            "FW-ACC-10",
            new BigDecimal("120.00"),
            "USD",
            "Merchant",
            "ECOM",
            "DEBIT",
            "PENDING_REVIEW",
            Instant.parse("2026-06-19T07:30:00Z"),
            "device-1",
            "203.0.113.10"
        ));
    }
}
