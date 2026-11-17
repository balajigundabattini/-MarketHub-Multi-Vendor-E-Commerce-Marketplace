package com.markethub.checkout.infrastructure.jpa;

import java.math.BigDecimal;
import java.time.Instant;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.orders.infrastructure.jpa.OrderEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "coupon_redemptions")
public class CouponRedemptionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "coupon_id", nullable = false) private CouponEntity coupon;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id", nullable = false) private UserEntity customer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private OrderEntity order;
    @Column(name = "code_snapshot", nullable = false, length = 40) private String codeSnapshot;
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4) private BigDecimal discountAmount;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected CouponRedemptionEntity() { }
    public CouponRedemptionEntity(CouponEntity coupon, UserEntity customer, OrderEntity order, String code, BigDecimal discountAmount) { this.coupon = coupon; this.customer = customer; this.order = order; this.codeSnapshot = code; this.discountAmount = discountAmount; }
    @PrePersist void beforeInsert() { createdAt = Instant.now(); }
}
