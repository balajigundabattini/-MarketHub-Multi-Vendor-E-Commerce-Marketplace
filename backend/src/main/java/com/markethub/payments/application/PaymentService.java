package com.markethub.payments.application;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.markethub.catalog.infrastructure.jpa.InventoryRepository;
import com.markethub.orders.infrastructure.jpa.InventoryReservationRepository;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryEntity;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryRepository;
import com.markethub.orders.infrastructure.jpa.SellerOrderStatusHistoryEntity;
import com.markethub.orders.infrastructure.jpa.SellerOrderStatusHistoryRepository;
import com.markethub.payments.api.PaymentResponse;
import com.markethub.payments.infrastructure.jpa.PaymentEntity;
import com.markethub.payments.infrastructure.jpa.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class PaymentService {
    private final PaymentRepository payments; private final RazorpayGateway gateway; private final InventoryReservationRepository reservations; private final InventoryRepository inventory; private final OrderStatusHistoryRepository orderHistory; private final SellerOrderStatusHistoryRepository sellerHistory;
    public PaymentService(PaymentRepository payments,RazorpayGateway gateway,InventoryReservationRepository reservations,InventoryRepository inventory,OrderStatusHistoryRepository orderHistory,SellerOrderStatusHistoryRepository sellerHistory) { this.payments=payments;this.gateway=gateway;this.reservations=reservations;this.inventory=inventory;this.orderHistory=orderHistory;this.sellerHistory=sellerHistory; }
    @Transactional public PaymentResponse createForOrder(com.markethub.orders.infrastructure.jpa.OrderEntity order) { PaymentEntity p=payments.saveAndFlush(new PaymentEntity(order,"RAZORPAY",null)); p.retry(gateway.createOrder(p.getId(),p.getAmount(),p.getCurrency())); return response(payments.save(p)); }
    @Transactional(readOnly=true) public PaymentResponse get(Long userId,Long id) { return payments.findByIdAndOrderCustomerId(id,userId).map(this::response).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Payment was not found")); }
    @Transactional public PaymentResponse retry(Long userId,Long id) { PaymentEntity p=owned(userId,id); if(!"FAILED".equals(p.getStatus())||!"PENDING_PAYMENT".equals(p.getOrder().getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT,"This payment cannot be retried"); p.retry(gateway.createOrder(p.getId(),p.getAmount(),p.getCurrency())); return response(p); }
    @Transactional public PaymentResponse verify(Long userId,Long id,String providerPaymentId,String signature) { PaymentEntity p=owned(userId,id); if("CAPTURED".equals(p.getStatus())) return response(p); if(!"PENDING".equals(p.getStatus())||!validSignature(p.getProviderOrderId(),providerPaymentId,signature)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Payment signature is invalid or payment is not pending"); capture(p,providerPaymentId); return response(p); }
    @Transactional public void webhook(String providerOrderId,String providerPaymentId,String event,Long amountMinor,String currency) {
        PaymentEntity p=payments.findByProviderOrderIdForUpdate(providerOrderId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Payment order was not found"));
        long expected=PaymentAmounts.minorUnits(p.getAmount(),p.getCurrency());
        if(expected!=amountMinor||!p.getCurrency().equals(currency)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provider payment amount does not match the order");
        if("payment.captured".equals(event)) capture(p,providerPaymentId);
        else if("payment.failed".equals(event)) p.fail(providerPaymentId);
    }
    private PaymentEntity owned(Long userId,Long id) { return payments.findOwnedForUpdate(id,userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Payment was not found")); }
    private void capture(PaymentEntity p,String providerPaymentId) {
        var order=p.getOrder(); if("CAPTURED".equals(p.getStatus())) return;
        if(!"PENDING_PAYMENT".equals(order.getStatus())) {
            if("PAYMENT_EXPIRED".equals(order.getStatus())) { p.capture(providerPaymentId); order.markPaidNeedsReview(); orderHistory.save(new OrderStatusHistoryEntity(order,null,"PAYMENT_EXPIRED","PAYMENT_REVIEW_REQUIRED","Late provider capture requires manual fulfillment or refund review")); return; }
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Order is no longer awaiting payment");
        }
        p.capture(providerPaymentId); order.markPaid(); orderHistory.save(new OrderStatusHistoryEntity(order,null,"PENDING_PAYMENT","CONFIRMED","Payment captured"));
        for(var seller:order.getSellerOrders()) { String old=seller.getStatus(); seller.setStatus("CONFIRMED"); sellerHistory.save(new SellerOrderStatusHistoryEntity(seller,null,old,"CONFIRMED","Payment captured")); for(var item:seller.getItems()) { var reserve=reservations.findByOrderItemId(item.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Stock reservation is missing")); if("ACTIVE".equals(reserve.getStatus())) { var stock=inventory.findByProductIdForUpdate(reserve.getProduct().getId()).orElseThrow(); stock.commitReservation(reserve.getQuantity()); reserve.commit(); } } }
    }
    private boolean validSignature(String orderId,String paymentId,String signature) { try { Mac mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(gateway.keySecret().getBytes(StandardCharsets.UTF_8),"HmacSHA256")); String expected=java.util.HexFormat.of().formatHex(mac.doFinal((orderId+"|"+paymentId).getBytes(StandardCharsets.UTF_8))); return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),signature.getBytes(StandardCharsets.US_ASCII)); } catch(Exception e){return false;} }
    private PaymentResponse response(PaymentEntity p) { return new PaymentResponse(p.getId(),p.getOrder().getId(),p.getStatus(),p.getAmount(),PaymentAmounts.minorUnits(p.getAmount(),p.getCurrency()),p.getCurrency(),"FAILED".equals(p.getStatus())&&"PENDING_PAYMENT".equals(p.getOrder().getStatus()),gateway.keyId(),p.getProviderOrderId()); }
}
