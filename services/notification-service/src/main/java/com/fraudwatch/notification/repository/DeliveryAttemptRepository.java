package com.fraudwatch.notification.repository;

import com.fraudwatch.notification.domain.DeliveryAttempt;
import com.fraudwatch.notification.domain.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, Long> {

    List<DeliveryAttempt> findAllByNotificationOrderByCreatedAtAsc(Notification notification);
}
