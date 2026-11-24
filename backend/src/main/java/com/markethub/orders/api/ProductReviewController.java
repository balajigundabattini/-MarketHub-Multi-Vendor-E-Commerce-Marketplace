package com.markethub.orders.api;

import com.markethub.orders.application.ProductReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
public class ProductReviewController {
    private final ProductReviewService reviews;

    public ProductReviewController(ProductReviewService reviews) {
        this.reviews = reviews;
    }

    @PostMapping
    public ProductReviewService.ReviewView submit(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId,
            @Valid @RequestBody ProductReviewService.ReviewRequest request) {
        return reviews.submit(Long.valueOf(jwt.getSubject()), productId, request);
    }

    @PutMapping("/{reviewId}")
    public ProductReviewService.ReviewView update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @Valid @RequestBody ProductReviewService.ReviewUpdateRequest request) {

        return reviews.update(
                Long.valueOf(jwt.getSubject()),
                productId,
                reviewId,
                request);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long productId,
            @PathVariable Long reviewId) {

        reviews.delete(
                Long.valueOf(jwt.getSubject()),
                productId,
                reviewId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public Page<ProductReviewService.ReviewView> getPublicReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return reviews.publicReviews(productId, page, size);
    }
}
