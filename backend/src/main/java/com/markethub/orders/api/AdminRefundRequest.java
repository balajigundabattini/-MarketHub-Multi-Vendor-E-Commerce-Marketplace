package com.markethub.orders.api;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record AdminRefundRequest(@NotBlank @Size(max=500) String reason) { }
