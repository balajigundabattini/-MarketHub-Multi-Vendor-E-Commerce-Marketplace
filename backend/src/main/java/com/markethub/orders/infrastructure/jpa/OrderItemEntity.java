package com.markethub.orders.infrastructure.jpa;

import java.math.BigDecimal;
import java.time.Instant;

import com.markethub.catalog.infrastructure.jpa.ProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "seller_order_id", nullable = false) private SellerOrderEntity sellerOrder;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false) private ProductEntity product;
    @Column(name = "sku_snapshot", nullable = false, length = 80) private String skuSnapshot;
    @Column(name = "name_snapshot", nullable = false, length = 200) private String nameSnapshot;
    @Column(nullable = false) private int quantity;
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4) private BigDecimal unitPrice;
    @Column(name = "line_total", nullable = false, precision = 19, scale = 4) private BigDecimal lineTotal;
    @Column(nullable = false, length = 32) private String status = "ACTIVE";
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected OrderItemEntity() { }
    public OrderItemEntity(SellerOrderEntity sellerOrder, ProductEntity product, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        this.sellerOrder = sellerOrder; this.product = product; this.skuSnapshot = product.getSku(); this.nameSnapshot = product.getName();
        this.quantity = quantity; this.unitPrice = unitPrice; this.lineTotal = lineTotal;
    }
    public void setStatus(String status) { this.status = status; }
    @PrePersist void beforeInsert() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public SellerOrderEntity getSellerOrder() { return sellerOrder; }
    public ProductEntity getProduct() { return product; }
    public String getSkuSnapshot() { return skuSnapshot; }
    public String getNameSnapshot() { return nameSnapshot; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public String getStatus() { return status; }
}
