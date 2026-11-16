package com.markethub.identity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActionTokenRequest(@NotBlank @Size(max = 128) String token) { }
