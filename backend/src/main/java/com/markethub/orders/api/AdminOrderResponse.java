package com.markethub.orders.api;
import java.math.BigDecimal;
import java.time.Instant;
public record AdminOrderResponse(Long id,String orderNumber,String customerName,Instant createdAt,BigDecimal total,String currency,String paymentStatus,String status,boolean canRefund) { }
