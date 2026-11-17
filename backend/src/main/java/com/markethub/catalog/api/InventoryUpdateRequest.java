package com.markethub.catalog.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryUpdateRequest(@NotNull @Min(0) Integer onHand, @Size(max = 40) String reason) { }
