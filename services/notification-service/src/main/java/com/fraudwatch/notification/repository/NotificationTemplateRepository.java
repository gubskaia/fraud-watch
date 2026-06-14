package com.fraudwatch.notification.repository;

import com.fraudwatch.notification.domain.NotificationTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByCodeAndActiveTrue(String code);
}

