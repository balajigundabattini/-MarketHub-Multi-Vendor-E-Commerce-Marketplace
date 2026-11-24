
package com.markethub.catalog.application;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import com.markethub.catalog.api.ProductImageResponse;
import com.markethub.catalog.api.ProductResponse;
import com.markethub.catalog.infrastructure.jpa.ProductEntity;

public final class CatalogMapper {

        private CatalogMapper() {
        }

        // Backward-compatible method for existing seller and admin services
        public static ProductResponse toResponse(ProductEntity product, int available) {
                return toResponse(product, available, null, 0L);
        }

        // Method used by the public catalog with actual rating statistics
        public static ProductResponse toResponse(
                        ProductEntity product,
                        int available,
                        BigDecimal averageRating,
                        Long reviewCount) {

                List<ProductImageResponse> images = product.getImages().stream()
                                .sorted(Comparator.comparingInt(image -> image.getSortOrder()))
                                .map(image -> new ProductImageResponse(
                                                image.getStorageKey(),
                                                image.getAltText()))
                                .toList();

                return new ProductResponse(
                                product.getId(),
                                product.getName(),
                                product.getSku(),
                                product.getSlug(),
                                product.getBrand(),
                                product.getDescription(),
                                product.getPrice(),
                                product.getCurrency(),
                                product.getCategory().getId(),
                                product.getCategory().getName(),
                                product.getStore().getName(),
                                product.getStore().getSeller().getBusinessName(),
                                product.getStatus().name(),
                                images.isEmpty() ? null : images.get(0).url(),
                                images.isEmpty() ? null : images.get(0).altText(),
                                images,
                                List.of(),
                                available,
                                product.getCreatedAt(),
                                averageRating,
                                reviewCount
                );
        }
}
