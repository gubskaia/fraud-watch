package com.fraudwatch.fraud.service;

import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.fraud.FraudDecisionPayload;
import com.fraudwatch.fraud.config.RabbitConfig;
import com.fraudwatch.fraud.domain.FraudDecision;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class FraudDecisionPublisher {

    private final RabbitTemplate rabbitTemplate;

    public FraudDecisionPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(FraudDecision decision, List<String> triggeredRules, List<String> explanations, String correlationId) {
        FraudDecisionPayload payload = new FraudDecisionPayload(
            decision.getTransactionId(),
            decision.getTransactionReference(),
            decision.getAccountId(),
            decision.getRiskScore(),
            decision.getDecision().name(),
            triggeredRules,
            explanations,
            decision.getDecidedAt()
        );

        EventEnvelope<FraudDecisionPayload> event = new EventEnvelope<>(
            UUID.randomUUID().toString(),
            eventType(decision),
            "v1",
            Instant.now(),
            correlationId,
            Map.of("service", "fraud-service"),
            payload
        );

        rabbitTemplate.convertAndSend(
            RabbitConfig.FRAUD_EXCHANGE,
            routingKey(decision),
            event
        );
    }

    private String eventType(FraudDecision decision) {
        return switch (decision.getDecision()) {
            case APPROVED -> "TransactionApproved";
            case BLOCKED -> "TransactionBlocked";
            case UNDER_REVIEW -> "TransactionReviewRequired";
        };
    }

    private String routingKey(FraudDecision decision) {
        return switch (decision.getDecision()) {
            case APPROVED -> RabbitConfig.APPROVED_ROUTING_KEY;
            case BLOCKED -> RabbitConfig.BLOCKED_ROUTING_KEY;
            case UNDER_REVIEW -> RabbitConfig.REVIEW_REQUIRED_ROUTING_KEY;
        };
    }
}

