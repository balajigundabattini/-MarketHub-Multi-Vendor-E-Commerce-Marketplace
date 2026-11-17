package com.markethub.payments.api;
import jakarta.validation.constraints.NotBlank;
public record VerifyPaymentRequest(@NotBlank String razorpayPaymentId, @NotBlank String razorpaySignature) { }
