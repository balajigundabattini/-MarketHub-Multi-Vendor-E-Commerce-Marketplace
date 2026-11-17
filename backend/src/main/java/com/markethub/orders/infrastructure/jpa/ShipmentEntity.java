package com.markethub.orders.infrastructure.jpa;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "shipments")
public class ShipmentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "seller_order_id", nullable = false) private SellerOrderEntity sellerOrder;
    @Column(nullable = false, length = 100) private String carrier;
    @Column(name = "tracking_number", nullable = false, length = 160) private String trackingNumber;
    @Column(nullable = false, length = 24) private String status = "SHIPPED";
    @Column(name = "shipped_at", nullable = false, insertable = false, updatable = false) private Instant shippedAt;
    @Column(name = "delivered_at") private Instant deliveredAt;
    protected ShipmentEntity() { }
    public ShipmentEntity(SellerOrderEntity order, String carrier, String trackingNumber) { this.sellerOrder = order; this.carrier = carrier; this.trackingNumber = trackingNumber; }
    public Long getId() { return id; }
    public String getCarrier() { return carrier; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getStatus() { return status; }
    public Instant getShippedAt() { return shippedAt; }
    public void markDelivered() { this.status = "DELIVERED"; this.deliveredAt = Instant.now(); }
}
