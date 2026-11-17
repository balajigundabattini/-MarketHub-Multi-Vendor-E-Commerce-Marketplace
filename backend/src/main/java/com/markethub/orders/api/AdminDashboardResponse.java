package com.markethub.orders.api;
import java.math.BigDecimal;
public record AdminDashboardResponse(BigDecimal grossSales,String currency,long orderCount,long customerCount,long activeSellerCount,long pendingSellerApplications,long productsAwaitingModeration){}
