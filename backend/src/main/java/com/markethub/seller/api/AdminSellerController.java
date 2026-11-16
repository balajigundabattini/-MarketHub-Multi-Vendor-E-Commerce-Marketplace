package com.markethub.seller.api;

import com.markethub.seller.application.SellerManagementService;
import com.markethub.seller.domain.SellerStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/seller-applications")
public class AdminSellerController {
    private final SellerManagementService sellers;

    public AdminSellerController(SellerManagementService sellers) { this.sellers = sellers; }

    @GetMapping
    public Page<SellerApplicationResponse> list(@RequestParam(required = false) SellerStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return sellers.listApplications(status, pageable);
    }

    @PatchMapping("/{sellerId}/decision")
    public SellerApplicationResponse review(@PathVariable Long sellerId, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SellerReviewRequest request) {
        return sellers.review(sellerId, Long.valueOf(jwt.getSubject()), request);
    }
}
