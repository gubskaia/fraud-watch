package com.fraudwatch.transaction.service;

import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.fraud.FraudDecisionPayload;
import com.fraudwatch.events.review.ReviewDecisionMadePayload;
import com.fraudwatch.transaction.domain.Transaction;
import com.fraudwatch.transaction.domain.TransactionStatus;
import com.fraudwatch.transaction.messaging.TransactionStatusEventPublisher;
import com.fraudwatch.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionLifecycleService {

    private final TransactionRepository transactionRepository;
    private final TransactionStatusEventPublisher transactionStatusEventPublisher;

    public TransactionLifecycleService(
        TransactionRepository transactionRepository,
        TransactionStatusEventPublisher transactionStatusEventPublisher
    ) {
        this.transactionRepository = transactionRepository;
        this.transactionStatusEventPublisher = transactionStatusEventPublisher;
    }

    @Transactional
    public void applyFraudDecision(EventEnvelope<FraudDecisionPayload> event) {
        Transaction transaction = transactionRepository.findDetailedById(event.payload().transactionId())
            .orElse(null);
        if (transaction == null) {
            return;
        }

        TransactionStatus newStatus = mapFraudDecision(event.payload().decision());
        applyStatusChange(transaction, newStatus, "Fraud decision: " + event.payload().decision(), event.correlationId());
    }

    @Transactional
    public void applyReviewDecision(EventEnvelope<ReviewDecisionMadePayload> event) {
        Transaction transaction = transactionRepository.findByTransactionReference(event.payload().transactionReference())
            .flatMap(found -> transactionRepository.findDetailedById(found.getId()))
            .orElse(null);
        if (transaction == null) {
            return;
        }

        TransactionStatus newStatus = mapReviewDecision(event.payload().finalDecision());
        applyStatusChange(
            transaction,
            newStatus,
            "Manual review decision: " + event.payload().finalDecision(),
            event.correlationId()
        );
    }

    private void applyStatusChange(
        Transaction transaction,
        TransactionStatus newStatus,
        String reason,
        String correlationId
    ) {
        TransactionStatus previousStatus = transaction.getStatus();
        if (previousStatus == newStatus) {
            return;
        }
        transaction.setStatus(newStatus);
        transactionStatusEventPublisher.publish(transaction, previousStatus, newStatus, reason, correlationId);
    }

    private TransactionStatus mapFraudDecision(String decision) {
        return switch (decision) {
            case "APPROVED" -> TransactionStatus.APPROVED;
            case "BLOCKED" -> TransactionStatus.BLOCKED;
            case "UNDER_REVIEW" -> TransactionStatus.UNDER_REVIEW;
            default -> TransactionStatus.PENDING_REVIEW;
        };
    }

    private TransactionStatus mapReviewDecision(String decision) {
        return switch (decision) {
            case "APPROVED" -> TransactionStatus.APPROVED;
            case "BLOCKED" -> TransactionStatus.BLOCKED;
            default -> TransactionStatus.UNDER_REVIEW;
        };
    }
}
