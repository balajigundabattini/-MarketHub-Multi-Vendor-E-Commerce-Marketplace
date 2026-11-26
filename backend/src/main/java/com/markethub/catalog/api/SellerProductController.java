package com.markethub.catalog.api;

import java.net.URI;

import com.markethub.catalog.application.SellerCatalogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/seller/products")
public class SellerProductController {
    private final SellerCatalogService catalog;
    public SellerProductController(SellerCatalogService catalog) { this.catalog = catalog; }

    @GetMapping
    public Page<ProductResponse> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
                System.out.println(">>> SELLER PRODUCT CONTROLLER REACHED");
        return catalog.listProducts(userId(jwt), page, size);
    }
    @GetMapping("/{productId}")
    public ProductResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
        return catalog.getProduct(userId(jwt), productId);
    }
    @PostMapping
    public ResponseEntity<ProductResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProductRequest request) {
        ProductResponse response = catalog.createProduct(userId(jwt), request);
        return ResponseEntity.created(URI.create("/api/v1/seller/products/" + response.id())).body(response);
    }
    @PutMapping("/{productId}")
    public ProductResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId,
            @Valid @RequestBody ProductRequest request) { return catalog.updateProduct(userId(jwt), productId, request); }
    @PostMapping("/{productId}/deactivate")
    public ResponseEntity<Void> deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
        catalog.deactivateProduct(userId(jwt), productId);
        return ResponseEntity.noContent().build();
    }
    private Long userId(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
}
