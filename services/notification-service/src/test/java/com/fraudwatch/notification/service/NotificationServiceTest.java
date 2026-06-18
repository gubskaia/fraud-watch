package com.fraudwatch.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.fraud.FraudDecisionPayload;
import com.fraudwatch.events.review.ReviewDecisionMadePayload;
import com.fraudwatch.notification.domain.Notification;
import com.fraudwatch.notification.domain.NotificationChannel;
import com.fraudwatch.notification.domain.NotificationTemplate;
import com.fraudwatch.notification.exception.NotificationBusinessException;
import com.fraudwatch.notification.mapper.NotificationMapper;
import com.fraudwatch.notification.repository.DeliveryAttemptRepository;
import com.fraudwatch.notification.repository.NotificationRepository;
import com.fraudwatch.notification.repository.NotificationTemplateRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationTemplateRepository notificationTemplateRepository;

    @Mock
    private DeliveryAttemptRepository deliveryAttemptRepository;

    @Mock
    private MockDeliveryService mockDeliveryService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
            notificationRepository,
            notificationTemplateRepository,
            deliveryAttemptRepository,
            new NotificationMapper(),
            mockDeliveryService
        );
    }

    @Test
    void shouldCreateApprovedNotificationFromFraudDecision() {
        EventEnvelope<FraudDecisionPayload> event = fraudEvent("APPROVED");
        NotificationTemplate template = template(
            "TRANSACTION_APPROVED",
            "Transaction {{transactionReference}} approved",
            "Decision {{decision}} with score {{riskScore}}"
        );

        when(notificationRepository.findByEventId("event-1")).thenReturn(Optional.empty());
        when(notificationTemplateRepository.findByCodeAndActiveTrue("TRANSACTION_APPROVED"))
            .thenReturn(Optional.of(template));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.handleFraudDecision(event);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();

        assertThat(saved.getEventId()).isEqualTo("event-1");
        assertThat(saved.getRecipientRef()).isEqualTo("account-101");
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(saved.getRelatedEntityType()).isEqualTo("TRANSACTION");
        assertThat(saved.getRelatedEntityId()).isEqualTo("11");
        assertThat(saved.getSubject()).isEqualTo("Transaction tx-11 approved");
        assertThat(saved.getBody()).isEqualTo("Decision APPROVED with score 42");

        verify(mockDeliveryService).deliver(saved, "Mock in-app delivery completed successfully");
    }

    @Test
    void shouldCreateReviewDecisionNotificationWithFallbackReasonCode() {
        EventEnvelope<ReviewDecisionMadePayload> event = new EventEnvelope<>(
            "event-2",
            "ReviewDecisionMade",
            "v1",
            Instant.parse("2026-06-18T12:00:00Z"),
            "corr-2",
            Map.of(),
            new ReviewDecisionMadePayload(7L, 11L, "tx-11", "APPROVED", null, "analyst-1", Instant.now())
        );
        NotificationTemplate template = template(
            "REVIEW_DECISION_MADE",
            "Review completed for {{transactionReference}}",
            "Decision {{decision}} by {{analyst}} with reason {{reasonCode}}"
        );

        when(notificationRepository.findByEventId("event-2")).thenReturn(Optional.empty());
        when(notificationTemplateRepository.findByCodeAndActiveTrue("REVIEW_DECISION_MADE"))
            .thenReturn(Optional.of(template));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.handleReviewDecision(event);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();

        assertThat(saved.getRecipientRef()).isEqualTo("review-case-7");
        assertThat(saved.getRelatedEntityType()).isEqualTo("REVIEW_CASE");
        assertThat(saved.getRelatedEntityId()).isEqualTo("7");
        assertThat(saved.getBody()).isEqualTo("Decision APPROVED by analyst-1 with reason N/A");
    }

    @Test
    void shouldIgnoreDuplicateFraudDecisionEvent() {
        when(notificationRepository.findByEventId("event-1")).thenReturn(Optional.of(new Notification()));

        notificationService.handleFraudDecision(fraudEvent("BLOCKED"));

        verify(notificationTemplateRepository, never()).findByCodeAndActiveTrue(any());
        verify(notificationRepository, never()).save(any());
        verify(mockDeliveryService, never()).deliver(any(), any());
    }

    @Test
    void shouldRejectUnsupportedFraudDecision() {
        when(notificationRepository.findByEventId("event-1")).thenReturn(Optional.empty());

        NotificationBusinessException exception = assertThrows(
            NotificationBusinessException.class,
            () -> notificationService.handleFraudDecision(fraudEvent("ESCALATED"))
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getMessage()).isEqualTo("Unsupported fraud decision");
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenTemplateIsMissing() {
        when(notificationRepository.findByEventId("event-1")).thenReturn(Optional.empty());
        when(notificationTemplateRepository.findByCodeAndActiveTrue("TRANSACTION_BLOCKED")).thenReturn(Optional.empty());

        NotificationBusinessException exception = assertThrows(
            NotificationBusinessException.class,
            () -> notificationService.handleFraudDecision(fraudEvent("BLOCKED"))
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getMessage()).isEqualTo("Notification template is missing");
        verify(notificationRepository, never()).save(any());
        verify(mockDeliveryService, never()).deliver(any(), any());
    }

    private EventEnvelope<FraudDecisionPayload> fraudEvent(String decision) {
        return new EventEnvelope<>(
            "event-1",
            "FraudDecisionMade",
            "v1",
            Instant.parse("2026-06-18T12:00:00Z"),
            "corr-1",
            Map.of(),
            new FraudDecisionPayload(
                11L,
                "tx-11",
                101L,
                42,
                decision,
                List.of("RULE_A"),
                List.of("Explanation A"),
                Instant.parse("2026-06-18T12:00:00Z")
            )
        );
    }

    private NotificationTemplate template(String code, String subjectTemplate, String bodyTemplate) {
        NotificationTemplate template = new NotificationTemplate();
        template.setCode(code);
        template.setSubjectTemplate(subjectTemplate);
        template.setBodyTemplate(bodyTemplate);
        template.setActive(true);
        return template;
    }
}
