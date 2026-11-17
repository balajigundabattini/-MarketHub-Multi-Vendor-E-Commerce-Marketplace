package com.markethub.orders.api;
import jakarta.validation.Valid;
import com.markethub.orders.application.SellerFulfillmentService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/seller/orders")
public class SellerOrdersController {
    private final SellerFulfillmentService fulfillment;
    public SellerOrdersController(SellerFulfillmentService fulfillment) { this.fulfillment = fulfillment; }
    @GetMapping public Page<SellerOrderResponse> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return fulfillment.list(userId(jwt), page, size); }
    @PatchMapping("/{id}/status") public SellerOrderResponse status(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody SellerOrderStatusRequest request) { return fulfillment.update(userId(jwt), id, request.status()); }
    @PostMapping("/{id}/shipments") public SellerOrderResponse ship(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody ShipmentRequest request) { return fulfillment.ship(userId(jwt), id, request.carrier(), request.trackingNumber()); }
    @PatchMapping("/{id}/returns/{requestId}") public SellerOrderResponse returnDecision(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @PathVariable Long requestId, @Valid @RequestBody SellerReturnDecisionRequest request) { return fulfillment.reviewReturn(userId(jwt), id, requestId, request.decision(), request.note()); }
    private Long userId(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
}
