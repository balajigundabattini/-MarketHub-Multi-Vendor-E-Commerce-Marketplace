package com.markethub.catalog.application;

import com.markethub.catalog.api.CategoryRequest;
import com.markethub.catalog.api.CategoryResponse;
import com.markethub.catalog.api.ProductResponse;
import com.markethub.catalog.domain.ProductStatus;
import com.markethub.catalog.infrastructure.jpa.CategoryEntity;
import com.markethub.catalog.infrastructure.jpa.CategoryRepository;
import com.markethub.catalog.infrastructure.jpa.InventoryRepository;
import com.markethub.catalog.infrastructure.jpa.ProductEntity;
import com.markethub.catalog.infrastructure.jpa.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminCatalogService {
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final InventoryRepository inventory;

    public AdminCatalogService(CategoryRepository categories, ProductRepository products, InventoryRepository inventory) {
        this.categories = categories; this.products = products; this.inventory = inventory;
    }

    @Transactional(readOnly = true)
    public java.util.List<CategoryResponse> categories() {
        return categories.findAll(org.springframework.data.domain.Sort.by("name")).stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.isActive())).toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        CategoryEntity category = categories.save(new CategoryEntity(request.name().trim(), request.slug().trim(), clean(request.description())));
        return categoryResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        CategoryEntity category = categories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category was not found"));
        category.update(request.name().trim(), request.slug().trim(), clean(request.description()));
        return categoryResponse(category);
    }

    @Transactional
    public CategoryResponse setCategoryActive(Long id, boolean active) {
        CategoryEntity category = categories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category was not found"));
        category.setActive(active);
        return categoryResponse(category);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> products(ProductStatus status, Pageable pageable) {
        Page<ProductEntity> page = status == null ? products.findAll(pageable) : products.findAllByStatus(status, pageable);
        return page.map(this::productResponse);
    }

    @Transactional
    public ProductResponse moderate(Long id, ProductStatus decision) {
        if (decision != ProductStatus.ACTIVE && decision != ProductStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Moderation decision must be ACTIVE or REJECTED");
        }
        ProductEntity product = products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product was not found"));
        if (product.getStatus() != ProductStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product is not awaiting moderation");
        }
        product.moderate(decision);
        return productResponse(product);
    }

    private ProductResponse productResponse(ProductEntity product) {
        return CatalogMapper.toResponse(product, inventory.findById(product.getId()).map(i -> i.getAvailable()).orElse(0));
    }
    private CategoryResponse categoryResponse(CategoryEntity category) { return new CategoryResponse(category.getId(), category.getName(), category.getSlug(), category.getDescription(), category.isActive()); }
    private String clean(String text) { return text == null || text.isBlank() ? null : text.trim(); }
}
