package com.markethub.cart.api;

import com.markethub.cart.application.CartService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartService cart;
    public CartController(CartService cart) { this.cart = cart; }
    @GetMapping
    public CartResponse get(@AuthenticationPrincipal Jwt jwt) { return cart.getCart(userId(jwt)); }
    @PostMapping("/items")
    public CartResponse add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CartItemRequest request) { return cart.add(userId(jwt), request.productId(), request.quantity()); }
    @PutMapping("/items/{itemId}")
    public CartResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId, @Valid @RequestBody CartQuantityRequest request) { return cart.update(userId(jwt), itemId, request.quantity()); }
    @DeleteMapping("/items/{itemId}")
    public CartResponse remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId) { return cart.remove(userId(jwt), itemId); }
    private Long userId(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
}
