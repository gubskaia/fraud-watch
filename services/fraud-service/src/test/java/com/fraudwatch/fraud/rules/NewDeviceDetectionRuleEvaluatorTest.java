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
class NewDeviceDetectionRuleEvaluatorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private NewDeviceDetectionRuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new NewDeviceDetectionRuleEvaluator(stringRedisTemplate);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldMatchWhenDeviceIsNew() {
        FraudRule rule = rule("NEW_DEVICE_DETECTION", 25);
        when(stringRedisTemplate.hasKey("fraud:device:account:10:device-1")).thenReturn(false);

        assertThat(evaluator.evaluate(rule, context("device-1")))
            .isPresent()
            .get()
            .extracting(RuleMatch::ruleCode, RuleMatch::scoreContribution)
            .containsExactly("NEW_DEVICE_DETECTION", 25);

        verify(valueOperations).set(eq("fraud:device:account:10:device-1"), eq("seen"), any(Duration.class));
    }

    @Test
    void shouldNotMatchWhenDeviceWasSeenBefore() {
        FraudRule rule = rule("NEW_DEVICE_DETECTION", 25);
        when(stringRedisTemplate.hasKey("fraud:device:account:10:device-1")).thenReturn(true);

        assertThat(evaluator.evaluate(rule, context("device-1"))).isEmpty();
    }

    @Test
    void shouldNotMatchWhenDeviceIdIsMissing() {
        FraudRule rule = rule("NEW_DEVICE_DETECTION", 25);

        assertThat(evaluator.evaluate(rule, context(null))).isEmpty();
    }

    private FraudRule rule(String code, int weight) {
        FraudRule rule = new FraudRule();
        rule.setCode(code);
        rule.setWeight(weight);
        rule.setEnabled(true);
        return rule;
    }

    private RuleContext context(String deviceId) {
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
            deviceId,
            "127.0.0.1"
        ));
    }
}
