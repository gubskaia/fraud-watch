package com.fraudwatch.fraud.service;

import com.fraudwatch.fraud.dto.FraudDecisionResponse;
import com.fraudwatch.fraud.exception.FraudBusinessException;
import com.fraudwatch.fraud.mapper.FraudMapper;
import com.fraudwatch.fraud.repository.FraudDecisionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudDecisionQueryService {

    private final FraudDecisionRepository fraudDecisionRepository;
    private final FraudMapper fraudMapper;

    public FraudDecisionQueryService(FraudDecisionRepository fraudDecisionRepository, FraudMapper fraudMapper) {
        this.fraudDecisionRepository = fraudDecisionRepository;
        this.fraudMapper = fraudMapper;
    }

    @Transactional(readOnly = true)
    public FraudDecisionResponse getDecision(Long transactionId) {
        return fraudDecisionRepository.findByTransactionId(transactionId)
            .map(fraudMapper::toDecisionResponse)
            .orElseThrow(() -> new FraudBusinessException(HttpStatus.NOT_FOUND, "Fraud decision was not found"));
    }
}
