package com.fraudwatch.transaction.messaging;

import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.transaction.TransactionCreatedPayload;
import com.fraudwatch.transaction.config.RabbitConfig;
import com.fraudwatch.transaction.domain.Transaction;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventPublisher {

    private static final String EVENT_TYPE = "TransactionCreated";
    private static final String EVENT_VERSION = "v1";

    private final RabbitTemplate rabbitTemplate;

    public TransactionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishTransactionCreated(Transaction transaction, String correlationId) {
        TransactionCreatedPayload payload = new TransactionCreatedPayload(
            transaction.getId(),
            transaction.getTransactionReference(),
            transaction.getAccount().getId(),
            transaction.getAccount().getAccountNumber(),
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getMerchantName(),
            transaction.getMerchantCategory(),
            transaction.getDirection().name(),
            transaction.getStatus().name(),
            transaction.getCreatedAt()
        );

        EventEnvelope<TransactionCreatedPayload> event = new EventEnvelope<>(
            UUID.randomUUID().toString(),
            EVENT_TYPE,
            EVENT_VERSION,
            Instant.now(),
            correlationId,
            Map.of("service", "transaction-service"),
            payload
        );

        rabbitTemplate.convertAndSend(
            RabbitConfig.TRANSACTION_EXCHANGE,
            RabbitConfig.TRANSACTION_CREATED_ROUTING_KEY,
            event
        );
    }
}

