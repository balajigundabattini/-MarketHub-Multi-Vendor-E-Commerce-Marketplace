package com.markethub.wishlist.api;

import java.net.URI;

import com.markethub.wishlist.application.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistController {
    private final WishlistService wishlist;
    public WishlistController(WishlistService wishlist) { this.wishlist = wishlist; }
    @GetMapping
    public WishlistResponse get(@AuthenticationPrincipal Jwt jwt) { return wishlist.get(userId(jwt)); }
    @PostMapping("/items")
    public ResponseEntity<WishlistResponse> add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody WishlistItemRequest request) {
        WishlistResponse response = wishlist.add(userId(jwt), request.productId());
        return ResponseEntity.created(URI.create("/api/v1/wishlist")).body(response);
    }
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
        wishlist.remove(userId(jwt), productId);
        return ResponseEntity.noContent().build();
    }
    private Long userId(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
}
