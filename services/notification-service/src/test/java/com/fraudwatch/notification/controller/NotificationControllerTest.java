package com.fraudwatch.notification.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fraudwatch.notification.config.SecurityConfig;
import com.fraudwatch.notification.dto.DeliveryAttemptResponse;
import com.fraudwatch.notification.dto.NotificationResponse;
import com.fraudwatch.notification.exception.NotificationBusinessException;
import com.fraudwatch.notification.exception.NotificationExceptionHandler;
import com.fraudwatch.notification.service.NotificationService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = NotificationController.class)
@Import({SecurityConfig.class, NotificationExceptionHandler.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    void shouldReturnAllNotificationsWhenRecipientFilterIsMissing() throws Exception {
        when(notificationService.getAllNotifications()).thenReturn(List.of(notificationResponse("account-101")));

        mockMvc.perform(get("/api/notifications"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(41))
            .andExpect(jsonPath("$[0].recipientRef").value("account-101"))
            .andExpect(jsonPath("$[0].templateCode").value("TRANSACTION_APPROVED"))
            .andExpect(jsonPath("$[0].attempts[0].status").value("DELIVERED"));

        verify(notificationService).getAllNotifications();
        verify(notificationService, never()).getByRecipient(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldReturnNotificationsFilteredByRecipient() throws Exception {
        when(notificationService.getByRecipient("account-202")).thenReturn(List.of(notificationResponse("account-202")));

        mockMvc.perform(get("/api/notifications").param("recipientRef", "account-202"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].recipientRef").value("account-202"))
            .andExpect(jsonPath("$[0].relatedEntityType").value("TRANSACTION"));

        verify(notificationService).getByRecipient("account-202");
        verify(notificationService, never()).getAllNotifications();
    }

    @Test
    void shouldTreatBlankRecipientFilterAsMissing() throws Exception {
        when(notificationService.getAllNotifications()).thenReturn(List.of(notificationResponse("account-101")));

        mockMvc.perform(get("/api/notifications").param("recipientRef", "   "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].recipientRef").value("account-101"));

        verify(notificationService).getAllNotifications();
        verify(notificationService, never()).getByRecipient(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldTranslateBusinessError() throws Exception {
        when(notificationService.getByRecipient("broken-recipient"))
            .thenThrow(new NotificationBusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Notification template is missing"));

        mockMvc.perform(get("/api/notifications").param("recipientRef", "broken-recipient"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Notification template is missing"))
            .andExpect(jsonPath("$.path").value("/api/notifications"));
    }

    private NotificationResponse notificationResponse(String recipientRef) {
        return new NotificationResponse(
            41L,
            "event-123",
            recipientRef,
            "IN_APP",
            "Transaction approved",
            "Your transaction tx-11 was approved",
            "DELIVERED",
            "TRANSACTION",
            "11",
            Instant.parse("2026-06-18T12:05:00Z"),
            "TRANSACTION_APPROVED",
            List.of(new DeliveryAttemptResponse(
                81L,
                1,
                "DELIVERED",
                "Mock in-app delivery completed successfully",
                Instant.parse("2026-06-18T12:04:00Z")
            )),
            Instant.parse("2026-06-18T12:03:00Z")
        );
    }
}
