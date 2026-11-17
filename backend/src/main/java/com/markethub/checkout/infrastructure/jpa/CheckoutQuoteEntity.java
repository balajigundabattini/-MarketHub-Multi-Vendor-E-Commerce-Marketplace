package com.markethub.checkout.infrastructure.jpa;

import java.math.BigDecimal;
import java.time.Instant;

import com.markethub.customer.infrastructure.jpa.AddressEntity;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "checkout_quotes")
public class CheckoutQuoteEntity {
    @Id @Column(length = 36) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id", nullable = false) private UserEntity customer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "address_id", nullable = false) private AddressEntity address;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "coupon_id") private CouponEntity coupon;
    @Column(name = "coupon_code", length = 40) private String couponCode;
    @Column(name = "cart_fingerprint", nullable = false, length = 64) private String cartFingerprint;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal subtotal;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal shipping;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal tax;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal discount;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal total;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "consumed_at") private Instant consumedAt;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private Instant createdAt;
    protected CheckoutQuoteEntity() { }
    public CheckoutQuoteEntity(String id, UserEntity customer, AddressEntity address, CouponEntity coupon,
            String couponCode, String fingerprint, BigDecimal subtotal, BigDecimal shipping, BigDecimal tax,
            BigDecimal discount, BigDecimal total, String currency, Instant expiresAt) {
        this.id = id; this.customer = customer; this.address = address; this.coupon = coupon; this.couponCode = couponCode;
        this.cartFingerprint = fingerprint; this.subtotal = subtotal; this.shipping = shipping; this.tax = tax;
        this.discount = discount; this.total = total; this.currency = currency; this.expiresAt = expiresAt;
    }
    public void consume(Instant now) { consumedAt = now; }
    public String getId() { return id; }
    public UserEntity getCustomer() { return customer; }
    public AddressEntity getAddress() { return address; }
    public CouponEntity getCoupon() { return coupon; }
    public String getCouponCode() { return couponCode; }
    public String getCartFingerprint() { return cartFingerprint; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getShipping() { return shipping; }
    public BigDecimal getTax() { return tax; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public String getCurrency() { return currency; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
}
