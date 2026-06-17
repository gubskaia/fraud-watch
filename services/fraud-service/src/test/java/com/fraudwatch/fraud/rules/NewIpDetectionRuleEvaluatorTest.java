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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class NewIpDetectionRuleEvaluatorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private NewIpDetectionRuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new NewIpDetectionRuleEvaluator(stringRedisTemplate);
    }

    @Test
    void shouldFlagPreviouslyUnseenIpAddress() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.hasKey("fraud:ip:account:10:203.0.113.15")).thenReturn(false);

        Optional<RuleMatch> result = evaluator.evaluate(rule(), context("203.0.113.15"));

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().ruleCode()).isEqualTo("NEW_IP_DETECTION");
        verify(valueOperations).set(eq("fraud:ip:account:10:203.0.113.15"), eq("seen"), any(Duration.class));
    }

    @Test
    void shouldIgnorePreviouslySeenIpAddress() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.hasKey("fraud:ip:account:10:203.0.113.15")).thenReturn(true);

        Optional<RuleMatch> result = evaluator.evaluate(rule(), context("203.0.113.15"));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldIgnoreBlankIpAddress() {
        Optional<RuleMatch> result = evaluator.evaluate(rule(), context(" "));

        assertThat(result).isEmpty();
    }

    private FraudRule rule() {
        FraudRule rule = new FraudRule();
        rule.setCode("NEW_IP_DETECTION");
        rule.setWeight(15);
        return rule;
    }

    private RuleContext context(String ipAddress) {
        return new RuleContext(new TransactionCreatedPayload(
            1L,
            "tx-1",
            10L,
            "FW-ACC-10",
            new BigDecimal("50.00"),
            "USD",
            "Merchant",
            "ECOM",
            "DEBIT",
            "PENDING_REVIEW",
            Instant.now(),
            "device-1",
            ipAddress
        ));
    }
}
