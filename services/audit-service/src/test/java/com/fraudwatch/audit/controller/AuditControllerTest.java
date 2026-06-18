package com.fraudwatch.audit.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fraudwatch.audit.config.SecurityConfig;
import com.fraudwatch.audit.dto.AuditRecordResponse;
import com.fraudwatch.audit.service.AuditRecordService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuditController.class)
@Import(SecurityConfig.class)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditRecordService auditRecordService;

    @Test
    void shouldReturnAllAuditRecordsWhenNoFilterIsProvided() throws Exception {
        when(auditRecordService.getAllRecords()).thenReturn(List.of(auditRecordResponse("TRANSACTION", "11")));

        mockMvc.perform(get("/api/audit/records"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].eventId").value("event-123"))
            .andExpect(jsonPath("$[0].aggregateType").value("TRANSACTION"))
            .andExpect(jsonPath("$[0].aggregateId").value("11"))
            .andExpect(jsonPath("$[0].summary").value("Transaction moved to UNDER_REVIEW"));

        verify(auditRecordService).getAllRecords();
        verify(auditRecordService, never()).getByAggregate(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldReturnRecordsFilteredByQueryParameters() throws Exception {
        when(auditRecordService.getByAggregate("TRANSACTION", "11"))
            .thenReturn(List.of(auditRecordResponse("TRANSACTION", "11")));

        mockMvc.perform(get("/api/audit/records")
                .param("aggregateType", "TRANSACTION")
                .param("aggregateId", "11"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].aggregateType").value("TRANSACTION"))
            .andExpect(jsonPath("$[0].aggregateId").value("11"))
            .andExpect(jsonPath("$[0].eventType").value("TransactionReviewRequired"));

        verify(auditRecordService).getByAggregate("TRANSACTION", "11");
        verify(auditRecordService, never()).getAllRecords();
    }

    @Test
    void shouldReturnRecordsFilteredByPath() throws Exception {
        when(auditRecordService.getByAggregate("REVIEW_CASE", "case-7"))
            .thenReturn(List.of(auditRecordResponse("REVIEW_CASE", "case-7")));

        mockMvc.perform(get("/api/audit/records/REVIEW_CASE/case-7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].aggregateType").value("REVIEW_CASE"))
            .andExpect(jsonPath("$[0].aggregateId").value("case-7"))
            .andExpect(jsonPath("$[0].source").value("review-service"));

        verify(auditRecordService).getByAggregate("REVIEW_CASE", "case-7");
    }

    private AuditRecordResponse auditRecordResponse(String aggregateType, String aggregateId) {
        return new AuditRecordResponse(
            51L,
            "event-123",
            "TransactionReviewRequired",
            aggregateType,
            aggregateId,
            "corr-789",
            "review-service",
            "Transaction moved to UNDER_REVIEW",
            "{\"transactionId\":11,\"decision\":\"UNDER_REVIEW\"}",
            Instant.parse("2026-06-18T12:10:00Z"),
            Instant.parse("2026-06-18T12:11:00Z")
        );
    }
}
