package com.fraudwatch.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fraud_cases")
public class FraudCase extends BaseEntity {

    @Column(name = "transaction_id", nullable = false, unique = true)
    private Long transactionId;

    @Column(name = "transaction_reference", nullable = false, length = 100)
    private String transactionReference;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "triggered_rules", nullable = false, length = 1000)
    private String triggeredRules;

    @Column(nullable = false, length = 3000)
    private String explanations;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FraudCaseStatus status = FraudCaseStatus.OPEN;

    @Column(name = "assigned_to", length = 120)
    private String assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reason_code_id")
    private ReasonCode reasonCode;

    @Column(name = "decision_at")
    private Instant decisionAt;
}

