package com.markethub.orders.api;

import com.markethub.orders.application.ProductReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/v1/admin/reviews")
public class AdminReviewController {
    private final ProductReviewService reviews;

    public AdminReviewController(ProductReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping
    public Page<ProductReviewService.ReviewView> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reviews.pending(page, size);
    }

    @PatchMapping("/{id}")
    public ProductReviewService.ReviewView moderate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody Decision request) {

        Long adminId = Long.valueOf(jwt.getSubject());

        return reviews.moderate(adminId, id, request.status());
    }

    public record Decision(@jakarta.validation.constraints.Pattern(regexp = "APPROVED|REJECTED") String status) {
    }
}
