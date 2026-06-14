package com.fraudwatch.fraud.repository;

import com.fraudwatch.fraud.domain.FraudDecision;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudDecisionRepository extends JpaRepository<FraudDecision, Long> {

    Optional<FraudDecision> findByTransactionId(Long transactionId);
}
