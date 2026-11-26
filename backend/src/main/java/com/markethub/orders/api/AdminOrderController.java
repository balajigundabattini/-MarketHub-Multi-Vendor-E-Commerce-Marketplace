package com.markethub.orders.api;

import java.util.List;

import jakarta.validation.Valid;
import com.markethub.orders.application.AdminOrderService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final AdminOrderService orders;

    public AdminOrderController(AdminOrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public Page<AdminOrderResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return orders.list(status, page, size);
    }

    @PostMapping("/{orderId}/refunds")
    public AdminRefundResponse refund(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long orderId,
            @Valid @RequestBody AdminRefundRequest request) {

        return orders.refund(
                Long.valueOf(jwt.getSubject()),
                orderId,
                request.reason());
    }

    @PostMapping("/{orderId}/fulfill")
    public ResponseEntity<Void> fulfill(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long orderId) {

        orders.fulfillLatePaidOrder(
                Long.valueOf(jwt.getSubject()),
                orderId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cancellation-requests")
    public List<AdminCancellationRequestResponse> cancellationRequests() {
        return orders.cancellationRequests();
    }

    @PatchMapping("/cancellation-requests/{requestId}")
    public ResponseEntity<Void> reviewCancellationRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId,
            @Valid @RequestBody AdminCancellationReviewRequest request) {

        orders.reviewCancellationRequest(
                Long.valueOf(jwt.getSubject()),
                requestId,
                request);

        return ResponseEntity.noContent().build();
    }
}