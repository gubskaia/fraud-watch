package com.fraudwatch.review.repository;

import com.fraudwatch.review.domain.AnalystComment;
import com.fraudwatch.review.domain.FraudCase;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalystCommentRepository extends JpaRepository<AnalystComment, Long> {

    List<AnalystComment> findAllByFraudCaseOrderByCreatedAtAsc(FraudCase fraudCase);
}

