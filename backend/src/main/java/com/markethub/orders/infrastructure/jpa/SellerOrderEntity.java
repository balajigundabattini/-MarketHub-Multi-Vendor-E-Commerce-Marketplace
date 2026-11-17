package com.markethub.orders.infrastructure.jpa;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.markethub.seller.infrastructure.jpa.SellerEntity;
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
@Table(name = "seller_orders")
public class SellerOrderEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private OrderEntity order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "seller_id", nullable = false) private SellerEntity seller;
    @Column(name = "store_name_snapshot", nullable = false, length = 160) private String storeNameSnapshot;
    @Column(nullable = false, length = 32) private String status;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal subtotal;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal commission = BigDecimal.ZERO;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "sellerOrder") private List<OrderItemEntity> items = new ArrayList<>();
    @OneToMany(mappedBy = "sellerOrder") private List<ShipmentEntity> shipments = new ArrayList<>();
    protected SellerOrderEntity() { }
    public SellerOrderEntity(OrderEntity order, SellerEntity seller, String storeName, String status, BigDecimal subtotal) { this.order = order; this.seller = seller; this.storeNameSnapshot = storeName; this.status = status; this.subtotal = subtotal; }
    public void setStatus(String status) { this.status = status; }
    @PrePersist void beforeInsert() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void beforeUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public OrderEntity getOrder() { return order; }
    public SellerEntity getSeller() { return seller; }
    public String getStoreNameSnapshot() { return storeNameSnapshot; }
    public String getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public List<OrderItemEntity> getItems() { return items; }
    public List<ShipmentEntity> getShipments() { return shipments; }
}
