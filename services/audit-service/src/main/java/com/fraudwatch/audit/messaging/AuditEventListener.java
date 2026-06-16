package com.fraudwatch.audit.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.audit.config.RabbitConfig;
import com.fraudwatch.audit.service.AuditRecordService;
import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.fraud.FraudDecisionPayload;
import com.fraudwatch.events.review.ReviewDecisionMadePayload;
import com.fraudwatch.events.transaction.TransactionCreatedPayload;
import java.io.IOException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener {

    private final ObjectMapper objectMapper;
    private final AuditRecordService auditRecordService;

    public AuditEventListener(ObjectMapper objectMapper, AuditRecordService auditRecordService) {
        this.objectMapper = objectMapper;
        this.auditRecordService = auditRecordService;
    }

    @RabbitListener(queues = RabbitConfig.AUDIT_TRANSACTION_CREATED_QUEUE)
    public void handleTransactionCreated(byte[] rawMessage) {
        EventEnvelope<TransactionCreatedPayload> event = read(rawMessage, new TypeReference<>() {});
        auditRecordService.storeEvent(
            event,
            "TRANSACTION",
            String.valueOf(event.payload().transactionId()),
            "transaction-service",
            "Transaction created and submitted for fraud evaluation"
        );
    }

    @RabbitListener(queues = RabbitConfig.AUDIT_TRANSACTION_APPROVED_QUEUE)
    public void handleTransactionApproved(byte[] rawMessage) {
        EventEnvelope<FraudDecisionPayload> event = read(rawMessage, new TypeReference<>() {});
        auditRecordService.storeEvent(
            event,
            "TRANSACTION",
            String.valueOf(event.payload().transactionId()),
            "fraud-service",
            "Fraud engine approved transaction"
        );
    }

    @RabbitListener(queues = RabbitConfig.AUDIT_TRANSACTION_BLOCKED_QUEUE)
    public void handleTransactionBlocked(byte[] rawMessage) {
        EventEnvelope<FraudDecisionPayload> event = read(rawMessage, new TypeReference<>() {});
        auditRecordService.storeEvent(
            event,
            "TRANSACTION",
            String.valueOf(event.payload().transactionId()),
            "fraud-service",
            "Fraud engine blocked transaction"
        );
    }

    @RabbitListener(queues = RabbitConfig.AUDIT_TRANSACTION_REVIEW_REQUIRED_QUEUE)
    public void handleTransactionReviewRequired(byte[] rawMessage) {
        EventEnvelope<FraudDecisionPayload> event = read(rawMessage, new TypeReference<>() {});
        auditRecordService.storeEvent(
            event,
            "TRANSACTION",
            String.valueOf(event.payload().transactionId()),
            "fraud-service",
            "Fraud engine escalated transaction for manual review"
        );
    }

    @RabbitListener(queues = RabbitConfig.AUDIT_REVIEW_DECISION_QUEUE)
    public void handleReviewDecisionMade(byte[] rawMessage) {
        EventEnvelope<ReviewDecisionMadePayload> event = read(rawMessage, new TypeReference<>() {});
        auditRecordService.storeEvent(
            event,
            "REVIEW_CASE",
            String.valueOf(event.payload().fraudCaseId()),
            "review-service",
            "Analyst finalized manual review decision"
        );
    }

    private <T> EventEnvelope<T> read(byte[] rawMessage, TypeReference<EventEnvelope<T>> typeReference) {
        try {
            return objectMapper.readValue(rawMessage, typeReference);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to parse audit event", exception);
        }
    }
}
