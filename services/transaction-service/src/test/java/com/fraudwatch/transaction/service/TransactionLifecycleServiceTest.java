package com.fraudwatch.transaction.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.fraud.FraudDecisionPayload;
import com.fraudwatch.events.review.ReviewDecisionMadePayload;
import com.fraudwatch.transaction.domain.Account;
import com.fraudwatch.transaction.domain.Transaction;
import com.fraudwatch.transaction.domain.TransactionStatus;
import com.fraudwatch.transaction.messaging.TransactionStatusEventPublisher;
import com.fraudwatch.transaction.repository.TransactionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionLifecycleServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionStatusEventPublisher transactionStatusEventPublisher;

    private TransactionLifecycleService transactionLifecycleService;

    @BeforeEach
    void setUp() {
        transactionLifecycleService = new TransactionLifecycleService(transactionRepository, transactionStatusEventPublisher);
    }

    @Test
    void shouldApplyFraudApprovedDecision() {
        Transaction transaction = transaction(TransactionStatus.PENDING_REVIEW);
        when(transactionRepository.findDetailedById(1L)).thenReturn(Optional.of(transaction));

        transactionLifecycleService.applyFraudDecision(fraudEvent("APPROVED"));

        verify(transactionStatusEventPublisher).publish(
            eq(transaction),
            eq(TransactionStatus.PENDING_REVIEW),
            eq(TransactionStatus.APPROVED),
            eq("Fraud decision: APPROVED"),
            eq("corr-1")
        );
    }

    @Test
    void shouldApplyFraudReviewRequiredDecision() {
        Transaction transaction = transaction(TransactionStatus.PENDING_REVIEW);
        when(transactionRepository.findDetailedById(1L)).thenReturn(Optional.of(transaction));

        transactionLifecycleService.applyFraudDecision(fraudEvent("UNDER_REVIEW"));

        verify(transactionStatusEventPublisher).publish(
            eq(transaction),
            eq(TransactionStatus.PENDING_REVIEW),
            eq(TransactionStatus.UNDER_REVIEW),
            eq("Fraud decision: UNDER_REVIEW"),
            eq("corr-1")
        );
    }

    @Test
    void shouldApplyManualReviewDecision() {
        Transaction transaction = transaction(TransactionStatus.UNDER_REVIEW);
        when(transactionRepository.findByTransactionReference("tx-ref-1")).thenReturn(Optional.of(transaction));
        when(transactionRepository.findDetailedById(1L)).thenReturn(Optional.of(transaction));

        transactionLifecycleService.applyReviewDecision(reviewEvent("BLOCKED"));

        verify(transactionStatusEventPublisher).publish(
            eq(transaction),
            eq(TransactionStatus.UNDER_REVIEW),
            eq(TransactionStatus.BLOCKED),
            eq("Manual review decision: BLOCKED"),
            eq("corr-2")
        );
    }

    @Test
    void shouldNotPublishWhenStatusDoesNotChange() {
        Transaction transaction = transaction(TransactionStatus.APPROVED);
        when(transactionRepository.findDetailedById(1L)).thenReturn(Optional.of(transaction));

        transactionLifecycleService.applyFraudDecision(fraudEvent("APPROVED"));

        verify(transactionStatusEventPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    private Transaction transaction(TransactionStatus status) {
        Account account = new Account();
        account.setId(10L);
        account.setAccountNumber("ACC-1");

        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setAccount(account);
        transaction.setTransactionReference("tx-ref-1");
        transaction.setStatus(status);
        return transaction;
    }

    private EventEnvelope<FraudDecisionPayload> fraudEvent(String decision) {
        return new EventEnvelope<>(
            "event-1",
            "FraudDecision",
            "v1",
            Instant.now(),
            "corr-1",
            Map.of("service", "fraud-service"),
            new FraudDecisionPayload(
                1L,
                "tx-ref-1",
                10L,
                42,
                decision,
                List.of("RULE_1"),
                List.of("explanation"),
                Instant.now()
            )
        );
    }

    private EventEnvelope<ReviewDecisionMadePayload> reviewEvent(String decision) {
        return new EventEnvelope<>(
            "event-2",
            "ReviewDecisionMade",
            "v1",
            Instant.now(),
            "corr-2",
            Map.of("service", "review-service"),
            new ReviewDecisionMadePayload(
                5L,
                1L,
                "tx-ref-1",
                decision,
                "CONFIRMED_FRAUD",
                "analyst-1",
                Instant.now()
            )
        );
    }
}
