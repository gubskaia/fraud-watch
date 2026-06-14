package com.fraudwatch.audit.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceInfoController {

    @GetMapping("/internal/info")
    public Map<String, String> info() {
        return Map.of("service", "audit-service", "status", "bootstrapped");
    }
}

