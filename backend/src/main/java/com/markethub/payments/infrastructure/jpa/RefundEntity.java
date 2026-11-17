package com.markethub.payments.infrastructure.jpa;
import java.math.BigDecimal;
import java.time.Instant;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.orders.infrastructure.jpa.OrderEntity;
import jakarta.persistence.*;
@Entity @Table(name="refunds")
public class RefundEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="payment_id",nullable=false,unique=true) private PaymentEntity payment;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id",nullable=false) private OrderEntity order;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="requested_by",nullable=false) private UserEntity requestedBy;
    @Column(name="provider_refund_id",length=100,unique=true) private String providerRefundId;
    @Column(nullable=false,length=500) private String reason;
    @Column(nullable=false,length=24) private String status;
    @Column(name="order_status_before",nullable=false,length=32) private String orderStatusBefore;
    @Column(nullable=false,precision=19,scale=4) private BigDecimal amount;
    @Column(nullable=false,length=3) private String currency;
    @Column(name="created_at",nullable=false,insertable=false,updatable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false,insertable=false,updatable=false) private Instant updatedAt;
    protected RefundEntity() { }
    public RefundEntity(PaymentEntity payment,UserEntity admin,String reason,String orderStatus) { this.payment=payment;this.order=payment.getOrder();this.requestedBy=admin;this.reason=reason;this.status="REQUESTED";this.orderStatusBefore=orderStatus;this.amount=payment.getAmount();this.currency=payment.getCurrency(); }
    public void submitted(String providerId,String providerStatus) { this.providerRefundId=providerId;this.status="processed".equalsIgnoreCase(providerStatus)?"PROCESSED":"failed".equalsIgnoreCase(providerStatus)?"FAILED":"PROCESSING"; }
    public void processed(){this.status="PROCESSED";} public void failed(){this.status="FAILED";}
    public Long getId(){return id;} public PaymentEntity getPayment(){return payment;} public OrderEntity getOrder(){return order;} public String getProviderRefundId(){return providerRefundId;} public String getStatus(){return status;} public String getOrderStatusBefore(){return orderStatusBefore;} public BigDecimal getAmount(){return amount;} public String getCurrency(){return currency;} public Instant getCreatedAt(){return createdAt;}
}
