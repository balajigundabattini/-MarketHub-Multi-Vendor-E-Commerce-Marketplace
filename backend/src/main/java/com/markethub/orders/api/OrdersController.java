package com.markethub.orders.api;

import jakarta.validation.Valid;
import com.markethub.orders.application.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrdersController {
    private final OrderService orders;
    public OrdersController(OrderService orders) { this.orders = orders; }
    @GetMapping public Page<OrderSummaryResponse> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return orders.list(userId(jwt), page, size);
    }
    @GetMapping("/{orderId}") public OrderDetailsResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long orderId) {
        return orders.get(userId(jwt), orderId);
    }
    @PostMapping("/{orderId}/items/{itemId}/cancel")
    public OrderDetailsResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long orderId, @PathVariable Long itemId,
            @Valid @RequestBody OrderReasonRequest request) {
        return orders.requestCancellation(userId(jwt), orderId, itemId, request.reason());
    }
    @PostMapping("/{orderId}/items/{itemId}/returns")
    public OrderDetailsResponse requestReturn(@AuthenticationPrincipal Jwt jwt, @PathVariable Long orderId, @PathVariable Long itemId,
            @Valid @RequestBody OrderReasonRequest request) {
        return orders.requestReturn(userId(jwt), orderId, itemId, request.reason());
    }
    private Long userId(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
}
