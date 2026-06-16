package com.fraudwatch.transaction.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.fraud.FraudDecisionPayload;
import com.fraudwatch.events.review.ReviewDecisionMadePayload;
import com.fraudwatch.transaction.config.RabbitConfig;
import com.fraudwatch.transaction.service.TransactionLifecycleService;
import java.io.IOException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionDecisionListener {

    private final ObjectMapper objectMapper;
    private final TransactionLifecycleService transactionLifecycleService;

    public TransactionDecisionListener(
        ObjectMapper objectMapper,
        TransactionLifecycleService transactionLifecycleService
    ) {
        this.objectMapper = objectMapper;
        this.transactionLifecycleService = transactionLifecycleService;
    }

    @RabbitListener(queues = RabbitConfig.FRAUD_APPROVED_QUEUE)
    public void handleFraudApproved(byte[] rawMessage) {
        transactionLifecycleService.applyFraudDecision(readFraudEvent(rawMessage));
    }

    @RabbitListener(queues = RabbitConfig.FRAUD_BLOCKED_QUEUE)
    public void handleFraudBlocked(byte[] rawMessage) {
        transactionLifecycleService.applyFraudDecision(readFraudEvent(rawMessage));
    }

    @RabbitListener(queues = RabbitConfig.FRAUD_REVIEW_REQUIRED_QUEUE)
    public void handleFraudReviewRequired(byte[] rawMessage) {
        transactionLifecycleService.applyFraudDecision(readFraudEvent(rawMessage));
    }

    @RabbitListener(queues = RabbitConfig.REVIEW_DECISION_QUEUE)
    public void handleReviewDecision(byte[] rawMessage) {
        try {
            EventEnvelope<ReviewDecisionMadePayload> event = objectMapper.readValue(
                rawMessage,
                new TypeReference<>() {
                }
            );
            transactionLifecycleService.applyReviewDecision(event);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to parse review decision event", exception);
        }
    }

    private EventEnvelope<FraudDecisionPayload> readFraudEvent(byte[] rawMessage) {
        try {
            return objectMapper.readValue(
                rawMessage,
                new TypeReference<>() {
                }
            );
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to parse fraud decision event", exception);
        }
    }
}
