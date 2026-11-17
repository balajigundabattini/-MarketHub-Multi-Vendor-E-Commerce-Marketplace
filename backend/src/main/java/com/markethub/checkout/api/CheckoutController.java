package com.markethub.checkout.api;

import jakarta.validation.Valid;
import com.markethub.checkout.application.CheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {
    private final CheckoutService checkout;
    public CheckoutController(CheckoutService checkout) { this.checkout = checkout; }

    @PostMapping("/quote")
    public CheckoutQuoteResponse quote(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckoutQuoteRequest request) {
        return checkout.quote(userId(jwt), request.addressId(), request.couponCode());
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public PlacedOrderResponse place(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlaceOrderRequest request) {
        return checkout.placeOrder(userId(jwt), request.quoteId(), request.addressId(), request.paymentMethod());
    }
    private Long userId(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
}
