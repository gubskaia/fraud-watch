package com.fraudwatch.fraud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fraudwatch.events.transaction.TransactionCreatedPayload;
import com.fraudwatch.fraud.domain.FraudDecision;
import com.fraudwatch.fraud.domain.FraudDecisionStatus;
import com.fraudwatch.fraud.domain.FraudRule;
import com.fraudwatch.fraud.repository.FraudDecisionRepository;
import com.fraudwatch.fraud.repository.FraudRuleRepository;
import com.fraudwatch.fraud.rules.FraudRuleEvaluator;
import com.fraudwatch.fraud.rules.RuleContext;
import com.fraudwatch.fraud.rules.RuleMatch;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FraudScoringServiceTest {

    @Mock
    private FraudRuleRepository fraudRuleRepository;

    @Mock
    private FraudDecisionRepository fraudDecisionRepository;

    @Mock
    private FraudDecisionPublisher fraudDecisionPublisher;

    private FraudScoringService fraudScoringService;

    @BeforeEach
    void setUp() {
        Executor directExecutor = Runnable::run;
        fraudScoringService = new FraudScoringService(
            fraudRuleRepository,
            fraudDecisionRepository,
            List.of(
                new MatchingEvaluator("RULE_A", "Rule A matched"),
                new MatchingEvaluator("RULE_B", "Rule B matched")
            ),
            fraudDecisionPublisher,
            directExecutor
        );
    }

    @Test
    void shouldAggregateTriggeredRulesIntoSingleDecision() {
        when(fraudDecisionRepository.findByTransactionId(10L)).thenReturn(Optional.empty());
        when(fraudRuleRepository.findAllByEnabledTrue()).thenReturn(List.of(rule("RULE_A", 20), rule("RULE_B", 15)));
        when(fraudDecisionRepository.save(any(FraudDecision.class))).thenAnswer(invocation -> invocation.getArgument(0));

        fraudScoringService.processTransactionCreated(payload(), "corr-test-1");

        ArgumentCaptor<FraudDecision> decisionCaptor = ArgumentCaptor.forClass(FraudDecision.class);
        verify(fraudDecisionRepository).save(decisionCaptor.capture());
        FraudDecision savedDecision = decisionCaptor.getValue();

        assertThat(savedDecision.getTransactionId()).isEqualTo(10L);
        assertThat(savedDecision.getRiskScore()).isEqualTo(35);
        assertThat(savedDecision.getDecision()).isEqualTo(FraudDecisionStatus.UNDER_REVIEW);
        assertThat(savedDecision.getTriggeredRules()).contains("RULE_A").contains("RULE_B");
        assertThat(savedDecision.getExplanations()).contains("Rule A matched").contains("Rule B matched");

        verify(fraudDecisionPublisher).publish(
            any(FraudDecision.class),
            eq(List.of("RULE_A", "RULE_B")),
            eq(List.of("Rule A matched", "Rule B matched")),
            eq("corr-test-1")
        );
    }

    private FraudRule rule(String code, int weight) {
        FraudRule rule = new FraudRule();
        rule.setCode(code);
        rule.setWeight(weight);
        return rule;
    }

    private TransactionCreatedPayload payload() {
        return new TransactionCreatedPayload(
            10L,
            "tx-10",
            55L,
            "FW-ACC-55",
            new BigDecimal("120.00"),
            "USD",
            "Merchant",
            "ECOM",
            "DEBIT",
            "PENDING_REVIEW",
            Instant.now(),
            "device-55",
            "203.0.113.55"
        );
    }

    private record MatchingEvaluator(String ruleCode, String explanation) implements FraudRuleEvaluator {

        @Override
        public String supportedRuleCode() {
            return ruleCode;
        }

        @Override
        public Optional<RuleMatch> evaluate(FraudRule rule, RuleContext context) {
            return Optional.of(new RuleMatch(rule.getCode(), rule.getWeight(), explanation));
        }
    }
}
