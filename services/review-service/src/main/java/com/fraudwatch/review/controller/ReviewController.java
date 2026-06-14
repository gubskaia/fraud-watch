package com.fraudwatch.review.controller;

import com.fraudwatch.review.dto.AnalystCommentRequest;
import com.fraudwatch.review.dto.ReviewCaseResponse;
import com.fraudwatch.review.dto.ReviewDecisionRequest;
import com.fraudwatch.review.service.ReviewCaseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews/cases")
public class ReviewController {

    private final ReviewCaseService reviewCaseService;

    public ReviewController(ReviewCaseService reviewCaseService) {
        this.reviewCaseService = reviewCaseService;
    }

    @GetMapping
    public List<ReviewCaseResponse> getCases() {
        return reviewCaseService.getCases();
    }

    @GetMapping("/{id}")
    public ReviewCaseResponse getCase(@PathVariable Long id) {
        return reviewCaseService.getCase(id);
    }

    @PostMapping("/{id}/approve")
    public ReviewCaseResponse approveCase(
        @PathVariable Long id,
        @Valid @RequestBody ReviewDecisionRequest request
    ) {
        return reviewCaseService.approveCase(id, request);
    }

    @PostMapping("/{id}/block")
    public ReviewCaseResponse blockCase(
        @PathVariable Long id,
        @Valid @RequestBody ReviewDecisionRequest request
    ) {
        return reviewCaseService.blockCase(id, request);
    }

    @PostMapping("/{id}/comment")
    public ReviewCaseResponse addComment(
        @PathVariable Long id,
        @Valid @RequestBody AnalystCommentRequest request
    ) {
        return reviewCaseService.addComment(id, request);
    }
}
