package com.fraudwatch.review.repository;

import com.fraudwatch.review.domain.FraudCase;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudCaseRepository extends JpaRepository<FraudCase, Long> {

    @EntityGraph(attributePaths = "reasonCode")
    List<FraudCase> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "reasonCode")
    Optional<FraudCase> findDetailedById(Long id);

    Optional<FraudCase> findByTransactionId(Long transactionId);
}

