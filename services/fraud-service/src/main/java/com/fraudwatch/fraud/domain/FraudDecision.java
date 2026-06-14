package com.fraudwatch.fraud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fraud_decisions")
public class FraudDecision extends BaseEntity {

    @Column(name = "transaction_id", nullable = false, unique = true)
    private Long transactionId;

    @Column(name = "transaction_reference", nullable = false, length = 100)
    private String transactionReference;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FraudDecisionStatus decision;

    @Column(name = "triggered_rules", nullable = false, length = 1000)
    private String triggeredRules;

    @Column(nullable = false, length = 3000)
    private String explanations;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;
}

