package com.markethub.catalog.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(Long id, String name, String sku, String slug, String brand,
        String description, BigDecimal price, String currency, Long categoryId, String categoryName,
        String storeName, String sellerName, String status, String imageUrl, String imageAlt,
        List<ProductImageResponse> images, List<SpecificationResponse> specifications,
        Integer availableQuantity, Instant createdAt) {
    public record SpecificationResponse(String name, String value) { }
}
