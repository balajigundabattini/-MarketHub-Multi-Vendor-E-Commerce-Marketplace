package com.markethub.catalog.application;

import java.math.BigDecimal;
import java.util.List;

import com.markethub.catalog.api.CategoryResponse;
import com.markethub.catalog.api.ProductResponse;
import com.markethub.catalog.domain.ProductStatus;
import com.markethub.catalog.infrastructure.jpa.CategoryEntity;
import com.markethub.catalog.infrastructure.jpa.CategoryRepository;
import com.markethub.catalog.infrastructure.jpa.InventoryRepository;
import com.markethub.catalog.infrastructure.jpa.ProductEntity;
import com.markethub.catalog.infrastructure.jpa.ProductRepository;
import com.markethub.seller.domain.SellerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogService {
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final InventoryRepository inventory;

    public CatalogService(CategoryRepository categories, ProductRepository products, InventoryRepository inventory) {
        this.categories = categories;
        this.products = products;
        this.inventory = inventory;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> activeCategories() {
        return categories.findAllByActiveTrueOrderByNameAsc().stream().map(this::categoryResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String search, String categorySlug, BigDecimal minPrice,
            BigDecimal maxPrice, String sort, int page, int size) {
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0
                || minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price range is invalid");
        }
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page or page size is invalid");
        Sort order = switch (sort == null ? "newest" : sort) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort option is not supported");
        };
        Pageable pageable = PageRequest.of(page, size, order);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        String normalizedCategory = categorySlug == null || categorySlug.isBlank() ? null : categorySlug.trim();
        return products.searchCatalog(ProductStatus.ACTIVE, "ACTIVE", SellerStatus.APPROVED,
                normalizedSearch, normalizedCategory, minPrice, maxPrice, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        ProductEntity product = products.findByIdAndStatusAndStore_StatusAndStore_Seller_Status(
                        id, ProductStatus.ACTIVE, "ACTIVE", SellerStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product was not found"));
        return response(product);
    }

    private CategoryResponse categoryResponse(CategoryEntity category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(), category.getDescription(), category.isActive());
    }

    private ProductResponse response(ProductEntity product) {
        int available = inventory.findById(product.getId()).map(row -> row.getAvailable()).orElse(0);
        return CatalogMapper.toResponse(product, available);
    }
}
