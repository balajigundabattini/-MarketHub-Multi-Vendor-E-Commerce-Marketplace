package com.markethub.orders.application;
import java.util.List;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.orders.api.AdminOrderResponse;
import com.markethub.orders.api.AdminRefundResponse;
import com.markethub.orders.infrastructure.jpa.OrderRepository;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryEntity;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryRepository;
import com.markethub.orders.infrastructure.jpa.SellerOrderStatusHistoryEntity;
import com.markethub.orders.infrastructure.jpa.SellerOrderStatusHistoryRepository;
import com.markethub.payments.application.RazorpayGateway;
import com.markethub.payments.infrastructure.jpa.PaymentRepository;
import com.markethub.payments.infrastructure.jpa.RefundEntity;
import com.markethub.payments.infrastructure.jpa.RefundRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class AdminOrderService {
    private final OrderRepository orders;private final PaymentRepository payments;private final RefundRepository refunds;
    private final RazorpayGateway gateway;private final UserRepository users;private final OrderStatusHistoryRepository history;private final SellerOrderStatusHistoryRepository sellerHistory;
    public AdminOrderService(OrderRepository orders,PaymentRepository payments,RefundRepository refunds,RazorpayGateway gateway,UserRepository users,OrderStatusHistoryRepository history,SellerOrderStatusHistoryRepository sellerHistory){this.orders=orders;this.payments=payments;this.refunds=refunds;this.gateway=gateway;this.users=users;this.history=history;this.sellerHistory=sellerHistory;}
    @Transactional(readOnly=true) public Page<AdminOrderResponse> list(String status,int page,int size){
        if(page<0||size<1||size>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Page or page size is invalid");
        return orders.findAdminPage(status,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt"))).map(o->{var p=payments.findByOrderId(o.getId()).orElse(null);boolean can=p!=null&&"CAPTURED".equals(p.getStatus())&&List.of("CONFIRMED","PROCESSING","SHIPPED","DELIVERED","PAYMENT_REVIEW_REQUIRED").contains(o.getStatus())&&!refunds.existsByPaymentId(p.getId());return new AdminOrderResponse(o.getId(),o.getPublicOrderNumber(),o.getCustomer().getDisplayName(),o.getCreatedAt(),o.getTotal(),o.getCurrency(),o.getPaymentStatus(),o.getStatus(),can);});
    }
    @Transactional public AdminRefundResponse refund(Long adminId,Long orderId,String reason){
        var payment=payments.findByOrderIdForUpdate(orderId).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Only captured online payments can be refunded"));
        var order=orders.findByIdForUpdate(orderId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order was not found"));
        if(!"CAPTURED".equals(payment.getStatus())||!List.of("CONFIRMED","PROCESSING","SHIPPED","DELIVERED","PAYMENT_REVIEW_REQUIRED").contains(order.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"This order is not eligible for a refund");
        if(refunds.existsByPaymentId(payment.getId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"A refund was already requested for this order");
        var admin=users.findById(adminId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Administrator was not found"));
        RefundEntity refund=refunds.saveAndFlush(new RefundEntity(payment,admin,reason.trim(),order.getStatus()));
        String previous=order.getStatus();order.setStatus("REFUND_PENDING");payment.refundPending();history.save(new OrderStatusHistoryEntity(order,admin,previous,"REFUND_PENDING","Full refund requested: "+reason.trim()));
        RazorpayGateway.RefundResult provider=gateway.refund(payment.getProviderPaymentId(),reason.trim());refund.submitted(provider.id(),provider.status());
        if("PROCESSED".equals(refund.getStatus()))finish(refund,admin);
        else if("FAILED".equals(refund.getStatus()))restoreAfterFailure(refund,admin);
        return response(refund);
    }
    @Transactional public void refundWebhook(String providerRefundId,String event){
        RefundEntity refund=refunds.findByProviderRefundId(providerRefundId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Refund was not found"));
        if(List.of("PROCESSED","FAILED").contains(refund.getStatus()))return;
        if("refund.processed".equals(event)){refund.processed();finish(refund,null);}
        else if("refund.failed".equals(event)){refund.failed();restoreAfterFailure(refund,null);}
    }
    private void finish(RefundEntity refund,com.markethub.identity.infrastructure.jpa.UserEntity actor){
        var order=refund.getOrder();String old=order.getStatus();order.setStatus("REFUNDED");refund.getPayment().refunded();
        history.save(new OrderStatusHistoryEntity(order,actor,old,"REFUNDED","Provider refund processed"));
        for(var seller:order.getSellerOrders()){String sellerOld=seller.getStatus();seller.setStatus("REFUNDED");seller.getItems().forEach(item->item.setStatus("REFUNDED"));sellerHistory.save(new SellerOrderStatusHistoryEntity(seller,actor,sellerOld,"REFUNDED","Order refunded"));}
    }
    private void restoreAfterFailure(RefundEntity refund,com.markethub.identity.infrastructure.jpa.UserEntity actor){var order=refund.getOrder();refund.getPayment().refundFailed();order.setStatus(refund.getOrderStatusBefore());history.save(new OrderStatusHistoryEntity(order,actor,"REFUND_PENDING",refund.getOrderStatusBefore(),"Provider refund failed"));}
    private AdminRefundResponse response(RefundEntity r){return new AdminRefundResponse(r.getId(),r.getOrder().getId(),r.getStatus(),r.getAmount(),r.getCurrency(),r.getProviderRefundId());}
}
