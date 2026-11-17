package com.markethub.orders.api;
import java.math.BigDecimal;
public record AdminRefundResponse(Long refundId,Long orderId,String status,BigDecimal amount,String currency,String providerRefundId) { }
