package com.markethub.checkout.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckoutQuoteRequest(@NotNull Long addressId, @Size(max = 40) String couponCode) { }
