package com.markethub.orders.api;

import java.math.BigDecimal;

public record AdminDashboardResponse(
        BigDecimal grossSales,
        BigDecimal collectedRevenue,
        BigDecimal netRevenue,
        BigDecimal refundTotal,
        String currency,
        long orderCount,
        long customerCount,
        long activeSellerCount,
        long pendingSellerApplications,
        long productsAwaitingModeration) {
}