package com.markethub.seller.api;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record SellerReportResponse(BigDecimal grossSales,BigDecimal commission,BigDecimal netEarnings,long orderCount,String currency,List<Daily> dailySales){
    public record Daily(LocalDate date,long orderCount,BigDecimal grossSales,BigDecimal commission,BigDecimal netEarnings){}
}
