package com.markethub.payments.infrastructure.jpa;
import java.math.BigDecimal;
import java.time.Instant;
import com.markethub.orders.infrastructure.jpa.OrderEntity;
import jakarta.persistence.*;
@Entity @Table(name="payments")
public class PaymentEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="order_id", nullable=false, unique=true) private OrderEntity order;
    @Column(nullable=false, length=24) private String provider;
    @Column(name="provider_order_id", length=100, unique=true) private String providerOrderId;
    @Column(name="provider_payment_id", length=100) private String providerPaymentId;
    @Column(nullable=false, length=24) private String status;
    @Column(name="attempt_count", nullable=false) private int attemptCount;
    @Column(nullable=false, precision=19, scale=4) private BigDecimal amount;
    @Column(nullable=false, length=3) private String currency;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false) private Instant updatedAt;
    protected PaymentEntity() { }
    public PaymentEntity(OrderEntity order, String provider, String providerOrderId) { this.order=order; this.provider=provider; this.providerOrderId=providerOrderId; this.status="PENDING"; this.attemptCount=0; this.amount=order.getTotal(); this.currency=order.getCurrency(); }
    public void retry(String providerOrderId) { this.providerOrderId=providerOrderId; this.providerPaymentId=null; this.status="PENDING"; this.attemptCount++; }
    public void capture(String paymentId) { this.providerPaymentId=paymentId; this.status="CAPTURED"; }
    public void fail(String paymentId) { if ("CAPTURED".equals(status)) return; this.providerPaymentId=paymentId; this.status="FAILED"; }
    public void expire() { if ("PENDING".equals(status) || "FAILED".equals(status)) this.status="EXPIRED"; }
    public Long getId(){return id;} public OrderEntity getOrder(){return order;} public String getProvider(){return provider;} public String getProviderOrderId(){return providerOrderId;} public String getProviderPaymentId(){return providerPaymentId;} public String getStatus(){return status;} public int getAttemptCount(){return attemptCount;} public BigDecimal getAmount(){return amount;} public String getCurrency(){return currency;}
}
