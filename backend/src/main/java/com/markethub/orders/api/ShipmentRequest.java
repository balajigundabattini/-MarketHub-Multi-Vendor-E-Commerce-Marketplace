package com.markethub.orders.api;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ShipmentRequest(@NotBlank @Size(max = 100) String carrier, @NotBlank @Size(max = 160) String trackingNumber) { }
