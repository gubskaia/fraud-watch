package com.fraudwatch.auth.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceInfoController {

    @GetMapping("/internal/info")
    public Map<String, String> info() {
        return Map.of("service", "auth-service", "status", "bootstrapped");
    }
}

