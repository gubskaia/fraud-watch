package com.fraudwatch.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.audit.domain.AuditRecord;
import com.fraudwatch.audit.dto.AuditRecordResponse;
import com.fraudwatch.audit.mapper.AuditMapper;
import com.fraudwatch.audit.repository.AuditRecordRepository;
import com.fraudwatch.events.EventEnvelope;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditRecordServiceTest {

    @Mock
    private AuditRecordRepository auditRecordRepository;

    @Mock
    private AuditMapper auditMapper;

    @Mock
    private ObjectMapper objectMapper;

    private AuditRecordService auditRecordService;

    @BeforeEach
    void setUp() {
        auditRecordService = new AuditRecordService(auditRecordRepository, auditMapper, objectMapper);
    }

    @Test
    void shouldStoreNewEvent() throws Exception {
        EventEnvelope<Map<String, Object>> event = new EventEnvelope<>(
            "event-1",
            "transaction.created",
            "v1",
            Instant.parse("2026-06-18T09:00:00Z"),
            "corr-1",
            Map.of("tenant", "demo"),
            Map.of("transactionId", 101, "status", "PENDING_REVIEW")
        );
        when(auditRecordRepository.findByEventId("event-1")).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(event.payload())).thenReturn("{\"transactionId\":101,\"status\":\"PENDING_REVIEW\"}");

        auditRecordService.storeEvent(
            event,
            "TRANSACTION",
            "101",
            "transaction-service",
            "Transaction created and submitted for fraud evaluation"
        );

        ArgumentCaptor<AuditRecord> recordCaptor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecordRepository).save(recordCaptor.capture());

        AuditRecord savedRecord = recordCaptor.getValue();
        assertThat(savedRecord.getEventId()).isEqualTo("event-1");
        assertThat(savedRecord.getEventType()).isEqualTo("transaction.created");
        assertThat(savedRecord.getAggregateType()).isEqualTo("TRANSACTION");
        assertThat(savedRecord.getAggregateId()).isEqualTo("101");
        assertThat(savedRecord.getCorrelationId()).isEqualTo("corr-1");
        assertThat(savedRecord.getSource()).isEqualTo("transaction-service");
        assertThat(savedRecord.getSummary()).isEqualTo("Transaction created and submitted for fraud evaluation");
        assertThat(savedRecord.getPayloadJson()).isEqualTo("{\"transactionId\":101,\"status\":\"PENDING_REVIEW\"}");
        assertThat(savedRecord.getOccurredAt()).isEqualTo(Instant.parse("2026-06-18T09:00:00Z"));
    }

    @Test
    void shouldIgnoreDuplicateEventId() {
        EventEnvelope<Map<String, Object>> event = new EventEnvelope<>(
            "event-duplicate",
            "transaction.created",
            "v1",
            Instant.parse("2026-06-18T09:00:00Z"),
            "corr-2",
            Map.of(),
            Map.of("transactionId", 101)
        );
        when(auditRecordRepository.findByEventId("event-duplicate")).thenReturn(Optional.of(new AuditRecord()));

        auditRecordService.storeEvent(event, "TRANSACTION", "101", "transaction-service", "Ignored duplicate");

        verify(auditRecordRepository, never()).save(any());
    }

    @Test
    void shouldUseCurrentTimestampWhenEventOccurredAtIsMissing() throws Exception {
        EventEnvelope<Map<String, Object>> event = new EventEnvelope<>(
            "event-2",
            "review.decision.made",
            "v1",
            null,
            "corr-3",
            Map.of(),
            Map.of("fraudCaseId", 77)
        );
        when(auditRecordRepository.findByEventId("event-2")).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(event.payload())).thenReturn("{\"fraudCaseId\":77}");

        auditRecordService.storeEvent(
            event,
            "REVIEW_CASE",
            "77",
            "review-service",
            "Analyst finalized manual review decision"
        );

        ArgumentCaptor<AuditRecord> recordCaptor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getOccurredAt()).isNotNull();
    }

    @Test
    void shouldTranslateSerializationFailure() throws Exception {
        EventEnvelope<Map<String, Object>> event = new EventEnvelope<>(
            "event-3",
            "fraud.blocked",
            "v1",
            Instant.parse("2026-06-18T09:00:00Z"),
            "corr-4",
            Map.of(),
            Map.of("decision", "BLOCK")
        );
        when(auditRecordRepository.findByEventId("event-3")).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(event.payload())).thenThrow(new JsonProcessingException("boom") {});

        assertThatThrownBy(() -> auditRecordService.storeEvent(
            event,
            "TRANSACTION",
            "404",
            "fraud-service",
            "Fraud engine blocked transaction"
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unable to serialize audit payload");
    }

    @Test
    void shouldReturnAllMappedRecords() {
        AuditRecord first = auditRecord(1L, "event-1", "TRANSACTION", "101");
        AuditRecord second = auditRecord(2L, "event-2", "REVIEW_CASE", "77");
        AuditRecordResponse firstResponse = response(1L, "event-1");
        AuditRecordResponse secondResponse = response(2L, "event-2");

        when(auditRecordRepository.findAllByOrderByOccurredAtDesc()).thenReturn(List.of(first, second));
        when(auditMapper.toResponse(first)).thenReturn(firstResponse);
        when(auditMapper.toResponse(second)).thenReturn(secondResponse);

        assertThat(auditRecordService.getAllRecords()).containsExactly(firstResponse, secondResponse);
    }

    @Test
    void shouldReturnRecordsFilteredByAggregate() {
        AuditRecord record = auditRecord(3L, "event-3", "TRANSACTION", "303");
        AuditRecordResponse response = response(3L, "event-3");

        when(auditRecordRepository.findAllByAggregateTypeAndAggregateIdOrderByOccurredAtDesc("TRANSACTION", "303"))
            .thenReturn(List.of(record));
        when(auditMapper.toResponse(record)).thenReturn(response);

        assertThat(auditRecordService.getByAggregate("TRANSACTION", "303")).containsExactly(response);
    }

    private AuditRecord auditRecord(Long id, String eventId, String aggregateType, String aggregateId) {
        AuditRecord record = new AuditRecord();
        record.setId(id);
        record.setEventId(eventId);
        record.setEventType("event.type");
        record.setAggregateType(aggregateType);
        record.setAggregateId(aggregateId);
        record.setCorrelationId("corr");
        record.setSource("service");
        record.setSummary("summary");
        record.setPayloadJson("{\"ok\":true}");
        record.setOccurredAt(Instant.parse("2026-06-18T09:00:00Z"));
        record.setCreatedAt(Instant.parse("2026-06-18T09:01:00Z"));
        return record;
    }

    private AuditRecordResponse response(Long id, String eventId) {
        return new AuditRecordResponse(
            id,
            eventId,
            "event.type",
            "TRANSACTION",
            "101",
            "corr",
            "service",
            "summary",
            "{\"ok\":true}",
            Instant.parse("2026-06-18T09:00:00Z"),
            Instant.parse("2026-06-18T09:01:00Z")
        );
    }
}
