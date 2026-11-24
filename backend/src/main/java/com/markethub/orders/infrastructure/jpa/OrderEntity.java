package com.markethub.orders.infrastructure.jpa;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.customer.infrastructure.jpa.AddressEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserEntity customer;
    @Column(name = "public_order_number", nullable = false, unique = true, length = 40)
    private String publicOrderNumber;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "payment_status", nullable = false, length = 24)
    private String paymentStatus;
    @Column(name = "payment_method", nullable = false, length = 24)
    private String paymentMethod;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal shipping;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal tax;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal discount;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal total;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "shipping_recipient_name", nullable = false, length = 120)
    private String shippingRecipientName;
    @Column(name = "shipping_phone", nullable = false, length = 32)
    private String shippingPhone;
    @Column(name = "shipping_line1", nullable = false, length = 200)
    private String shippingLine1;
    @Column(name = "shipping_line2", length = 200)
    private String shippingLine2;
    @Column(name = "shipping_city", nullable = false, length = 100)
    private String shippingCity;
    @Column(name = "shipping_region", nullable = false, length = 100)
    private String shippingRegion;
    @Column(name = "shipping_postal_code", nullable = false, length = 24)
    private String shippingPostalCode;
    @Column(name = "shipping_country_code", nullable = false, length = 2)
    private String shippingCountryCode;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @OneToMany(mappedBy = "order")
    private List<SellerOrderEntity> sellerOrders = new ArrayList<>();

    protected OrderEntity() {
    }

    public OrderEntity(UserEntity customer, String number, String status, String paymentMethod,
            BigDecimal subtotal, BigDecimal shipping, BigDecimal tax, BigDecimal discount, BigDecimal total,
            String currency, AddressEntity address) {
        this.customer = customer;
        this.publicOrderNumber = number;
        this.status = status;
        this.paymentStatus = "PENDING";
        this.paymentMethod = paymentMethod;
        this.subtotal = subtotal;
        this.shipping = shipping;
        this.tax = tax;
        this.discount = discount;
        this.total = total;
        this.currency = currency;
        this.shippingRecipientName = address.getRecipientName();
        this.shippingPhone = address.getPhone();
        this.shippingLine1 = address.getLine1();
        this.shippingLine2 = address.getLine2();
        this.shippingCity = address.getCity();
        this.shippingRegion = address.getRegion();
        this.shippingPostalCode = address.getPostalCode();
        this.shippingCountryCode = address.getCountryCode();
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void markPaid() {
        this.paymentStatus = "PAID";
        this.status = "CONFIRMED";
    }

    public void markRefunded() {
        this.paymentStatus = "REFUNDED";
    }

    public void markPaidNeedsReview() {
        this.paymentStatus = "PAID";
        this.status = "PAYMENT_REVIEW_REQUIRED";
    }

    @PrePersist
    void beforeInsert() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserEntity getCustomer() {
        return customer;
    }

    public String getPublicOrderNumber() {
        return publicOrderNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getShipping() {
        return shipping;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getCurrency() {
        return currency;
    }

    public String getShippingRecipientName() {
        return shippingRecipientName;
    }

    public String getShippingPhone() {
        return shippingPhone;
    }

    public String getShippingLine1() {
        return shippingLine1;
    }

    public String getShippingLine2() {
        return shippingLine2;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public String getShippingRegion() {
        return shippingRegion;
    }

    public String getShippingPostalCode() {
        return shippingPostalCode;
    }

    public String getShippingCountryCode() {
        return shippingCountryCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<SellerOrderEntity> getSellerOrders() {
        return sellerOrders;
    }
}
