package com.fraudwatch.transaction.messaging;

import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.transaction.TransactionStatusChangedPayload;
import com.fraudwatch.transaction.config.RabbitConfig;
import com.fraudwatch.transaction.domain.Transaction;
import com.fraudwatch.transaction.domain.TransactionStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionStatusEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public TransactionStatusEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(
        Transaction transaction,
        TransactionStatus previousStatus,
        TransactionStatus newStatus,
        String reason,
        String correlationId
    ) {
        TransactionStatusChangedPayload payload = new TransactionStatusChangedPayload(
            transaction.getId(),
            transaction.getTransactionReference(),
            transaction.getAccount().getId(),
            previousStatus.name(),
            newStatus.name(),
            reason,
            Instant.now()
        );

        EventEnvelope<TransactionStatusChangedPayload> event = new EventEnvelope<>(
            UUID.randomUUID().toString(),
            "TransactionStatusChanged",
            "v1",
            Instant.now(),
            correlationId,
            Map.of("service", "transaction-service"),
            payload
        );

        rabbitTemplate.convertAndSend(
            RabbitConfig.TRANSACTION_EXCHANGE,
            RabbitConfig.TRANSACTION_STATUS_CHANGED_ROUTING_KEY,
            event
        );
    }
}

