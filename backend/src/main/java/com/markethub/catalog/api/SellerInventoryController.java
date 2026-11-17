package com.markethub.catalog.api;

import java.util.List;

import com.markethub.catalog.application.SellerCatalogService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/inventory")
public class SellerInventoryController {
    private final SellerCatalogService catalog;
    public SellerInventoryController(SellerCatalogService catalog) { this.catalog = catalog; }
    @GetMapping
    public List<InventoryResponse> list(@AuthenticationPrincipal Jwt jwt) { return catalog.inventory(userId(jwt)); }
    @PatchMapping("/{productId}")
    public InventoryResponse adjust(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId,
            @Valid @RequestBody InventoryUpdateRequest request) { return catalog.adjustInventory(userId(jwt), productId, request); }
    private Long userId(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
}
