package com.markethub.catalog.api;

import com.markethub.catalog.application.AdminCatalogService;
import com.markethub.catalog.domain.ProductStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {
    private final AdminCatalogService catalog;

    public AdminCatalogController(AdminCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/categories")
    public java.util.List<CategoryResponse> categories() {
        return catalog.categories();
    }

    @PostMapping("/categories")
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return catalog.createCategory(request);
    }

    @PutMapping("/categories/{categoryId}")
    public CategoryResponse updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryRequest request) {
        return catalog.updateCategory(categoryId, request);
    }

    @PatchMapping("/categories/{categoryId}/status")
    public CategoryResponse setCategoryActive(@PathVariable Long categoryId,
            @Valid @RequestBody CategoryStatusRequest request) {
        return catalog.setCategoryActive(categoryId, request.active());
    }

    @GetMapping("/products")
    public Page<ProductResponse> products(@RequestParam(required = false) ProductStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return catalog.products(status, pageable);
    }

    @PatchMapping("/products/{productId}/moderation")
    public ProductResponse moderate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long productId,
            @Valid @RequestBody ProductModerationRequest request) {

        Long adminId = Long.valueOf(jwt.getSubject());
        return catalog.moderate(adminId, productId, request.status());
    }

    public record ProductModerationRequest(@jakarta.validation.constraints.NotNull ProductStatus status) {
    }

    public record CategoryStatusRequest(@jakarta.validation.constraints.NotNull Boolean active) {
    }
}
