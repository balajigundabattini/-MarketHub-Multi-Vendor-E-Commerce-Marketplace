package com.markethub.checkout.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import com.markethub.cart.infrastructure.jpa.CartItemEntity;
import com.markethub.cart.infrastructure.jpa.CartItemRepository;
import com.markethub.cart.infrastructure.jpa.CartRepository;
import com.markethub.catalog.domain.ProductStatus;
import com.markethub.catalog.infrastructure.jpa.InventoryRepository;
import com.markethub.catalog.infrastructure.jpa.ProductEntity;
import com.markethub.catalog.infrastructure.jpa.ProductRepository;
import com.markethub.checkout.infrastructure.jpa.CouponEntity;
import com.markethub.checkout.infrastructure.jpa.CouponRepository;
import com.markethub.customer.infrastructure.jpa.AddressEntity;
import com.markethub.customer.infrastructure.jpa.AddressRepository;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.seller.domain.SellerStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CheckoutPricingService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private final CartRepository carts;
    private final CartItemRepository cartItems;
    private final ProductRepository products;
    private final InventoryRepository inventory;
    private final AddressRepository addresses;
    private final CouponRepository coupons;
    private final UserRepository users;
    private final Clock clock;
    private final BigDecimal shippingFlat;
    private final BigDecimal freeShippingThreshold;
    private final BigDecimal taxRate;

    public CheckoutPricingService(CartRepository carts, CartItemRepository cartItems, ProductRepository products,
            InventoryRepository inventory, AddressRepository addresses, CouponRepository coupons, UserRepository users,
            Clock clock, @Value("${markethub.checkout.shipping-flat:99.00}") BigDecimal shippingFlat,
            @Value("${markethub.checkout.free-shipping-threshold:999.00}") BigDecimal freeShippingThreshold,
            @Value("${markethub.checkout.tax-rate:0.0}") BigDecimal taxRate) {
        this.carts = carts; this.cartItems = cartItems; this.products = products; this.inventory = inventory;
        this.addresses = addresses; this.coupons = coupons; this.users = users; this.clock = clock;
        this.shippingFlat = shippingFlat; this.freeShippingThreshold = freeShippingThreshold; this.taxRate = taxRate;
        if (shippingFlat.signum() < 0 || freeShippingThreshold.signum() < 0 || taxRate.signum() < 0 || taxRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Checkout pricing configuration is invalid");
        }
    }

    @Transactional(readOnly = true)
    public QuoteCalculation calculate(Long userId, Long addressId, String rawCouponCode) {
        UserEntity customer = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer account was not found"));
        AddressEntity address = addresses.findActiveOwned(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an active delivery address"));
        var cart = carts.findByUserIdAndStatus(userId, "ACTIVE")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty"));
        List<CartItemEntity> lines = cartItems.findAllByCartId(cart.getId());
        if (lines.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty");

        List<CheckoutLine> checkoutLines = lines.stream().map(line -> {
            ProductEntity product = sellable(line.getProduct().getId());
            var stock = inventory.findById(product.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "A product in your cart is unavailable"));
            if (line.getQuantity() > stock.getAvailable()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Cart quantity exceeds current stock for " + product.getName());
            return new CheckoutLine(line, product, line.getQuantity(), product.getPrice(),
                    product.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
        }).sorted(Comparator.comparing(line -> line.product().getId())).toList();
        String currency = checkoutLines.getFirst().product().getCurrency();
        if (checkoutLines.stream().anyMatch(line -> !currency.equals(line.product().getCurrency()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Checkout requires products in a single currency");
        }
        BigDecimal subtotal = checkoutLines.stream().map(CheckoutLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        String couponCode = rawCouponCode == null || rawCouponCode.isBlank() ? null : rawCouponCode.trim().toUpperCase(Locale.ROOT);
        CouponEntity coupon = couponCode == null ? null : coupons.findByCodeIgnoreCase(couponCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon code is invalid"));
        BigDecimal discount = coupon == null ? BigDecimal.ZERO : validateAndDiscount(coupon, subtotal);
        BigDecimal shipping = subtotal.compareTo(freeShippingThreshold) >= 0 ? BigDecimal.ZERO : shippingFlat;
        BigDecimal tax = subtotal.subtract(discount).max(BigDecimal.ZERO).multiply(taxRate).setScale(4, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(shipping).add(tax).subtract(discount).max(BigDecimal.ZERO);
        String fingerprint = fingerprint(checkoutLines, address, couponCode);
        return new QuoteCalculation(customer, address, coupon, couponCode, checkoutLines, fingerprint,
                subtotal, shipping, tax, discount, total, currency, clock.instant());
    }

    public void validateCouponForRedemption(CouponEntity coupon, BigDecimal subtotal) { validateAndDiscount(coupon, subtotal); }

    private BigDecimal validateAndDiscount(CouponEntity coupon, BigDecimal subtotal) {
        Instant now = clock.instant();
        if (!coupon.isActive() || (coupon.getStartsAt() != null && now.isBefore(coupon.getStartsAt()))
                || (coupon.getExpiresAt() != null && now.isAfter(coupon.getExpiresAt()))
                || (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon is not currently available");
        }
        if (subtotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart subtotal does not meet this coupon's minimum");
        }
        BigDecimal value;
        if ("PERCENT".equals(coupon.getDiscountType())) value = subtotal.multiply(coupon.getDiscountValue()).divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP);
        else value = coupon.getDiscountValue();
        if (coupon.getMaxDiscountAmount() != null) value = value.min(coupon.getMaxDiscountAmount());
        return value.min(subtotal).setScale(4, RoundingMode.HALF_UP);
    }

    private ProductEntity sellable(Long productId) {
        return products.findByIdAndStatusAndStore_StatusAndStore_Seller_StatusAndCategory_Active(
                        productId, ProductStatus.ACTIVE, "ACTIVE", SellerStatus.APPROVED, true)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "A product in your cart is no longer available"));
    }

    private String fingerprint(List<CheckoutLine> lines, AddressEntity address, String couponCode) {
        StringBuilder value = new StringBuilder();
        for (CheckoutLine line : lines) value.append(line.product().getId()).append(':').append(line.product().getStore().getSeller().getId())
                .append(':').append(line.quantity()).append(':').append(line.unitPrice().toPlainString()).append(':').append(line.product().getCurrency()).append('|');
        value.append(address.getId()).append('|').append(address.getRecipientName()).append('|').append(address.getPhone()).append('|')
                .append(address.getLine1()).append('|').append(address.getLine2()).append('|').append(address.getCity()).append('|')
                .append(address.getRegion()).append('|').append(address.getPostalCode()).append('|').append(address.getCountryCode()).append('|')
                .append(couponCode == null ? "" : couponCode);
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }

    public record CheckoutLine(CartItemEntity cartItem, ProductEntity product, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) { }
    public record QuoteCalculation(UserEntity customer, AddressEntity address, CouponEntity coupon, String couponCode,
            List<CheckoutLine> lines, String fingerprint, BigDecimal subtotal, BigDecimal shipping, BigDecimal tax,
            BigDecimal discount, BigDecimal total, String currency, Instant calculatedAt) { }
}
