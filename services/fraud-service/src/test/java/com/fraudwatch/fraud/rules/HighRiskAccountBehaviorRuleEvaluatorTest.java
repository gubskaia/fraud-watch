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
class HighRiskAccountBehaviorRuleEvaluatorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private HighRiskAccountBehaviorRuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new HighRiskAccountBehaviorRuleEvaluator(stringRedisTemplate);
    }

    @Test
    void shouldMatchWhenAccumulatedRiskSignalsCrossThreshold() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("fraud:behavior:account:10", 4L)).thenReturn(5L);

        assertThat(evaluator.evaluate(rule(), riskyContext()))
            .isPresent()
            .get()
            .extracting(RuleMatch::ruleCode, RuleMatch::scoreContribution)
            .containsExactly("HIGH_RISK_ACCOUNT_BEHAVIOR", 40);

        verify(stringRedisTemplate).expire(eq("fraud:behavior:account:10"), any(Duration.class));
    }

    @Test
    void shouldIgnoreWhenCurrentTransactionHasNoRiskSignals() {
        assertThat(evaluator.evaluate(rule(), benignContext())).isEmpty();
    }

    @Test
    void shouldComputeFourSignalsForClearlyRiskyTransaction() {
        assertThat(HighRiskAccountBehaviorRuleEvaluator.riskSignals(riskyContext())).isEqualTo(4);
    }

    private FraudRule rule() {
        FraudRule rule = new FraudRule();
        rule.setCode("HIGH_RISK_ACCOUNT_BEHAVIOR");
        rule.setWeight(40);
        rule.setEnabled(true);
        return rule;
    }

    private RuleContext riskyContext() {
        return context(
            new BigDecimal("15000.00"),
            "CRYPTO",
            "198.51.100.77",
            Instant.parse("2026-01-15T02:30:00Z")
        );
    }

    private RuleContext benignContext() {
        return context(
            new BigDecimal("250.00"),
            "ECOM",
            "127.0.0.1",
            Instant.parse("2026-01-15T12:30:00Z")
        );
    }

    private RuleContext context(
        BigDecimal amount,
        String merchantCategory,
        String ipAddress,
        Instant createdAt
    ) {
        return new RuleContext(new TransactionCreatedPayload(
            1L,
            "tx-1",
            10L,
            "FW-ACC-10",
            amount,
            "USD",
            "Merchant",
            merchantCategory,
            "DEBIT",
            "PENDING_REVIEW",
            createdAt,
            "device-1",
            ipAddress
        ));
    }
}
