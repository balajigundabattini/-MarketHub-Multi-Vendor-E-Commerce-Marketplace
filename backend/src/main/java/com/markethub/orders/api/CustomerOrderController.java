
package com.markethub.orders.api;

import com.markethub.orders.application.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/orders")
@Validated
public class CustomerOrderController {

    private final OrderService orders;

    public CustomerOrderController(OrderService orders) {
        this.orders = orders;
    }

    // Get customer order history
    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(orders.list(userId, page, size));
    }

    // Get individual order details
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailsResponse> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long orderId) {

        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(orders.get(userId, orderId));
    }

    // Request cancellation of an order item
    @PostMapping("/{orderId}/items/{itemId}/cancellation")
    public ResponseEntity<OrderDetailsResponse> requestCancellation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody OrderRequest body) {

        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(
                orders.requestCancellation(userId, orderId, itemId, body.reason()));
    }

    // Request return of a delivered order item
    @PostMapping("/{orderId}/items/{itemId}/return")
    public ResponseEntity<OrderDetailsResponse> requestReturn(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody OrderRequest body) {

        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(
                orders.requestReturn(userId, orderId, itemId, body.reason()));
    }

    public record OrderRequest(
            @NotBlank @Size(max = 500) String reason) {
    }
}
