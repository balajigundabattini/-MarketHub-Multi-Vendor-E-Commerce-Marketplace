package com.markethub.checkout.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PlaceOrderRequest(@NotBlank @Size(max = 36) String quoteId,
        @NotNull Long addressId,
        @NotBlank @Pattern(regexp = "RAZORPAY|COD") String paymentMethod) { }
