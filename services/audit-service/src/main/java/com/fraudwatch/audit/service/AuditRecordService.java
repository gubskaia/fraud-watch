package com.fraudwatch.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.audit.domain.AuditRecord;
import com.fraudwatch.audit.dto.AuditRecordResponse;
import com.fraudwatch.audit.mapper.AuditMapper;
import com.fraudwatch.audit.repository.AuditRecordRepository;
import com.fraudwatch.events.EventEnvelope;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditRecordService {

    private final AuditRecordRepository auditRecordRepository;
    private final AuditMapper auditMapper;
    private final ObjectMapper objectMapper;

    public AuditRecordService(
        AuditRecordRepository auditRecordRepository,
        AuditMapper auditMapper,
        ObjectMapper objectMapper
    ) {
        this.auditRecordRepository = auditRecordRepository;
        this.auditMapper = auditMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public <T> void storeEvent(
        EventEnvelope<T> event,
        String aggregateType,
        String aggregateId,
        String source,
        String summary
    ) {
        if (auditRecordRepository.findByEventId(event.eventId()).isPresent()) {
            return;
        }

        AuditRecord record = new AuditRecord();
        record.setEventId(event.eventId());
        record.setEventType(event.eventType());
        record.setAggregateType(aggregateType);
        record.setAggregateId(aggregateId);
        record.setCorrelationId(event.correlationId());
        record.setSource(source);
        record.setSummary(summary);
        record.setPayloadJson(writePayload(event.payload()));
        record.setOccurredAt(event.occurredAt() == null ? Instant.now() : event.occurredAt());

        auditRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<AuditRecordResponse> getAllRecords() {
        return auditRecordRepository.findAllByOrderByOccurredAtDesc()
            .stream()
            .map(auditMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditRecordResponse> getByAggregate(String aggregateType, String aggregateId) {
        return auditRecordRepository.findAllByAggregateTypeAndAggregateIdOrderByOccurredAtDesc(aggregateType, aggregateId)
            .stream()
            .map(auditMapper::toResponse)
            .toList();
    }

    private String writePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize audit payload", exception);
        }
    }
}

