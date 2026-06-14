package com.fraudwatch.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "analyst_comments")
public class AnalystComment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fraud_case_id", nullable = false)
    private FraudCase fraudCase;

    @Column(nullable = false, length = 120)
    private String analyst;

    @Column(nullable = false, length = 1000)
    private String comment;
}

