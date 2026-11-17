package com.markethub.catalog.api;

import java.math.BigDecimal;
import java.util.List;

import com.markethub.catalog.application.CatalogService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {
    private final CatalogService catalog;
    public CatalogController(CatalogService catalog) { this.catalog = catalog; }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() { return catalog.activeCategories(); }

    @GetMapping("/products")
    public Page<ProductResponse> products(@RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return catalog.search(search, category, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping("/products/{productId}")
    public ProductResponse product(@PathVariable Long productId) { return catalog.getProduct(productId); }
}
