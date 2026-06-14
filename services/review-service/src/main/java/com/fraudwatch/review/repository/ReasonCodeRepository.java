package com.fraudwatch.review.repository;

import com.fraudwatch.review.domain.ReasonCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReasonCodeRepository extends JpaRepository<ReasonCode, Long> {

    Optional<ReasonCode> findByCodeAndActiveTrue(String code);
}

