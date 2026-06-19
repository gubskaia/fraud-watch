package com.fraudwatch.fraud.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.transaction.TransactionStatusChangedPayload;
import com.fraudwatch.fraud.config.RabbitConfig;
import com.fraudwatch.fraud.service.FailedAttemptTrackingService;
import java.io.IOException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionStatusChangedListener {

    private final ObjectMapper objectMapper;
    private final FailedAttemptTrackingService failedAttemptTrackingService;

    public TransactionStatusChangedListener(
        ObjectMapper objectMapper,
        FailedAttemptTrackingService failedAttemptTrackingService
    ) {
        this.objectMapper = objectMapper;
        this.failedAttemptTrackingService = failedAttemptTrackingService;
    }

    @RabbitListener(queues = RabbitConfig.TRANSACTION_STATUS_CHANGED_QUEUE)
    public void handle(byte[] rawMessage) {
        try {
            EventEnvelope<TransactionStatusChangedPayload> event = objectMapper.readValue(
                rawMessage,
                new TypeReference<>() {
                }
            );
            failedAttemptTrackingService.recordStatusChange(event.payload());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to parse TransactionStatusChanged event", exception);
        }
    }
}
