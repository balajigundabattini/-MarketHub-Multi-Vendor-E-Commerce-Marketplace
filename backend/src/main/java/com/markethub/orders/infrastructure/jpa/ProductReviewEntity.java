package com.markethub.orders.infrastructure.jpa;

import java.time.Instant;
import com.markethub.catalog.infrastructure.jpa.ProductEntity;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "product_reviews")
public class ProductReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false, unique = true)
    private OrderItemEntity item;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserEntity customer;
    @Column(nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private int rating;
    @Column(nullable = false, length = 120)
    private String title;
    @Column(nullable = false, length = 2000)
    private String body;
    @Column(nullable = false, length = 24)
    private String status = "PENDING";
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by")
    private UserEntity moderator;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    protected ProductReviewEntity() {
    }

    public ProductReviewEntity(OrderItemEntity item, UserEntity customer, int rating, String title, String body) {
        this.item = item;
        this.product = item.getProduct();
        this.customer = customer;
        this.rating = rating;
        this.title = title;
        this.body = body;
    }

    @PrePersist
    void beforeInsert() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public int getRating() {
        return rating;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getReviewer() {
        return customer.getDisplayName();
    }

    public Long getProductId() {
        return product.getId();
    }

    public Long getCustomerId() {
        return customer.getId();
    }

    public Long getModeratorId() {
        return moderator != null ? moderator.getId() : null;
    }

    public Instant getModeratedAt() {
        return moderatedAt;
    }

    public void updateReview(int rating, String title, String body) {
        this.rating = rating;
        this.title = title;
        this.body = body;
        this.status = "PENDING";
        this.moderator = null;
        this.moderatedAt = null;
    }

    public void moderate(String status, UserEntity moderator) {
        this.status = status;
        this.moderator = moderator;
        this.moderatedAt = Instant.now();
    }

    public void deleteReview() {
        this.status = "DELETED";
    }
}
