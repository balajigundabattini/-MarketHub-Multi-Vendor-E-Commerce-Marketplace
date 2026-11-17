package com.markethub.orders.api;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public record SellerOrderStatusRequest(@NotBlank @Pattern(regexp = "ACCEPTED|REJECTED|PROCESSING|CANCELLED|DELIVERED") String status) { }
