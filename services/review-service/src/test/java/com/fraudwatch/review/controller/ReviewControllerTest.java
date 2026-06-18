package com.fraudwatch.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudwatch.review.config.SecurityConfig;
import com.fraudwatch.review.dto.AnalystCommentRequest;
import com.fraudwatch.review.dto.AssignCaseRequest;
import com.fraudwatch.review.dto.ReviewActionResponse;
import com.fraudwatch.review.dto.ReviewCaseResponse;
import com.fraudwatch.review.dto.ReviewCommentResponse;
import com.fraudwatch.review.dto.ReviewDecisionRequest;
import com.fraudwatch.review.exception.ReviewBusinessException;
import com.fraudwatch.review.exception.ReviewExceptionHandler;
import com.fraudwatch.review.service.ReviewCaseService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReviewController.class)
@Import({SecurityConfig.class, ReviewExceptionHandler.class})
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewCaseService reviewCaseService;

    @Test
    void shouldReturnCaseById() throws Exception {
        when(reviewCaseService.getCase(7L)).thenReturn(reviewCaseResponse("OPEN", null, null));

        mockMvc.perform(get("/api/reviews/cases/7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.transactionId").value(11))
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.triggeredRules[0]").value("RULE_A"))
            .andExpect(jsonPath("$.actions[0].actionType").value("ASSIGNED"));
    }

    @Test
    void shouldApproveCase() throws Exception {
        when(reviewCaseService.approveCase(eq(7L), any(ReviewDecisionRequest.class)))
            .thenReturn(reviewCaseResponse("APPROVED", "analyst-1", "LEGIT_ACTIVITY"));

        mockMvc.perform(post("/api/reviews/cases/7/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new ReviewDecisionRequest("analyst-1", "LEGIT_ACTIVITY", "Looks legitimate")
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.assignedTo").value("analyst-1"))
            .andExpect(jsonPath("$.reasonCode").value("LEGIT_ACTIVITY"));
    }

    @Test
    void shouldRejectInvalidAssignRequest() throws Exception {
        mockMvc.perform(post("/api/reviews/cases/7/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AssignCaseRequest(" ", "notes"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.details.fields.analyst").exists());
    }

    @Test
    void shouldRejectInvalidCommentRequest() throws Exception {
        mockMvc.perform(post("/api/reviews/cases/7/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AnalystCommentRequest("analyst-2", " "))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.details.fields.comment").exists());
    }

    @Test
    void shouldTranslateNotFoundBusinessError() throws Exception {
        when(reviewCaseService.getCase(999L))
            .thenThrow(new ReviewBusinessException(HttpStatus.NOT_FOUND, "Review case was not found"));

        mockMvc.perform(get("/api/reviews/cases/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Review case was not found"))
            .andExpect(jsonPath("$.path").value("/api/reviews/cases/999"));
    }

    @Test
    void shouldTranslateConflictBusinessError() throws Exception {
        when(reviewCaseService.blockCase(eq(7L), any(ReviewDecisionRequest.class)))
            .thenThrow(new ReviewBusinessException(HttpStatus.CONFLICT, "Review case is already finalized"));

        mockMvc.perform(post("/api/reviews/cases/7/block")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new ReviewDecisionRequest("analyst-3", "CONFIRMED_FRAUD", "Confirmed takeover")
                )))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Review case is already finalized"));
    }

    private ReviewCaseResponse reviewCaseResponse(String status, String assignedTo, String reasonCode) {
        return new ReviewCaseResponse(
            7L,
            11L,
            "tx-11",
            101L,
            42,
            List.of("RULE_A", "RULE_B"),
            List.of("Explanation A", "Explanation B"),
            status,
            assignedTo,
            reasonCode,
            Instant.parse("2026-06-18T00:00:00Z"),
            List.of(new ReviewCommentResponse(21L, "analyst-2", "Need one more check", Instant.parse("2026-06-18T00:01:00Z"))),
            List.of(new ReviewActionResponse(31L, "ASSIGNED", "analyst-1", null, "Picked from queue", Instant.parse("2026-06-18T00:02:00Z"))),
            Instant.parse("2026-06-18T00:00:00Z")
        );
    }
}
