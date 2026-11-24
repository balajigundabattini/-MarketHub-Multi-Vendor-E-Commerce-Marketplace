package com.markethub.orders.application;

import java.time.Clock;
import com.markethub.catalog.infrastructure.jpa.InventoryRepository;
import com.markethub.orders.infrastructure.jpa.InventoryReservationRepository;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryEntity;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryRepository;
import com.markethub.orders.infrastructure.jpa.SellerOrderStatusHistoryEntity;
import com.markethub.orders.infrastructure.jpa.SellerOrderStatusHistoryRepository;
import com.markethub.payments.infrastructure.jpa.PaymentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationExpiryService {
    private final InventoryReservationRepository reservations;
    private final InventoryRepository inventory;
    private final PaymentRepository payments;
    private final OrderStatusHistoryRepository orderHistory;
    private final SellerOrderStatusHistoryRepository sellerHistory;
    private final Clock clock;

    public ReservationExpiryService(InventoryReservationRepository reservations, InventoryRepository inventory,
            PaymentRepository payments,
            OrderStatusHistoryRepository orderHistory, SellerOrderStatusHistoryRepository sellerHistory, Clock clock) {
        this.reservations = reservations;
        this.inventory = inventory;
        this.payments = payments;
        this.orderHistory = orderHistory;
        this.sellerHistory = sellerHistory;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${markethub.checkout.reservation-sweep-ms:60000}")
    @Transactional
    public void expirePendingReservations() {
        for (var reservation : reservations.findAllByStatusAndExpiresAtBefore("ACTIVE", clock.instant())) {
            var seller = reservation.getOrderItem().getSellerOrder();
            var order = seller.getOrder();
            if (!"PENDING_PAYMENT".equals(order.getStatus()))
                continue;
            var payment = payments.findByOrderIdForUpdate(order.getId()).orElse(null);
            if (payment == null
                    || !java.util.Set.of("PENDING", "FAILED").contains(payment.getStatus())) {
                continue;
            }
            var locked = reservations.findByOrderItemId(reservation.getOrderItem().getId()).orElse(null);
            if (locked == null || !"ACTIVE".equals(locked.getStatus())
                    || locked.getExpiresAt().isAfter(clock.instant()))
                continue;
            var stock = inventory.findByProductIdForUpdate(locked.getProduct().getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Inventory record not found for product " + locked.getProduct().getId()));

            stock.release(locked.getQuantity());
            locked.release();
            locked.getOrderItem().setStatus("CANCELLED");
            boolean sellerFinished = seller.getItems().stream().allMatch(item -> "CANCELLED".equals(item.getStatus()));
            if (sellerFinished && !"CANCELLED".equals(seller.getStatus())) {
                String old = seller.getStatus();
                seller.setStatus("CANCELLED");
                sellerHistory.save(new SellerOrderStatusHistoryEntity(seller, null, old, "CANCELLED",
                        "Payment reservation expired"));
            }
            boolean allFinished = order.getSellerOrders().stream().allMatch(sub -> "CANCELLED".equals(sub.getStatus()));
            if (allFinished && "PENDING_PAYMENT".equals(order.getStatus())) {
                order.setStatus("PAYMENT_EXPIRED");
                orderHistory.save(new OrderStatusHistoryEntity(order, null, "PENDING_PAYMENT", "PAYMENT_EXPIRED",
                        "Payment reservation expired"));
                payment.expire();
            }
        }
    }
}
