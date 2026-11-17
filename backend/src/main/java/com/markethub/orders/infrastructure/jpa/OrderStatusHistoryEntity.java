package com.markethub.orders.infrastructure.jpa;

import java.time.Instant;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private OrderEntity order;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "actor_user_id") private UserEntity actor;
    @Column(name = "from_status", length = 32) private String fromStatus;
    @Column(name = "to_status", nullable = false, length = 32) private String toStatus;
    @Column(length = 500) private String reason;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected OrderStatusHistoryEntity() { }
    public OrderStatusHistoryEntity(OrderEntity order, UserEntity actor, String fromStatus, String toStatus, String reason) { this.order = order; this.actor = actor; this.fromStatus = fromStatus; this.toStatus = toStatus; this.reason = reason; }
    @PrePersist void beforeInsert() { createdAt = Instant.now(); }
}
