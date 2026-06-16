package com.fraudwatch.fraud.service;

import com.fraudwatch.events.transaction.TransactionCreatedPayload;
import com.fraudwatch.fraud.domain.FraudDecision;
import com.fraudwatch.fraud.domain.FraudDecisionStatus;
import com.fraudwatch.fraud.domain.FraudRule;
import com.fraudwatch.fraud.repository.FraudDecisionRepository;
import com.fraudwatch.fraud.repository.FraudRuleRepository;
import com.fraudwatch.fraud.rules.FraudRuleEvaluator;
import com.fraudwatch.fraud.rules.RuleContext;
import com.fraudwatch.fraud.rules.RuleMatch;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudScoringService {

    private final FraudRuleRepository fraudRuleRepository;
    private final FraudDecisionRepository fraudDecisionRepository;
    private final Map<String, FraudRuleEvaluator> evaluatorsByCode;
    private final FraudDecisionPublisher fraudDecisionPublisher;
    private final Executor fraudRuleExecutor;

    public FraudScoringService(
        FraudRuleRepository fraudRuleRepository,
        FraudDecisionRepository fraudDecisionRepository,
        List<FraudRuleEvaluator> evaluators,
        FraudDecisionPublisher fraudDecisionPublisher,
        @Qualifier("fraudRuleExecutor") Executor fraudRuleExecutor
    ) {
        this.fraudRuleRepository = fraudRuleRepository;
        this.fraudDecisionRepository = fraudDecisionRepository;
        this.evaluatorsByCode = evaluators.stream()
            .collect(Collectors.toMap(FraudRuleEvaluator::supportedRuleCode, Function.identity()));
        this.fraudDecisionPublisher = fraudDecisionPublisher;
        this.fraudRuleExecutor = fraudRuleExecutor;
    }

    @Transactional
    public void processTransactionCreated(TransactionCreatedPayload payload, String correlationId) {
        if (fraudDecisionRepository.findByTransactionId(payload.transactionId()).isPresent()) {
            return;
        }

        RuleContext context = new RuleContext(payload);
        List<RuleMatch> matches = evaluateRulesInParallel(fraudRuleRepository.findAllByEnabledTrue(), context);

        int riskScore = matches.stream().mapToInt(RuleMatch::scoreContribution).sum();
        FraudDecisionStatus decisionStatus = classify(riskScore);
        List<String> triggeredRules = matches.stream().map(RuleMatch::ruleCode).toList();
        List<String> explanations = matches.stream().map(RuleMatch::explanation).toList();

        FraudDecision decision = new FraudDecision();
        decision.setTransactionId(payload.transactionId());
        decision.setTransactionReference(payload.transactionReference());
        decision.setAccountId(payload.accountId());
        decision.setRiskScore(riskScore);
        decision.setDecision(decisionStatus);
        decision.setTriggeredRules(String.join("|", triggeredRules));
        decision.setExplanations(String.join("|", explanations));
        decision.setDecidedAt(Instant.now());

        FraudDecision savedDecision = fraudDecisionRepository.save(decision);
        fraudDecisionPublisher.publish(savedDecision, triggeredRules, explanations, correlationId);
    }

    private List<RuleMatch> evaluateRulesInParallel(List<FraudRule> rules, RuleContext context) {
        List<CompletableFuture<RuleMatch>> futures = rules.stream()
            .map(rule -> {
                FraudRuleEvaluator evaluator = evaluatorsByCode.get(rule.getCode());
                if (evaluator == null) {
                    return null;
                }
                return CompletableFuture.supplyAsync(
                    () -> evaluator.evaluate(rule, context).orElse(null),
                    fraudRuleExecutor
                );
            })
            .filter(java.util.Objects::nonNull)
            .toList();

        try {
            return futures.stream()
                .map(CompletableFuture::join)
                .filter(java.util.Objects::nonNull)
                .toList();
        } catch (CompletionException exception) {
            throw new IllegalStateException("Fraud rule execution failed", exception.getCause());
        }
    }

    private FraudDecisionStatus classify(int riskScore) {
        if (riskScore >= 70) {
            return FraudDecisionStatus.BLOCKED;
        }
        if (riskScore >= 30) {
            return FraudDecisionStatus.UNDER_REVIEW;
        }
        return FraudDecisionStatus.APPROVED;
    }
}
