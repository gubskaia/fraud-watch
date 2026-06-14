package com.fraudwatch.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "notification_templates")
public class NotificationTemplate extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String subjectTemplate;

    @Column(nullable = false, length = 2000)
    private String bodyTemplate;

    @Column(nullable = false)
    private boolean active = true;
}

