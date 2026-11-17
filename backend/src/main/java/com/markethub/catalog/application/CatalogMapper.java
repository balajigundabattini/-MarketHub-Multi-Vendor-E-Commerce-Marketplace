package com.markethub.catalog.application;

import java.util.Comparator;
import java.util.List;

import com.markethub.catalog.api.ProductImageResponse;
import com.markethub.catalog.api.ProductResponse;
import com.markethub.catalog.infrastructure.jpa.ProductEntity;

public final class CatalogMapper {
    private CatalogMapper() { }
    public static ProductResponse toResponse(ProductEntity product, int available) {
        List<ProductImageResponse> images = product.getImages().stream()
                .sorted(Comparator.comparingInt(image -> image.getSortOrder()))
                .map(image -> new ProductImageResponse(image.getStorageKey(), image.getAltText())).toList();
        return new ProductResponse(product.getId(), product.getName(), product.getSku(), product.getSlug(),
                product.getBrand(), product.getDescription(), product.getPrice(), product.getCurrency(),
                product.getCategory().getId(), product.getCategory().getName(), product.getStore().getName(),
                product.getStore().getSeller().getBusinessName(), product.getStatus().name(),
                images.isEmpty() ? null : images.get(0).url(), images.isEmpty() ? null : images.get(0).altText(),
                images, List.of(), available, product.getCreatedAt());
    }
}
