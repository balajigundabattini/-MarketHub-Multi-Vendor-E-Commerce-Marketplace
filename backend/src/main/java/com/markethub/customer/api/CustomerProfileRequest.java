package com.markethub.customer.api;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerProfileRequest(
        @NotBlank @Size(max = 120) String displayName,
        @Pattern(regexp = "^$|^\\+?[0-9 ()-]{7,32}$") String phone,
        @Past LocalDate birthDate) { }
