package com.fraudwatch.notification.service;

import com.fraudwatch.notification.domain.DeliveryAttempt;
import com.fraudwatch.notification.domain.Notification;
import com.fraudwatch.notification.domain.NotificationStatus;
import com.fraudwatch.notification.repository.DeliveryAttemptRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MockDeliveryService {

    private final DeliveryAttemptRepository deliveryAttemptRepository;

    public MockDeliveryService(DeliveryAttemptRepository deliveryAttemptRepository) {
        this.deliveryAttemptRepository = deliveryAttemptRepository;
    }

    @Transactional
    public void deliver(Notification notification, String details) {
        DeliveryAttempt attempt = new DeliveryAttempt();
        attempt.setNotification(notification);
        attempt.setAttemptNumber(deliveryAttemptRepository.findAllByNotificationOrderByCreatedAtAsc(notification).size() + 1);
        attempt.setStatus(NotificationStatus.DELIVERED);
        attempt.setDetails(details);
        deliveryAttemptRepository.save(attempt);

        notification.setStatus(NotificationStatus.DELIVERED);
        notification.setLastAttemptAt(Instant.now());
    }
}

