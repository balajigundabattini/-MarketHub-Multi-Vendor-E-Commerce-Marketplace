package com.markethub.orders.api;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record AdminReportResponse(BigDecimal grossSales,long orderCount,BigDecimal commissionTotal,BigDecimal refundTotal,String currency,List<Daily> daily){
    public record Daily(LocalDate date,long orderCount,BigDecimal grossSales,BigDecimal commission,BigDecimal refunds){}
}
