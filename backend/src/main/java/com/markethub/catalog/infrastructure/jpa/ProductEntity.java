package com.markethub.catalog.infrastructure.jpa;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.markethub.catalog.domain.ProductStatus;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.seller.infrastructure.jpa.StoreEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;
    @Column(nullable = false, length = 80)
    private String sku;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false, length = 220)
    private String slug;
    @Column(length = 120)
    private String brand;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;
    @Column(nullable = false, length = 3)
    private String currency = "INR";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductStatus status = ProductStatus.PENDING_APPROVAL;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserEntity reviewer;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImageEntity> images = new ArrayList<>();

    protected ProductEntity() {
    }

    public ProductEntity(StoreEntity store, CategoryEntity category, String sku, String name, String brand,
            String description, BigDecimal price, String currency) {
        this.store = store;
        this.category = category;
        this.sku = sku;
        this.name = name;
        this.brand = brand;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.status = ProductStatus.PENDING_APPROVAL;
    }

    @PrePersist
    void beforeInsert() {
        createdAt = Instant.now();
        if (slug == null)
            slug = "product-" + UUID.randomUUID();
    }

    public void update(CategoryEntity category, String sku, String name, String brand, String description,
            BigDecimal price, String currency) {
        this.category = category;
        this.sku = sku;
        this.name = name;
        this.brand = brand;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.status = ProductStatus.PENDING_APPROVAL;
    }

    public void replaceImages(List<String> urls) {
        images.clear();
        for (int i = 0; i < urls.size(); i++)
            images.add(new ProductImageEntity(this, urls.get(i), name, i));
    }

    public void deactivate() {
        status = ProductStatus.INACTIVE;
    }

    public void moderate(ProductStatus status, UserEntity reviewer) {
        this.status = status;
        this.reviewer = reviewer;
        this.reviewedAt = Instant.now();
    }

    public String getSlug() {
        return slug;
    }

    public Long getId() {
        return id;
    }

    public StoreEntity getStore() {
        return store;
    }

    public CategoryEntity getCategory() {
        return category;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ProductImageEntity> getImages() {
        return images;
    }
}
