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
class LocationAnomalySimulationRuleEvaluatorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LocationAnomalySimulationRuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new LocationAnomalySimulationRuleEvaluator(stringRedisTemplate);
    }

    @Test
    void shouldMatchWhenRegionChangesForAccount() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("fraud:location:account:10")).thenReturn("ipv4-region-203-0");

        assertThat(evaluator.evaluate(rule(), context("198.51.100.77")))
            .isPresent()
            .get()
            .extracting(RuleMatch::ruleCode, RuleMatch::scoreContribution)
            .containsExactly("LOCATION_ANOMALY_SIMULATION", 20);

        verify(valueOperations).set(eq("fraud:location:account:10"), eq("ipv4-region-198-51"), any(Duration.class));
    }

    @Test
    void shouldIgnoreFirstSeenRegion() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("fraud:location:account:10")).thenReturn(null);

        assertThat(evaluator.evaluate(rule(), context("198.51.100.77"))).isEmpty();
    }

    @Test
    void shouldIgnoreSameRegion() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("fraud:location:account:10")).thenReturn("ipv4-region-198-51");

        assertThat(evaluator.evaluate(rule(), context("198.51.200.42"))).isEmpty();
    }

    @Test
    void shouldIgnoreBlankIpAddress() {
        assertThat(evaluator.evaluate(rule(), context(" "))).isEmpty();
    }

    @Test
    void shouldDeriveRegionForIpv6Address() {
        assertThat(LocationAnomalySimulationRuleEvaluator.simulatedRegion("2001:db8::1"))
            .isEqualTo("ipv6-region-2001-db8");
    }

    private FraudRule rule() {
        FraudRule rule = new FraudRule();
        rule.setCode("LOCATION_ANOMALY_SIMULATION");
        rule.setWeight(20);
        rule.setEnabled(true);
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
