package com.fraudwatch.audit.repository;

import com.fraudwatch.audit.domain.AuditRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    Optional<AuditRecord> findByEventId(String eventId);

    List<AuditRecord> findAllByOrderByOccurredAtDesc();

    List<AuditRecord> findAllByAggregateTypeAndAggregateIdOrderByOccurredAtDesc(String aggregateType, String aggregateId);
}

