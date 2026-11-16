package com.markethub.seller.infrastructure.jpa;

import java.time.Instant;

import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.seller.domain.SellerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "sellers")
public class SellerEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "legal_name", nullable = false, length = 160)
    private String legalName;

    @Column(name = "business_name", nullable = false, length = 160)
    private String businessName;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32)
    private SellerStatus status = SellerStatus.PENDING_REVIEW;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by")
    private UserEntity reviewer;

    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(name = "review_note", length = 500) private String reviewNote;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected SellerEntity() { }

    public SellerEntity(UserEntity user, String legalName, String businessName) {
        this.user = user;
        this.legalName = legalName;
        this.businessName = businessName;
    }

    @PrePersist void beforeInsert() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void beforeUpdate() { updatedAt = Instant.now(); }

    public void review(SellerStatus status, UserEntity reviewer, String note) {
        this.status = status;
        this.reviewer = reviewer;
        this.reviewedAt = Instant.now();
        this.reviewNote = note;
    }

    public Long getId() { return id; }
    public UserEntity getUser() { return user; }
    public String getLegalName() { return legalName; }
    public String getBusinessName() { return businessName; }
    public SellerStatus getStatus() { return status; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewNote() { return reviewNote; }
    public Instant getCreatedAt() { return createdAt; }
}
