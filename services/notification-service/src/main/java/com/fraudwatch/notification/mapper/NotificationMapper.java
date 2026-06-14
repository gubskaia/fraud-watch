package com.fraudwatch.notification.mapper;

import com.fraudwatch.notification.domain.DeliveryAttempt;
import com.fraudwatch.notification.domain.Notification;
import com.fraudwatch.notification.dto.DeliveryAttemptResponse;
import com.fraudwatch.notification.dto.NotificationResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification, List<DeliveryAttempt> attempts) {
        return new NotificationResponse(
            notification.getId(),
            notification.getEventId(),
            notification.getRecipientRef(),
            notification.getChannel().name(),
            notification.getSubject(),
            notification.getBody(),
            notification.getStatus().name(),
            notification.getRelatedEntityType(),
            notification.getRelatedEntityId(),
            notification.getLastAttemptAt(),
            notification.getTemplate() == null ? null : notification.getTemplate().getCode(),
            attempts.stream().map(this::toAttemptResponse).toList(),
            notification.getCreatedAt()
        );
    }

    public DeliveryAttemptResponse toAttemptResponse(DeliveryAttempt attempt) {
        return new DeliveryAttemptResponse(
            attempt.getId(),
            attempt.getAttemptNumber(),
            attempt.getStatus().name(),
            attempt.getDetails(),
            attempt.getCreatedAt()
        );
    }
}

