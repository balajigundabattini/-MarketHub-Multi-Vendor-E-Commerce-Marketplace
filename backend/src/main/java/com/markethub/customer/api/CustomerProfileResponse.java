package com.markethub.customer.api;

import java.time.LocalDate;
public record CustomerProfileResponse(Long userId, String email, String displayName, String phone, LocalDate birthDate) { }
