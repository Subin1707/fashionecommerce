package com.fashion.review.controller;

import com.fashion.review.dto.CreateReviewRequest;
import com.fashion.review.entity.Review;
import com.fashion.review.service.ReviewService;
import com.fashion.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Review>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Review>> getReviewsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Review>> getMyReviews(Authentication authentication) {
        return ResponseEntity.ok(reviewService.getMyReviews(userId(authentication)));
    }

    @PutMapping("/{reviewId}/reply")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Review> reply(@PathVariable Long reviewId, @RequestBody java.util.Map<String, String> request) {
        return ResponseEntity.ok(reviewService.reply(reviewId, request.get("reply")));
    }

    @PostMapping("/product/{productId}")
    public ResponseEntity<Review> createReview(@PathVariable Long productId, @RequestBody CreateReviewRequest request, Authentication authentication) {
        return ResponseEntity.ok(reviewService.createReview(userId(authentication), productId, request));
    }

    @PutMapping("/{reviewId}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Review> updateReviewStatus(@PathVariable Long reviewId, @RequestParam String status) {
        return ResponseEntity.ok(reviewService.updateReviewStatus(reviewId, status));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    private Long userId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getUserId();
    }
}
