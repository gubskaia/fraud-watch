package com.fraudwatch.review.repository;

import com.fraudwatch.review.domain.FraudCase;
import com.fraudwatch.review.domain.ReviewAction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewActionRepository extends JpaRepository<ReviewAction, Long> {

    List<ReviewAction> findAllByFraudCaseOrderByCreatedAtAsc(FraudCase fraudCase);
}
