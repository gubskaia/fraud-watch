package com.fraudwatch.review.service;

import com.fraudwatch.events.fraud.FraudDecisionPayload;
import com.fraudwatch.review.domain.AnalystComment;
import com.fraudwatch.review.domain.FraudCase;
import com.fraudwatch.review.domain.FraudCaseStatus;
import com.fraudwatch.review.domain.ReasonCode;
import com.fraudwatch.review.domain.ReviewAction;
import com.fraudwatch.review.domain.ReviewActionType;
import com.fraudwatch.review.dto.AnalystCommentRequest;
import com.fraudwatch.review.dto.AssignCaseRequest;
import com.fraudwatch.review.dto.ReviewCaseResponse;
import com.fraudwatch.review.dto.ReviewDecisionRequest;
import com.fraudwatch.review.exception.ReviewBusinessException;
import com.fraudwatch.review.mapper.ReviewMapper;
import com.fraudwatch.review.messaging.ReviewDecisionPublisher;
import com.fraudwatch.review.repository.AnalystCommentRepository;
import com.fraudwatch.review.repository.FraudCaseRepository;
import com.fraudwatch.review.repository.ReasonCodeRepository;
import com.fraudwatch.review.repository.ReviewActionRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewCaseService {

    private final FraudCaseRepository fraudCaseRepository;
    private final ReasonCodeRepository reasonCodeRepository;
    private final AnalystCommentRepository analystCommentRepository;
    private final ReviewActionRepository reviewActionRepository;
    private final ReviewMapper reviewMapper;
    private final ReviewDecisionPublisher reviewDecisionPublisher;

    public ReviewCaseService(
        FraudCaseRepository fraudCaseRepository,
        ReasonCodeRepository reasonCodeRepository,
        AnalystCommentRepository analystCommentRepository,
        ReviewActionRepository reviewActionRepository,
        ReviewMapper reviewMapper,
        ReviewDecisionPublisher reviewDecisionPublisher
    ) {
        this.fraudCaseRepository = fraudCaseRepository;
        this.reasonCodeRepository = reasonCodeRepository;
        this.analystCommentRepository = analystCommentRepository;
        this.reviewActionRepository = reviewActionRepository;
        this.reviewMapper = reviewMapper;
        this.reviewDecisionPublisher = reviewDecisionPublisher;
    }

    @Transactional
    public void createCaseFromFraudDecision(FraudDecisionPayload payload) {
        if (!"UNDER_REVIEW".equalsIgnoreCase(payload.decision())) {
            return;
        }
        if (fraudCaseRepository.findByTransactionId(payload.transactionId()).isPresent()) {
            return;
        }

        FraudCase fraudCase = new FraudCase();
        fraudCase.setTransactionId(payload.transactionId());
        fraudCase.setTransactionReference(payload.transactionReference());
        fraudCase.setAccountId(payload.accountId());
        fraudCase.setRiskScore(payload.riskScore());
        fraudCase.setTriggeredRules(String.join("|", payload.triggeredRules()));
        fraudCase.setExplanations(String.join("|", payload.explanations()));
        fraudCase.setStatus(FraudCaseStatus.OPEN);
        fraudCaseRepository.save(fraudCase);
    }

    @Transactional(readOnly = true)
    public List<ReviewCaseResponse> getCases() {
        return fraudCaseRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::mapCase)
            .toList();
    }

    @Transactional(readOnly = true)
    public ReviewCaseResponse getCase(Long caseId) {
        return mapCase(getCaseEntity(caseId));
    }

    @Transactional
    public ReviewCaseResponse assignCase(Long caseId, AssignCaseRequest request) {
        FraudCase fraudCase = getOpenCase(caseId);
        fraudCase.setAssignedTo(request.analyst().trim());

        reviewActionRepository.save(buildAction(
            fraudCase,
            ReviewActionType.ASSIGNED,
            request.analyst(),
            null,
            request.details()
        ));
        return mapCase(fraudCase);
    }

    @Transactional
    public ReviewCaseResponse approveCase(Long caseId, ReviewDecisionRequest request) {
        FraudCase fraudCase = getOpenCase(caseId);
        ReasonCode reasonCode = getReasonCode(request.reasonCode());

        fraudCase.setStatus(FraudCaseStatus.APPROVED);
        fraudCase.setAssignedTo(request.analyst().trim());
        fraudCase.setReasonCode(reasonCode);
        fraudCase.setDecisionAt(Instant.now());

        reviewActionRepository.save(buildAction(fraudCase, ReviewActionType.APPROVED, request.analyst(), reasonCode, request.details()));
        reviewDecisionPublisher.publish(fraudCase, request.analyst().trim());
        return mapCase(fraudCase);
    }

    @Transactional
    public ReviewCaseResponse blockCase(Long caseId, ReviewDecisionRequest request) {
        FraudCase fraudCase = getOpenCase(caseId);
        ReasonCode reasonCode = getReasonCode(request.reasonCode());

        fraudCase.setStatus(FraudCaseStatus.BLOCKED);
        fraudCase.setAssignedTo(request.analyst().trim());
        fraudCase.setReasonCode(reasonCode);
        fraudCase.setDecisionAt(Instant.now());

        reviewActionRepository.save(buildAction(fraudCase, ReviewActionType.BLOCKED, request.analyst(), reasonCode, request.details()));
        reviewDecisionPublisher.publish(fraudCase, request.analyst().trim());
        return mapCase(fraudCase);
    }

    @Transactional
    public ReviewCaseResponse addComment(Long caseId, AnalystCommentRequest request) {
        FraudCase fraudCase = getCaseEntity(caseId);

        AnalystComment comment = new AnalystComment();
        comment.setFraudCase(fraudCase);
        comment.setAnalyst(request.analyst().trim());
        comment.setComment(request.comment().trim());
        analystCommentRepository.save(comment);

        reviewActionRepository.save(buildAction(fraudCase, ReviewActionType.COMMENTED, request.analyst(), null, request.comment()));
        return mapCase(fraudCase);
    }

    private FraudCase getCaseEntity(Long caseId) {
        return fraudCaseRepository.findDetailedById(caseId)
            .orElseThrow(() -> new ReviewBusinessException(HttpStatus.NOT_FOUND, "Review case was not found"));
    }

    private FraudCase getOpenCase(Long caseId) {
        FraudCase fraudCase = getCaseEntity(caseId);
        if (fraudCase.getStatus() != FraudCaseStatus.OPEN) {
            throw new ReviewBusinessException(HttpStatus.CONFLICT, "Review case is already finalized");
        }
        return fraudCase;
    }

    private ReasonCode getReasonCode(String code) {
        return reasonCodeRepository.findByCodeAndActiveTrue(code.trim())
            .orElseThrow(() -> new ReviewBusinessException(HttpStatus.BAD_REQUEST, "Reason code is invalid"));
    }

    private ReviewAction buildAction(
        FraudCase fraudCase,
        ReviewActionType actionType,
        String analyst,
        ReasonCode reasonCode,
        String details
    ) {
        ReviewAction action = new ReviewAction();
        action.setFraudCase(fraudCase);
        action.setActionType(actionType);
        action.setAnalyst(analyst.trim());
        action.setReasonCode(reasonCode);
        action.setDetails(details == null ? null : details.trim());
        return action;
    }

    private ReviewCaseResponse mapCase(FraudCase fraudCase) {
        List<AnalystComment> comments = analystCommentRepository.findAllByFraudCaseOrderByCreatedAtAsc(fraudCase);
        List<ReviewAction> actions = reviewActionRepository.findAllByFraudCaseOrderByCreatedAtAsc(fraudCase);
        return reviewMapper.toReviewCaseResponse(fraudCase, comments, actions);
    }
}
