package com.fraudwatch.review.messaging;

import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.review.ReviewDecisionMadePayload;
import com.fraudwatch.review.config.RabbitConfig;
import com.fraudwatch.review.domain.FraudCase;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReviewDecisionPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ReviewDecisionPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(FraudCase fraudCase, String analyst) {
        ReviewDecisionMadePayload payload = new ReviewDecisionMadePayload(
            fraudCase.getId(),
            fraudCase.getTransactionId(),
            fraudCase.getTransactionReference(),
            fraudCase.getStatus().name(),
            fraudCase.getReasonCode() == null ? null : fraudCase.getReasonCode().getCode(),
            analyst,
            fraudCase.getDecisionAt()
        );

        EventEnvelope<ReviewDecisionMadePayload> event = new EventEnvelope<>(
            UUID.randomUUID().toString(),
            "ReviewDecisionMade",
            "v1",
            Instant.now(),
            null,
            Map.of("service", "review-service"),
            payload
        );

        rabbitTemplate.convertAndSend(
            RabbitConfig.REVIEW_EXCHANGE,
            RabbitConfig.REVIEW_DECISION_ROUTING_KEY,
            event
        );
    }
}

