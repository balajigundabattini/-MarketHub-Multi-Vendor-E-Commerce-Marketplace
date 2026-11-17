package com.markethub.checkout.infrastructure.jpa;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "coupons")
public class CouponEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 40) private String code;
    @Column(name = "discount_type", nullable = false, length = 16) private String discountType;
    @Column(name = "discount_value", nullable = false, precision = 19, scale = 4) private BigDecimal discountValue;
    @Column(name = "minimum_order_amount", nullable = false, precision = 19, scale = 4) private BigDecimal minimumOrderAmount;
    @Column(name = "max_discount_amount", precision = 19, scale = 4) private BigDecimal maxDiscountAmount;
    @Column(name = "starts_at") private Instant startsAt;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(name = "usage_limit") private Integer usageLimit;
    @Column(name = "usage_count", nullable = false) private int usageCount;
    @Column(name = "is_active", nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected CouponEntity() { }
    public CouponEntity(String code, String discountType, BigDecimal discountValue, BigDecimal minimumOrderAmount,
            BigDecimal maxDiscountAmount, Instant startsAt, Instant expiresAt, Integer usageLimit, boolean active) {
        update(code, discountType, discountValue, minimumOrderAmount, maxDiscountAmount, startsAt, expiresAt, usageLimit, active);
    }
    public void update(String code, String discountType, BigDecimal discountValue, BigDecimal minimumOrderAmount,
            BigDecimal maxDiscountAmount, Instant startsAt, Instant expiresAt, Integer usageLimit, boolean active) {
        this.code = code; this.discountType = discountType; this.discountValue = discountValue;
        this.minimumOrderAmount = minimumOrderAmount; this.maxDiscountAmount = maxDiscountAmount;
        this.startsAt = startsAt; this.expiresAt = expiresAt; this.usageLimit = usageLimit; this.active = active;
    }
    public void incrementUsage() { usageCount++; }
    @PrePersist void beforeInsert() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void beforeUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getDiscountType() { return discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public BigDecimal getMinimumOrderAmount() { return minimumOrderAmount; }
    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Integer getUsageLimit() { return usageLimit; }
    public int getUsageCount() { return usageCount; }
    public boolean isActive() { return active; }
}
