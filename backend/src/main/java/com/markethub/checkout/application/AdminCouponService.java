package com.markethub.checkout.application;

import java.util.List;
import java.util.Locale;
import com.markethub.checkout.api.CouponRequest;
import com.markethub.checkout.api.CouponResponse;
import com.markethub.checkout.infrastructure.jpa.CouponEntity;
import com.markethub.checkout.infrastructure.jpa.CouponRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminCouponService {
    private final CouponRepository coupons;
    public AdminCouponService(CouponRepository coupons) { this.coupons = coupons; }
    @Transactional(readOnly = true) public List<CouponResponse> list() {
        return coupons.findAll().stream().map(this::response).toList();
    }
    @Transactional public CouponResponse create(CouponRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (coupons.findByCodeIgnoreCase(code).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon code already exists");
        validate(request);
        return response(coupons.save(new CouponEntity(code, request.discountType(), request.discountValue(), request.minimumOrderAmount(),
                null, request.startsAt(), request.expiresAt(), request.usageLimit(), request.active())));
    }
    @Transactional public CouponResponse update(Long id, CouponRequest request) {
        CouponEntity coupon = coupons.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon was not found"));
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (coupons.existsByCodeIgnoreCaseAndIdNot(code, id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon code already exists");
        validate(request);
        coupon.update(code, request.discountType(), request.discountValue(), request.minimumOrderAmount(), null,
                request.startsAt(), request.expiresAt(), request.usageLimit(), request.active());
        return response(coupon);
    }
    private void validate(CouponRequest request) {
        if ("PERCENT".equals(request.discountType()) && request.discountValue().compareTo(new java.math.BigDecimal("100")) > 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Percentage discount cannot exceed 100");
        if (request.usageLimit() != null && request.usageLimit() < 1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usage limit must be positive");
        if (request.startsAt() != null && request.expiresAt() != null && !request.expiresAt().isAfter(request.startsAt()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon expiry must be after its start time");
    }
    private CouponResponse response(CouponEntity c) { return new CouponResponse(c.getId(), c.getCode(), c.getDiscountType(), c.getDiscountValue(), c.getMinimumOrderAmount(), c.getUsageLimit(), c.getUsageCount(), c.getStartsAt(), c.getExpiresAt(), c.isActive()); }
}
