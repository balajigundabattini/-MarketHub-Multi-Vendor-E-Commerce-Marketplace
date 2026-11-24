package com.markethub.checkout.application;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.UUID;

import com.markethub.cart.infrastructure.jpa.CartItemRepository;
import com.markethub.cart.infrastructure.jpa.CartRepository;
import com.markethub.checkout.api.CheckoutQuoteResponse;
import com.markethub.checkout.api.PlacedOrderResponse;
import com.markethub.checkout.infrastructure.jpa.CheckoutQuoteEntity;
import com.markethub.checkout.infrastructure.jpa.CheckoutQuoteRepository;
import com.markethub.checkout.infrastructure.jpa.CouponRedemptionEntity;
import com.markethub.checkout.infrastructure.jpa.CouponRedemptionRepository;
import com.markethub.checkout.infrastructure.jpa.CouponRepository;
import com.markethub.catalog.infrastructure.jpa.InventoryEntity;
import com.markethub.catalog.infrastructure.jpa.InventoryRepository;
import com.markethub.catalog.infrastructure.jpa.ProductRepository;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.orders.infrastructure.jpa.InventoryReservationEntity;
import com.markethub.orders.infrastructure.jpa.InventoryReservationRepository;
import com.markethub.orders.infrastructure.jpa.OrderEntity;
import com.markethub.orders.infrastructure.jpa.OrderItemEntity;
import com.markethub.orders.infrastructure.jpa.OrderItemRepository;
import com.markethub.orders.infrastructure.jpa.OrderRepository;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryEntity;
import com.markethub.orders.infrastructure.jpa.OrderStatusHistoryRepository;
import com.markethub.orders.infrastructure.jpa.SellerOrderEntity;
import com.markethub.orders.infrastructure.jpa.SellerOrderRepository;
import com.markethub.orders.infrastructure.jpa.SellerOrderStatusHistoryEntity;
import com.markethub.orders.infrastructure.jpa.SellerOrderStatusHistoryRepository;
import com.markethub.payments.application.PaymentService;
import com.markethub.orders.application.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CheckoutService {
    private final CheckoutPricingService pricing;
    private final CheckoutQuoteRepository quotes;
    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;
    private final ProductRepository products;
    private final InventoryRepository inventory;
    private final CartRepository carts;
    private final CartItemRepository cartItems;
    private final OrderRepository orders;
    private final SellerOrderRepository sellerOrders;
    private final OrderItemRepository orderItems;
    private final InventoryReservationRepository reservations;
    private final OrderStatusHistoryRepository orderHistory;
    private final SellerOrderStatusHistoryRepository sellerOrderHistory;
    private final UserRepository users;
    private final java.time.Clock clock;
    private final PaymentService payments;
    private final java.math.BigDecimal commissionRate;
    private final NotificationService notifications;

    public CheckoutService(CheckoutPricingService pricing, CheckoutQuoteRepository quotes, CouponRepository coupons,
            CouponRedemptionRepository redemptions, ProductRepository products, InventoryRepository inventory,
            CartRepository carts, CartItemRepository cartItems, OrderRepository orders,
            SellerOrderRepository sellerOrders,
            OrderItemRepository orderItems, InventoryReservationRepository reservations,
            OrderStatusHistoryRepository orderHistory, SellerOrderStatusHistoryRepository sellerOrderHistory,
            UserRepository users, java.time.Clock clock, PaymentService payments,
            @org.springframework.beans.factory.annotation.Value("${markethub.seller.commission-rate:0.10}") java.math.BigDecimal commissionRate,
            NotificationService notifications) {
        this.pricing = pricing;
        this.quotes = quotes;
        this.coupons = coupons;
        this.redemptions = redemptions;
        this.products = products;
        this.inventory = inventory;
        this.carts = carts;
        this.cartItems = cartItems;
        this.orders = orders;
        this.sellerOrders = sellerOrders;
        this.orderItems = orderItems;
        this.reservations = reservations;
        this.orderHistory = orderHistory;
        this.sellerOrderHistory = sellerOrderHistory;
        this.users = users;
        this.clock = clock;
        this.payments = payments;
        this.commissionRate = commissionRate;
        this.notifications = notifications;
        if (commissionRate.signum() < 0 || commissionRate.compareTo(java.math.BigDecimal.ONE) > 0)
            throw new IllegalArgumentException("Seller commission rate must be from 0 to 1");
    }

    @Transactional
    public CheckoutQuoteResponse quote(Long userId, Long addressId, String couponCode) {
        CheckoutPricingService.QuoteCalculation calculation = pricing.calculate(userId, addressId, couponCode);
        String id = UUID.randomUUID().toString();
        var expiry = clock.instant().plus(Duration.ofMinutes(10));
        CheckoutQuoteEntity entity = new CheckoutQuoteEntity(id, calculation.customer(), calculation.address(),
                calculation.coupon(), calculation.couponCode(), calculation.fingerprint(), calculation.subtotal(),
                calculation.shipping(), calculation.tax(), calculation.discount(), calculation.total(),
                calculation.currency(), expiry);
        quotes.save(entity);
        return toQuoteResponse(entity);
    }

    @Transactional
    public PlacedOrderResponse placeOrder(Long userId, String quoteId, Long addressId, String paymentMethod) {
        CheckoutQuoteEntity quote = quotes.findByIdForUpdate(quoteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Checkout quote was not found"));
        if (!quote.getCustomer().getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Checkout quote was not found");
        if (quote.getConsumedAt() != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Checkout quote has already been used");
        if (!quote.getExpiresAt().isAfter(clock.instant()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Checkout quote expired; request a new quote");
        if (!quote.getAddress().getId().equals(addressId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delivery address changed; request a new quote");

        var cart = carts.findByUserIdAndStatus(userId, "ACTIVE")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Your cart changed; request a new quote"));
        var cartLines = cartItems.findAllByCartId(cart.getId());
        if (cartLines.isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Your cart is empty");
        var productIds = cartLines.stream().map(line -> line.getProduct().getId()).distinct().sorted().toList();
        for (Long productId : productIds) {
            products.findByIdForUpdate(productId).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                    "A product in your cart is no longer available"));
        }
        LinkedHashMap<Long, InventoryEntity> lockedStock = new LinkedHashMap<>();
        for (Long productId : productIds) {
            InventoryEntity stock = inventory.findByProductIdForUpdate(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "A product in your cart is out of stock"));
            lockedStock.put(productId, stock);
        }

        CheckoutPricingService.QuoteCalculation calculation = pricing.calculate(userId, addressId,
                quote.getCouponCode());
        if (!quote.getCartFingerprint().equals(calculation.fingerprint())
                || quote.getSubtotal().compareTo(calculation.subtotal()) != 0
                || quote.getShipping().compareTo(calculation.shipping()) != 0
                || quote.getTax().compareTo(calculation.tax()) != 0
                || quote.getDiscount().compareTo(calculation.discount()) != 0
                || quote.getTotal().compareTo(calculation.total()) != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cart prices, quantities, address, or availability changed; request a new quote");
        }
        for (var line : calculation.lines()) {
            if (lockedStock.get(line.product().getId()).getAvailable() < line.quantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stock changed for " + line.product().getName() + "; update your cart");
            }
        }

        var coupon = quote.getCoupon();
        if (coupon != null) {
            coupon = coupons.findByIdForUpdate(coupon.getId())
                    .orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.CONFLICT, "Coupon is no longer available"));
            pricing.validateCouponForRedemption(coupon, calculation.subtotal());
            if (!quote.getCouponCode().equalsIgnoreCase(coupon.getCode()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon changed; request a new quote");
        }

        String orderStatus = "COD".equals(paymentMethod) ? "CONFIRMED" : "PENDING_PAYMENT";
        OrderEntity order = orders.saveAndFlush(new OrderEntity(calculation.customer(), orderNumber(), orderStatus,
                paymentMethod, quote.getSubtotal(), quote.getShipping(), quote.getTax(), quote.getDiscount(),
                quote.getTotal(), quote.getCurrency(), calculation.address()));
        orderHistory.save(new OrderStatusHistoryEntity(order, calculation.customer(), null, orderStatus,
                "Checkout order placed"));

        var grouped = new LinkedHashMap<Long, java.util.List<CheckoutPricingService.CheckoutLine>>();
        for (var line : calculation.lines())
            grouped.computeIfAbsent(line.product().getStore().getSeller().getId(),
                    ignored -> new java.util.ArrayList<>()).add(line);
        var expiry = clock.instant().plus(Duration.ofMinutes(15));
        for (var group : grouped.values()) {
            var first = group.getFirst().product();
            String sellerStatus = "COD".equals(paymentMethod) ? "CONFIRMED" : "PENDING_PAYMENT";
            var sellerSubtotal = group.stream().map(CheckoutPricingService.CheckoutLine::lineTotal)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            var commission = sellerSubtotal.multiply(commissionRate).setScale(4, java.math.RoundingMode.HALF_UP);
            SellerOrderEntity sellerOrder = sellerOrders
                    .saveAndFlush(new SellerOrderEntity(order, first.getStore().getSeller(),
                            first.getStore().getName(), sellerStatus, sellerSubtotal, commission));
            sellerOrderHistory.save(new SellerOrderStatusHistoryEntity(sellerOrder, calculation.customer(), null,
                    sellerStatus, "Checkout order placed"));
            for (var line : group) {
                OrderItemEntity item = orderItems.saveAndFlush(new OrderItemEntity(sellerOrder, line.product(),
                        line.quantity(), line.unitPrice(), line.lineTotal()));
                InventoryEntity stock = lockedStock.get(line.product().getId());
                stock.reserve(line.quantity());
                reservations.save(new InventoryReservationEntity(item, line.product(), line.quantity(), expiry));
            }
        }
        if (coupon != null) {
            coupon.incrementUsage();
            redemptions.save(new CouponRedemptionEntity(coupon, calculation.customer(), order, quote.getCouponCode(),
                    quote.getDiscount()));
        }
        cartItems.clearForCart(cart.getId());
        quote.consume(clock.instant());
        var paymentResponse = "COD".equals(paymentMethod)
                ? null
                : payments.createForOrder(order);

        notifications.create(
                userId,
                "Order placed",
                "Your order " + order.getPublicOrderNumber() + " has been placed.",
                order);

        return new PlacedOrderResponse(
                order.getId(),
                order.getPublicOrderNumber(),
                paymentResponse == null ? null : paymentResponse.paymentId(),
                paymentResponse == null ? null : paymentResponse.status(),
                paymentResponse == null ? null : paymentResponse.amount(),
                paymentResponse == null ? null : paymentResponse.amountMinor(),
                paymentResponse == null ? null : paymentResponse.currency(),
                paymentResponse == null ? null : paymentResponse.keyId(),
                paymentResponse == null ? null : paymentResponse.providerOrderId());
    }

    private String orderNumber() {
        return "MH-"
                + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").withZone(java.time.ZoneOffset.UTC)
                        .format(clock.instant())
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
    }

    private CheckoutQuoteResponse toQuoteResponse(CheckoutQuoteEntity quote) {
        return new CheckoutQuoteResponse(quote.getId(), quote.getSubtotal(), quote.getShipping(), quote.getTax(),
                quote.getDiscount(), quote.getTotal(), quote.getCurrency(), quote.getExpiresAt());
    }
}
