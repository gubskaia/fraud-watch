package com.fraudwatch.review.mapper;

import com.fraudwatch.review.domain.AnalystComment;
import com.fraudwatch.review.domain.FraudCase;
import com.fraudwatch.review.domain.ReviewAction;
import com.fraudwatch.review.dto.ReviewActionResponse;
import com.fraudwatch.review.dto.ReviewCaseResponse;
import com.fraudwatch.review.dto.ReviewCommentResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewCaseResponse toReviewCaseResponse(
        FraudCase fraudCase,
        List<AnalystComment> comments,
        List<ReviewAction> actions
    ) {
        return new ReviewCaseResponse(
            fraudCase.getId(),
            fraudCase.getTransactionId(),
            fraudCase.getTransactionReference(),
            fraudCase.getAccountId(),
            fraudCase.getRiskScore(),
            splitValues(fraudCase.getTriggeredRules()),
            splitValues(fraudCase.getExplanations()),
            fraudCase.getStatus().name(),
            fraudCase.getAssignedTo(),
            fraudCase.getReasonCode() == null ? null : fraudCase.getReasonCode().getCode(),
            fraudCase.getDecisionAt(),
            comments.stream().map(this::toCommentResponse).toList(),
            actions.stream().map(this::toActionResponse).toList(),
            fraudCase.getCreatedAt()
        );
    }

    public ReviewCommentResponse toCommentResponse(AnalystComment comment) {
        return new ReviewCommentResponse(
            comment.getId(),
            comment.getAnalyst(),
            comment.getComment(),
            comment.getCreatedAt()
        );
    }

    public ReviewActionResponse toActionResponse(ReviewAction action) {
        return new ReviewActionResponse(
            action.getId(),
            action.getActionType().name(),
            action.getAnalyst(),
            action.getReasonCode() == null ? null : action.getReasonCode().getCode(),
            action.getDetails(),
            action.getCreatedAt()
        );
    }

    private List<String> splitValues(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("\\|"))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }
}
