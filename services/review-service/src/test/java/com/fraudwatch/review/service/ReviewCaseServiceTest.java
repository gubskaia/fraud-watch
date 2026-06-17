package com.fraudwatch.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ReviewCaseServiceTest {

    @Mock
    private FraudCaseRepository fraudCaseRepository;

    @Mock
    private ReasonCodeRepository reasonCodeRepository;

    @Mock
    private AnalystCommentRepository analystCommentRepository;

    @Mock
    private ReviewActionRepository reviewActionRepository;

    @Mock
    private ReviewDecisionPublisher reviewDecisionPublisher;

    private ReviewCaseService reviewCaseService;

    @BeforeEach
    void setUp() {
        reviewCaseService = new ReviewCaseService(
            fraudCaseRepository,
            reasonCodeRepository,
            analystCommentRepository,
            reviewActionRepository,
            new ReviewMapper(),
            reviewDecisionPublisher
        );
    }

    @Test
    void shouldCreateOpenCaseFromUnderReviewFraudDecision() {
        when(fraudCaseRepository.findByTransactionId(11L)).thenReturn(Optional.empty());
        when(fraudCaseRepository.save(any(FraudCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reviewCaseService.createCaseFromFraudDecision(new FraudDecisionPayload(
            11L,
            "tx-11",
            101L,
            42,
            "UNDER_REVIEW",
            List.of("RULE_A", "RULE_B"),
            List.of("Explanation A", "Explanation B"),
            Instant.now()
        ));

        ArgumentCaptor<FraudCase> caseCaptor = ArgumentCaptor.forClass(FraudCase.class);
        verify(fraudCaseRepository).save(caseCaptor.capture());
        FraudCase savedCase = caseCaptor.getValue();
        assertThat(savedCase.getTransactionId()).isEqualTo(11L);
        assertThat(savedCase.getStatus()).isEqualTo(FraudCaseStatus.OPEN);
        assertThat(savedCase.getTriggeredRules()).isEqualTo("RULE_A|RULE_B");
        assertThat(savedCase.getExplanations()).isEqualTo("Explanation A|Explanation B");
    }

    @Test
    void shouldApproveOpenCaseAndPublishDecision() {
        FraudCase fraudCase = openCase();
        ReasonCode reasonCode = reasonCode("LEGIT_ACTIVITY");

        when(fraudCaseRepository.findDetailedById(7L)).thenReturn(Optional.of(fraudCase));
        when(reasonCodeRepository.findByCodeAndActiveTrue("LEGIT_ACTIVITY")).thenReturn(Optional.of(reasonCode));
        when(analystCommentRepository.findAllByFraudCaseOrderByCreatedAtAsc(fraudCase)).thenReturn(List.of());
        when(reviewActionRepository.findAllByFraudCaseOrderByCreatedAtAsc(fraudCase)).thenAnswer(invocation -> {
            ReviewAction action = new ReviewAction();
            action.setActionType(ReviewActionType.APPROVED);
            action.setAnalyst("analyst-1");
            action.setReasonCode(reasonCode);
            action.setDetails("Looks legitimate");
            return List.of(action);
        });
        when(reviewActionRepository.save(any(ReviewAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewCaseResponse response = reviewCaseService.approveCase(
            7L,
            new ReviewDecisionRequest(" analyst-1 ", "LEGIT_ACTIVITY", "Looks legitimate")
        );

        assertThat(fraudCase.getStatus()).isEqualTo(FraudCaseStatus.APPROVED);
        assertThat(fraudCase.getAssignedTo()).isEqualTo("analyst-1");
        assertThat(fraudCase.getReasonCode()).isEqualTo(reasonCode);
        assertThat(fraudCase.getDecisionAt()).isNotNull();
        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.assignedTo()).isEqualTo("analyst-1");
        assertThat(response.reasonCode()).isEqualTo("LEGIT_ACTIVITY");

        verify(reviewDecisionPublisher).publish(fraudCase, "analyst-1");
    }

    @Test
    void shouldAssignOpenCaseWithoutPublishingDecision() {
        FraudCase fraudCase = openCase();
        when(fraudCaseRepository.findDetailedById(7L)).thenReturn(Optional.of(fraudCase));
        when(reviewActionRepository.save(any(ReviewAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(analystCommentRepository.findAllByFraudCaseOrderByCreatedAtAsc(fraudCase)).thenReturn(List.of());
        when(reviewActionRepository.findAllByFraudCaseOrderByCreatedAtAsc(fraudCase)).thenAnswer(invocation -> {
            ReviewAction action = new ReviewAction();
            action.setActionType(ReviewActionType.ASSIGNED);
            action.setAnalyst("analyst-queue-1");
            action.setDetails("Picked from analyst queue");
            return List.of(action);
        });

        ReviewCaseResponse response = reviewCaseService.assignCase(
            7L,
            new AssignCaseRequest(" analyst-queue-1 ", "Picked from analyst queue")
        );

        assertThat(fraudCase.getAssignedTo()).isEqualTo("analyst-queue-1");
        assertThat(response.assignedTo()).isEqualTo("analyst-queue-1");
        assertThat(response.actions()).hasSize(1);
        assertThat(response.actions().get(0).actionType()).isEqualTo("ASSIGNED");

        verify(reviewDecisionPublisher, never()).publish(any(), any());
        verify(reviewActionRepository).save(any(ReviewAction.class));
    }

    @Test
    void shouldBlockOpenCaseAndPublishDecision() {
        FraudCase fraudCase = openCase();
        ReasonCode reasonCode = reasonCode("CONFIRMED_FRAUD");

        when(fraudCaseRepository.findDetailedById(7L)).thenReturn(Optional.of(fraudCase));
        when(reasonCodeRepository.findByCodeAndActiveTrue("CONFIRMED_FRAUD")).thenReturn(Optional.of(reasonCode));
        when(analystCommentRepository.findAllByFraudCaseOrderByCreatedAtAsc(fraudCase)).thenReturn(List.of());
        when(reviewActionRepository.findAllByFraudCaseOrderByCreatedAtAsc(fraudCase)).thenAnswer(invocation -> {
            ReviewAction action = new ReviewAction();
            action.setActionType(ReviewActionType.BLOCKED);
            action.setAnalyst("analyst-3");
            action.setReasonCode(reasonCode);
            action.setDetails("Confirmed account takeover");
            return List.of(action);
        });
        when(reviewActionRepository.save(any(ReviewAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewCaseResponse response = reviewCaseService.blockCase(
            7L,
            new ReviewDecisionRequest(" analyst-3 ", "CONFIRMED_FRAUD", "Confirmed account takeover")
        );

        assertThat(fraudCase.getStatus()).isEqualTo(FraudCaseStatus.BLOCKED);
        assertThat(fraudCase.getAssignedTo()).isEqualTo("analyst-3");
        assertThat(fraudCase.getReasonCode()).isEqualTo(reasonCode);
        assertThat(fraudCase.getDecisionAt()).isNotNull();
        assertThat(response.status()).isEqualTo("BLOCKED");
        assertThat(response.reasonCode()).isEqualTo("CONFIRMED_FRAUD");

        verify(reviewDecisionPublisher).publish(fraudCase, "analyst-3");
    }

    @Test
    void shouldAddCommentWithoutPublishingDecision() {
        FraudCase fraudCase = openCase();
        when(fraudCaseRepository.findDetailedById(7L)).thenReturn(Optional.of(fraudCase));
        when(analystCommentRepository.save(any(AnalystComment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewActionRepository.save(any(ReviewAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(analystCommentRepository.findAllByFraudCaseOrderByCreatedAtAsc(fraudCase)).thenAnswer(invocation -> {
            AnalystComment comment = new AnalystComment();
            comment.setAnalyst("analyst-2");
            comment.setComment("Need one more check");
            return List.of(comment);
        });
        when(reviewActionRepository.findAllByFraudCaseOrderByCreatedAtAsc(fraudCase)).thenAnswer(invocation -> {
            ReviewAction action = new ReviewAction();
            action.setActionType(ReviewActionType.COMMENTED);
            action.setAnalyst("analyst-2");
            action.setDetails("Need one more check");
            return List.of(action);
        });

        ReviewCaseResponse response = reviewCaseService.addComment(
            7L,
            new AnalystCommentRequest(" analyst-2 ", " Need one more check ")
        );

        assertThat(response.comments()).hasSize(1);
        assertThat(response.comments().get(0).analyst()).isEqualTo("analyst-2");
        assertThat(response.actions()).hasSize(1);
        assertThat(response.actions().get(0).actionType()).isEqualTo("COMMENTED");

        verify(reviewDecisionPublisher, never()).publish(any(), any());
        verify(reviewActionRepository).save(any(ReviewAction.class));
        verify(analystCommentRepository).save(any(AnalystComment.class));
    }

    @Test
    void shouldRejectFinalizingAlreadyClosedCase() {
        FraudCase fraudCase = openCase();
        fraudCase.setStatus(FraudCaseStatus.APPROVED);
        when(fraudCaseRepository.findDetailedById(7L)).thenReturn(Optional.of(fraudCase));

        ReviewBusinessException exception = org.junit.jupiter.api.Assertions.assertThrows(
            ReviewBusinessException.class,
            () -> reviewCaseService.blockCase(
                7L,
                new ReviewDecisionRequest("analyst-3", "CONFIRMED_FRAUD", "Confirmed account takeover")
            )
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getMessage()).isEqualTo("Review case is already finalized");
        verify(reviewDecisionPublisher, never()).publish(any(), any());
        verify(reviewActionRepository, never()).save(any());
    }

    @Test
    void shouldRejectUnknownReasonCode() {
        FraudCase fraudCase = openCase();
        when(fraudCaseRepository.findDetailedById(7L)).thenReturn(Optional.of(fraudCase));
        when(reasonCodeRepository.findByCodeAndActiveTrue("UNKNOWN_CODE")).thenReturn(Optional.empty());

        ReviewBusinessException exception = org.junit.jupiter.api.Assertions.assertThrows(
            ReviewBusinessException.class,
            () -> reviewCaseService.approveCase(
                7L,
                new ReviewDecisionRequest("analyst-1", "UNKNOWN_CODE", "Looks legitimate")
            )
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getMessage()).isEqualTo("Reason code is invalid");
        verify(reviewDecisionPublisher, never()).publish(any(), any());
        verify(reviewActionRepository, never()).save(any());
    }

    private FraudCase openCase() {
        FraudCase fraudCase = new FraudCase();
        fraudCase.setId(7L);
        fraudCase.setTransactionId(11L);
        fraudCase.setTransactionReference("tx-11");
        fraudCase.setAccountId(101L);
        fraudCase.setRiskScore(42);
        fraudCase.setTriggeredRules("RULE_A|RULE_B");
        fraudCase.setExplanations("Explanation A|Explanation B");
        fraudCase.setStatus(FraudCaseStatus.OPEN);
        fraudCase.setCreatedAt(Instant.now());
        return fraudCase;
    }

    private ReasonCode reasonCode(String code) {
        ReasonCode reasonCode = new ReasonCode();
        reasonCode.setCode(code);
        reasonCode.setActive(true);
        return reasonCode;
    }
}
