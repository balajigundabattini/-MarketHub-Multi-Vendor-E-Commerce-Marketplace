package com.markethub.seller.api;

import java.time.Instant;

public record SellerApplicationResponse(
        Long id, String legalName, String businessName, String status,
        String reviewNote, Instant createdAt, Instant reviewedAt,
        String email, String applicantName) { }
