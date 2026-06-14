package com.fraudwatch.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "audit_records")
public class AuditRecord extends BaseEntity {

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(nullable = false, length = 120)
    private String source;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Column(name = "payload_json", nullable = false, length = 8000)
    private String payloadJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}

