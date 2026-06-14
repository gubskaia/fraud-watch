package com.fraudwatch.fraud.repository;

import com.fraudwatch.fraud.domain.FraudRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudRuleRepository extends JpaRepository<FraudRule, Long> {

    List<FraudRule> findAllByEnabledTrue();
}

