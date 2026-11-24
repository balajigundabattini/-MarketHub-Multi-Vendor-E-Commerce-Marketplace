package com.markethub.orders.application;

import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.orders.infrastructure.jpa.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductReviewService {
    private final ProductReviewRepository reviews;
    private final OrderItemRepository items;
    private final UserRepository users;
    private final NotificationService notifications;

    public ProductReviewService(ProductReviewRepository reviews, OrderItemRepository items, UserRepository users,
            NotificationService notifications) {
        this.reviews = reviews;
        this.items = items;
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional
    public ReviewView submit(Long customerId, Long productId, ReviewRequest request) {
        var item = items.findOwnedProductItemForUpdate(request.orderItemId(), productId, customerId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Eligible delivered purchase was not found"));
        if (!"DELIVERED".equals(item.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reviews can be submitted after delivery");
        if (reviews.existsByItem_Id(item.getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A review was already submitted for this purchase");
        var customer = users.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer was not found"));
        var saved = reviews.save(new ProductReviewEntity(item, customer, request.rating(), request.title().trim(),
                request.body().trim()));
        return ReviewView.from(saved);
    }

    @Transactional
    public ReviewView update(
            Long customerId,
            Long productId,
            Long reviewId,
            ReviewUpdateRequest request) {
        var review = reviews.findByIdAndProduct_IdAndCustomer_Id(
                reviewId,
                productId,
                customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Review was not found"));

        review.updateReview(
                request.rating(),
                request.title().trim(),
                request.body().trim());

        return ReviewView.from(review);
    }

    @Transactional
    public void delete(Long customerId, Long productId, Long reviewId) {
        var review = reviews.findByIdAndProduct_IdAndCustomer_Id(
                reviewId,
                productId,
                customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Review was not found"));

        review.deleteReview();
    }

    @Transactional(readOnly = true)
    public Page<ReviewView> publicReviews(Long productId, int page, int size) {
        validate(page, size);
        return reviews.findAllByProduct_IdAndStatus(productId, "APPROVED",
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))).map(ReviewView::from);
    }

    @Transactional(readOnly = true)
    public Page<ReviewView> pending(int page, int size) {
        validate(page, size);
        return reviews.findAllByStatus("PENDING", PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")))
                .map(ReviewView::from);
    }

    @Transactional
    public ReviewView moderate(Long adminId, Long id, String status) {
        if (!java.util.List.of("APPROVED", "REJECTED").contains(status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review status is invalid");
        var row = reviews.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review was not found"));
        if (!"PENDING".equals(row.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review has already been moderated");
        var admin = users.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Admin user was not found"));

        row.moderate(status, admin);
        notifications.create(row.getCustomerId(), "Review moderated",
                "Your review for product " + row.getProductId() + " was " + status.toLowerCase() + ".");
        return ReviewView.from(row);
    }

    private void validate(int p, int s) {
        if (p < 0 || s < 1 || s > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page or page size is invalid");
    }

    public record ReviewRequest(@jakarta.validation.constraints.NotNull Long orderItemId,
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(5) int rating,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 120) String title,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 2000) String body) {
    }

    public record ReviewUpdateRequest(
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(5) int rating,

            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 120) String title,

            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 2000) String body) {
    }

    public record ReviewView(Long id, Long productId, String reviewer, int rating, String title, String body,
            String status, java.time.Instant createdAt) {
        static ReviewView from(ProductReviewEntity e) {
            return new ReviewView(e.getId(), e.getProductId(), e.getReviewer(), e.getRating(), e.getTitle(),
                    e.getBody(), e.getStatus(), e.getCreatedAt());
        }
    }
}
