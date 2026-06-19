package com.fraudwatch.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fraudwatch.notification.domain.DeliveryAttempt;
import com.fraudwatch.notification.domain.Notification;
import com.fraudwatch.notification.domain.NotificationStatus;
import com.fraudwatch.notification.repository.DeliveryAttemptRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MockDeliveryServiceTest {

    @Mock
    private DeliveryAttemptRepository deliveryAttemptRepository;

    private MockDeliveryService mockDeliveryService;

    @BeforeEach
    void setUp() {
        mockDeliveryService = new MockDeliveryService(deliveryAttemptRepository);
    }

    @Test
    void shouldCreateDeliveredAttemptAndUpdateNotificationStatus() {
        Notification notification = new Notification();
        notification.setStatus(NotificationStatus.PENDING);
        notification.setLastAttemptAt(null);

        DeliveryAttempt existingAttempt = new DeliveryAttempt();
        existingAttempt.setAttemptNumber(1);
        when(deliveryAttemptRepository.findAllByNotificationOrderByCreatedAtAsc(notification))
            .thenReturn(List.of(existingAttempt));

        Instant beforeDelivery = Instant.now();
        mockDeliveryService.deliver(notification, "Mock in-app delivery completed successfully");

        ArgumentCaptor<DeliveryAttempt> attemptCaptor = ArgumentCaptor.forClass(DeliveryAttempt.class);
        verify(deliveryAttemptRepository).save(attemptCaptor.capture());

        DeliveryAttempt savedAttempt = attemptCaptor.getValue();
        assertThat(savedAttempt.getNotification()).isEqualTo(notification);
        assertThat(savedAttempt.getAttemptNumber()).isEqualTo(2);
        assertThat(savedAttempt.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
        assertThat(savedAttempt.getDetails()).isEqualTo("Mock in-app delivery completed successfully");

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
        assertThat(notification.getLastAttemptAt()).isAfterOrEqualTo(beforeDelivery);
    }
}
