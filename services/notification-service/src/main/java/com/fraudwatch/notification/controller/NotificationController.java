package com.fraudwatch.notification.controller;

import com.fraudwatch.notification.dto.NotificationResponse;
import com.fraudwatch.notification.service.NotificationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> getNotifications(
        @RequestParam(name = "recipientRef", required = false) String recipientRef
    ) {
        if (recipientRef != null && !recipientRef.isBlank()) {
            return notificationService.getByRecipient(recipientRef);
        }
        return notificationService.getAllNotifications();
    }
}
