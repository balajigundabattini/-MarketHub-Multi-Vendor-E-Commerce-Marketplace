package com.markethub.catalog.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_images")
public class ProductImageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false) private ProductEntity product;
    @Column(name = "storage_key", nullable = false, length = 500) private String storageKey;
    @Column(name = "alt_text", length = 240) private String altText;
    @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED") private short sortOrder;
    protected ProductImageEntity() { }
    public ProductImageEntity(ProductEntity product, String storageKey, String altText, int sortOrder) { this.product = product; this.storageKey = storageKey; this.altText = altText; this.sortOrder = (short) sortOrder; }
    public String getStorageKey() { return storageKey; }
    public String getAltText() { return altText; }
    public short getSortOrder() { return sortOrder; }
}
