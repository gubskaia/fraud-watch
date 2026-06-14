package com.fraudwatch.notification.service;

import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.fraud.FraudDecisionPayload;
import com.fraudwatch.events.review.ReviewDecisionMadePayload;
import com.fraudwatch.notification.domain.Notification;
import com.fraudwatch.notification.domain.NotificationChannel;
import com.fraudwatch.notification.domain.NotificationTemplate;
import com.fraudwatch.notification.dto.NotificationResponse;
import com.fraudwatch.notification.exception.NotificationBusinessException;
import com.fraudwatch.notification.mapper.NotificationMapper;
import com.fraudwatch.notification.repository.DeliveryAttemptRepository;
import com.fraudwatch.notification.repository.NotificationRepository;
import com.fraudwatch.notification.repository.NotificationTemplateRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final NotificationMapper notificationMapper;
    private final MockDeliveryService mockDeliveryService;

    public NotificationService(
        NotificationRepository notificationRepository,
        NotificationTemplateRepository notificationTemplateRepository,
        DeliveryAttemptRepository deliveryAttemptRepository,
        NotificationMapper notificationMapper,
        MockDeliveryService mockDeliveryService
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.notificationMapper = notificationMapper;
        this.mockDeliveryService = mockDeliveryService;
    }

    @Transactional
    public void handleFraudDecision(EventEnvelope<FraudDecisionPayload> event) {
        if (notificationRepository.findByEventId(event.eventId()).isPresent()) {
            return;
        }

        String templateCode = switch (event.payload().decision()) {
            case "APPROVED" -> "TRANSACTION_APPROVED";
            case "BLOCKED" -> "TRANSACTION_BLOCKED";
            case "UNDER_REVIEW" -> "TRANSACTION_REVIEW_REQUIRED";
            default -> throw new NotificationBusinessException(HttpStatus.BAD_REQUEST, "Unsupported fraud decision");
        };

        String recipient = "account-%s".formatted(event.payload().accountId());
        Map<String, String> values = Map.of(
            "transactionReference", event.payload().transactionReference(),
            "decision", event.payload().decision(),
            "riskScore", String.valueOf(event.payload().riskScore())
        );
        createAndDeliver(event.eventId(), recipient, "TRANSACTION", String.valueOf(event.payload().transactionId()), templateCode, values);
    }

    @Transactional
    public void handleReviewDecision(EventEnvelope<ReviewDecisionMadePayload> event) {
        if (notificationRepository.findByEventId(event.eventId()).isPresent()) {
            return;
        }

        Map<String, String> values = Map.of(
            "transactionReference", event.payload().transactionReference(),
            "decision", event.payload().finalDecision(),
            "reasonCode", event.payload().reasonCode() == null ? "N/A" : event.payload().reasonCode(),
            "analyst", event.payload().analyst()
        );
        createAndDeliver(
            event.eventId(),
            "review-case-%s".formatted(event.payload().fraudCaseId()),
            "REVIEW_CASE",
            String.valueOf(event.payload().fraudCaseId()),
            "REVIEW_DECISION_MADE",
            values
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(notification -> notificationMapper.toResponse(
                notification,
                deliveryAttemptRepository.findAllByNotificationOrderByCreatedAtAsc(notification)
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getByRecipient(String recipientRef) {
        return notificationRepository.findAllByRecipientRefOrderByCreatedAtDesc(recipientRef)
            .stream()
            .map(notification -> notificationMapper.toResponse(
                notification,
                deliveryAttemptRepository.findAllByNotificationOrderByCreatedAtAsc(notification)
            ))
            .toList();
    }

    private void createAndDeliver(
        String eventId,
        String recipientRef,
        String relatedEntityType,
        String relatedEntityId,
        String templateCode,
        Map<String, String> values
    ) {
        NotificationTemplate template = notificationTemplateRepository.findByCodeAndActiveTrue(templateCode)
            .orElseThrow(() -> new NotificationBusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Notification template is missing"));

        Notification notification = new Notification();
        notification.setEventId(eventId);
        notification.setRecipientRef(recipientRef);
        notification.setChannel(NotificationChannel.IN_APP);
        notification.setTemplate(template);
        notification.setSubject(render(template.getSubjectTemplate(), values));
        notification.setBody(render(template.getBodyTemplate(), values));
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);

        Notification saved = notificationRepository.save(notification);
        mockDeliveryService.deliver(saved, "Mock in-app delivery completed successfully");
    }

    private String render(String template, Map<String, String> values) {
        String rendered = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }
}

