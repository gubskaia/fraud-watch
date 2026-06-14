package com.fraudwatch.notification.repository;

import com.fraudwatch.notification.domain.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByEventId(String eventId);

    @EntityGraph(attributePaths = "template")
    List<Notification> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "template")
    List<Notification> findAllByRecipientRefOrderByCreatedAtDesc(String recipientRef);
}

