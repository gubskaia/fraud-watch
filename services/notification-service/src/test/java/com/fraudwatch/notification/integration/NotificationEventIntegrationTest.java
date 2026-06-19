package com.fraudwatch.notification.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.events.EventEnvelope;
import com.fraudwatch.events.fraud.FraudDecisionPayload;
import com.fraudwatch.events.review.ReviewDecisionMadePayload;
import com.fraudwatch.notification.domain.DeliveryAttempt;
import com.fraudwatch.notification.domain.Notification;
import com.fraudwatch.notification.domain.NotificationStatus;
import com.fraudwatch.notification.messaging.NotificationEventListener;
import com.fraudwatch.notification.repository.DeliveryAttemptRepository;
import com.fraudwatch.notification.repository.NotificationRepository;
import com.fraudwatch.test.InfrastructureContainers;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class NotificationEventIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = InfrastructureContainers.postgres("notification_db_integration_test");

    @Container
    static final RabbitMQContainer rabbitMq = InfrastructureContainers.rabbitMq();

    @Autowired
    private NotificationEventListener notificationEventListener;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DeliveryAttemptRepository deliveryAttemptRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitMq::getHost);
        registry.add("spring.rabbitmq.port", rabbitMq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMq::getAdminPassword);
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> false);
        registry.add("spring.rabbitmq.listener.direct.auto-startup", () -> false);
    }

    @BeforeEach
    void cleanState() {
        deliveryAttemptRepository.deleteAll();
        notificationRepository.deleteAll();
    }

    @Test
    void shouldPersistApprovedFraudDecisionNotificationOnce() throws Exception {
        EventEnvelope<FraudDecisionPayload> event = approvedFraudDecisionEvent();
        byte[] payload = objectMapper.writeValueAsBytes(event);

        notificationEventListener.handleApproved(payload);
        notificationEventListener.handleApproved(payload);

        assertThat(notificationRepository.findAll()).hasSize(1);

        Notification notification = notificationRepository.findByEventId(event.eventId()).orElseThrow();
        assertThat(notification.getRecipientRef()).isEqualTo("account-5001");
        assertThat(notification.getRelatedEntityType()).isEqualTo("TRANSACTION");
        assertThat(notification.getRelatedEntityId()).isEqualTo("101");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
        assertThat(notification.getSubject()).isEqualTo("Transaction tx-notify-101 approved");
        assertThat(notification.getBody()).isEqualTo(
            "Your transaction tx-notify-101 was approved with risk score 22."
        );
        assertThat(notification.getLastAttemptAt()).isNotNull();

        List<DeliveryAttempt> attempts = deliveryAttemptRepository.findAllByNotificationOrderByCreatedAtAsc(notification);
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getAttemptNumber()).isEqualTo(1);
        assertThat(attempts.get(0).getStatus()).isEqualTo(NotificationStatus.DELIVERED);
    }

    @Test
    void shouldPersistReviewDecisionNotificationWithRenderedReason() throws Exception {
        EventEnvelope<ReviewDecisionMadePayload> event = reviewDecisionEvent();

        notificationEventListener.handleReviewDecision(objectMapper.writeValueAsBytes(event));

        Notification notification = notificationRepository.findByEventId(event.eventId()).orElseThrow();
        assertThat(notification.getRecipientRef()).isEqualTo("review-case-77");
        assertThat(notification.getRelatedEntityType()).isEqualTo("REVIEW_CASE");
        assertThat(notification.getRelatedEntityId()).isEqualTo("77");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
        assertThat(notification.getSubject()).isEqualTo("Review completed for tx-notify-101");
        assertThat(notification.getBody()).isEqualTo(
            "Manual review is complete. Final decision: BLOCKED. Reason: CONFIRMED_FRAUD. Analyst: analyst-it."
        );
    }

    private EventEnvelope<FraudDecisionPayload> approvedFraudDecisionEvent() {
        return new EventEnvelope<>(
            UUID.randomUUID().toString(),
            "FraudDecisionMade",
            "v1",
            Instant.parse("2026-06-19T11:00:00Z"),
            "corr-notify-fraud",
            Map.of("service", "fraud-service"),
            new FraudDecisionPayload(
                101L,
                "tx-notify-101",
                5001L,
                22,
                "APPROVED",
                List.of("LOW_RISK_PROFILE"),
                List.of("No high-risk indicators found"),
                Instant.parse("2026-06-19T11:00:00Z")
            )
        );
    }

    private EventEnvelope<ReviewDecisionMadePayload> reviewDecisionEvent() {
        return new EventEnvelope<>(
            UUID.randomUUID().toString(),
            "ReviewDecisionMade",
            "v1",
            Instant.parse("2026-06-19T11:20:00Z"),
            "corr-notify-review",
            Map.of("service", "review-service"),
            new ReviewDecisionMadePayload(
                77L,
                101L,
                "tx-notify-101",
                "BLOCKED",
                "CONFIRMED_FRAUD",
                "analyst-it",
                Instant.parse("2026-06-19T11:20:00Z")
            )
        );
    }
}
