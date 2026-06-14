package com.fraudwatch.notification.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.fraud.FraudDecisionPayload;
import com.fraudwatch.events.review.ReviewDecisionMadePayload;
import com.fraudwatch.notification.config.RabbitConfig;
import com.fraudwatch.notification.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public NotificationEventListener(ObjectMapper objectMapper, NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_APPROVED_QUEUE)
    public void handleApproved(byte[] rawMessage) {
        notificationService.handleFraudDecision(readFraudEvent(rawMessage));
    }

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_BLOCKED_QUEUE)
    public void handleBlocked(byte[] rawMessage) {
        notificationService.handleFraudDecision(readFraudEvent(rawMessage));
    }

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_REVIEW_REQUIRED_QUEUE)
    public void handleReviewRequired(byte[] rawMessage) {
        notificationService.handleFraudDecision(readFraudEvent(rawMessage));
    }

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_REVIEW_DECISION_QUEUE)
    public void handleReviewDecision(byte[] rawMessage) {
        try {
            EventEnvelope<ReviewDecisionMadePayload> event = objectMapper.readValue(
                rawMessage,
                new TypeReference<>() {
                }
            );
            notificationService.handleReviewDecision(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to parse review notification event", exception);
        }
    }

    private EventEnvelope<FraudDecisionPayload> readFraudEvent(byte[] rawMessage) {
        try {
            return objectMapper.readValue(
                rawMessage,
                new TypeReference<>() {
                }
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to parse fraud notification event", exception);
        }
    }
}

