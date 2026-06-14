package com.fraudwatch.fraud.controller;

import com.fraudwatch.fraud.dto.FraudDecisionResponse;
import com.fraudwatch.fraud.dto.FraudRuleResponse;
import com.fraudwatch.fraud.dto.UpdateFraudRuleRequest;
import com.fraudwatch.fraud.service.FraudDecisionQueryService;
import com.fraudwatch.fraud.service.FraudRuleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fraud")
public class FraudController {

    private final FraudRuleService fraudRuleService;
    private final FraudDecisionQueryService fraudDecisionQueryService;

    public FraudController(
        FraudRuleService fraudRuleService,
        FraudDecisionQueryService fraudDecisionQueryService
    ) {
        this.fraudRuleService = fraudRuleService;
        this.fraudDecisionQueryService = fraudDecisionQueryService;
    }

    @GetMapping("/rules")
    public List<FraudRuleResponse> getRules() {
        return fraudRuleService.getRules();
    }

    @PutMapping("/rules/{id}")
    public FraudRuleResponse updateRule(
        @PathVariable Long id,
        @Valid @RequestBody UpdateFraudRuleRequest request
    ) {
        return fraudRuleService.updateRule(id, request);
    }

    @GetMapping("/decisions/{transactionId}")
    public FraudDecisionResponse getDecision(@PathVariable Long transactionId) {
        return fraudDecisionQueryService.getDecision(transactionId);
    }
}
