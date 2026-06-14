package com.fraudwatch.audit.mapper;

import com.fraudwatch.audit.domain.AuditRecord;
import com.fraudwatch.audit.dto.AuditRecordResponse;
import org.springframework.stereotype.Component;

@Component
public class AuditMapper {

    public AuditRecordResponse toResponse(AuditRecord record) {
        return new AuditRecordResponse(
            record.getId(),
            record.getEventId(),
            record.getEventType(),
            record.getAggregateType(),
            record.getAggregateId(),
            record.getCorrelationId(),
            record.getSource(),
            record.getSummary(),
            record.getPayloadJson(),
            record.getOccurredAt(),
            record.getCreatedAt()
        );
    }
}
