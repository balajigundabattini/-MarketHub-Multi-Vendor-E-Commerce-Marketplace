package com.markethub.checkout.api;

import java.util.List;
import jakarta.validation.Valid;
import com.markethub.checkout.application.AdminCouponService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/coupons")
public class AdminCouponController {
    private final AdminCouponService service;
    public AdminCouponController(AdminCouponService service) { this.service = service; }
    @GetMapping public List<CouponResponse> list() { return service.list(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public CouponResponse create(@Valid @RequestBody CouponRequest request) { return service.create(request); }
    @PutMapping("/{id}") public CouponResponse update(@PathVariable Long id, @Valid @RequestBody CouponRequest request) { return service.update(id, request); }
}
