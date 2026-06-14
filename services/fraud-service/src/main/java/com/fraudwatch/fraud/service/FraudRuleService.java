package com.fraudwatch.fraud.service;

import com.fraudwatch.fraud.domain.FraudRule;
import com.fraudwatch.fraud.dto.FraudRuleResponse;
import com.fraudwatch.fraud.dto.UpdateFraudRuleRequest;
import com.fraudwatch.fraud.exception.FraudBusinessException;
import com.fraudwatch.fraud.mapper.FraudMapper;
import com.fraudwatch.fraud.repository.FraudRuleRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudRuleService {

    private final FraudRuleRepository fraudRuleRepository;
    private final FraudMapper fraudMapper;

    public FraudRuleService(FraudRuleRepository fraudRuleRepository, FraudMapper fraudMapper) {
        this.fraudRuleRepository = fraudRuleRepository;
        this.fraudMapper = fraudMapper;
    }

    @Transactional(readOnly = true)
    public List<FraudRuleResponse> getRules() {
        return fraudRuleRepository.findAll()
            .stream()
            .map(fraudMapper::toRuleResponse)
            .toList();
    }

    @Transactional
    public FraudRuleResponse updateRule(Long ruleId, UpdateFraudRuleRequest request) {
        FraudRule rule = fraudRuleRepository.findById(ruleId)
            .orElseThrow(() -> new FraudBusinessException(HttpStatus.NOT_FOUND, "Fraud rule was not found"));

        rule.setEnabled(request.enabled());
        rule.setWeight(request.weight());
        return fraudMapper.toRuleResponse(rule);
    }
}

