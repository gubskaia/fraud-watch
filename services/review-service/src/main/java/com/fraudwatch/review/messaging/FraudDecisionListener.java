package com.fraudwatch.review.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.fraud.FraudDecisionPayload;
import com.fraudwatch.review.config.RabbitConfig;
import com.fraudwatch.review.service.ReviewCaseService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class FraudDecisionListener {

    private final ObjectMapper objectMapper;
    private final ReviewCaseService reviewCaseService;

    public FraudDecisionListener(ObjectMapper objectMapper, ReviewCaseService reviewCaseService) {
        this.objectMapper = objectMapper;
        this.reviewCaseService = reviewCaseService;
    }

    @RabbitListener(queues = RabbitConfig.REVIEW_REQUIRED_QUEUE)
    public void handle(byte[] rawMessage) {
        try {
            EventEnvelope<FraudDecisionPayload> event = objectMapper.readValue(
                rawMessage,
                new TypeReference<>() {
                }
            );
            reviewCaseService.createCaseFromFraudDecision(event.payload());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to parse review-required event", exception);
        }
    }
}

