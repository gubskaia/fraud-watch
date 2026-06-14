package com.fraudwatch.audit.controller;

import com.fraudwatch.audit.dto.AuditRecordResponse;
import com.fraudwatch.audit.service.AuditRecordService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditRecordService auditRecordService;

    public AuditController(AuditRecordService auditRecordService) {
        this.auditRecordService = auditRecordService;
    }

    @GetMapping("/records")
    public List<AuditRecordResponse> getRecords(
        @RequestParam(name = "aggregateType", required = false) String aggregateType,
        @RequestParam(name = "aggregateId", required = false) String aggregateId
    ) {
        if (aggregateType != null && aggregateId != null) {
            return auditRecordService.getByAggregate(aggregateType, aggregateId);
        }
        return auditRecordService.getAllRecords();
    }

    @GetMapping("/records/{aggregateType}/{aggregateId}")
    public List<AuditRecordResponse> getRecordsByAggregate(
        @PathVariable String aggregateType,
        @PathVariable String aggregateId
    ) {
        return auditRecordService.getByAggregate(aggregateType, aggregateId);
    }
}
