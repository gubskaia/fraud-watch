package com.fraudwatch.fraud.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.transaction.TransactionCreatedPayload;
import com.fraudwatch.fraud.config.RabbitConfig;
import com.fraudwatch.fraud.service.FraudScoringService;
import java.io.IOException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionCreatedListener {

    private final ObjectMapper objectMapper;
    private final FraudScoringService fraudScoringService;

    public TransactionCreatedListener(ObjectMapper objectMapper, FraudScoringService fraudScoringService) {
        this.objectMapper = objectMapper;
        this.fraudScoringService = fraudScoringService;
    }

    @RabbitListener(queues = RabbitConfig.TRANSACTION_CREATED_QUEUE)
    public void handle(byte[] rawMessage) {
        try {
            EventEnvelope<TransactionCreatedPayload> event = objectMapper.readValue(
                rawMessage,
                new TypeReference<>() {
                }
            );
            fraudScoringService.processTransactionCreated(event.payload(), event.correlationId());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to parse TransactionCreated event", exception);
        }
    }
}
