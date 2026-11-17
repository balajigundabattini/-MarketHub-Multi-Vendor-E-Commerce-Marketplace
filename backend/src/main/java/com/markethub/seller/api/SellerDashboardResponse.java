package com.markethub.seller.api;
import java.math.BigDecimal;
public record SellerDashboardResponse(BigDecimal monthlySales,long openOrders,long activeProducts,long lowStockProducts,String currency){}
