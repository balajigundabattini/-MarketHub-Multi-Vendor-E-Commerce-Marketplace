package com.markethub.orders.infrastructure.jpa;
import java.time.Instant;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.payments.infrastructure.jpa.PaymentEntity;
import jakarta.persistence.*;
@Entity @Table(name="notifications") public class NotificationEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private UserEntity user;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id") private OrderEntity order;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="payment_id") private PaymentEntity payment;
 @Column(nullable=false,length=160) private String title; @Column(nullable=false,length=500) private String message;
 @Column(name="is_read",nullable=false) private boolean read; @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 protected NotificationEntity(){} public NotificationEntity(UserEntity user,String title,String message,OrderEntity order,PaymentEntity payment){this.user=user;this.title=title;this.message=message;this.order=order;this.payment=payment;}
 @PrePersist void beforeInsert(){createdAt=Instant.now();} public Long getId(){return id;} public String getTitle(){return title;} public String getMessage(){return message;} public boolean isRead(){return read;} public Instant getCreatedAt(){return createdAt;} public OrderEntity getOrder(){return order;} public PaymentEntity getPayment(){return payment;} public void markRead(){read=true;}
}
