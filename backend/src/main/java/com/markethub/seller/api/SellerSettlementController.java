package com.markethub.seller.api;
import com.markethub.seller.application.SellerSettlementService; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.security.oauth2.jwt.Jwt; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/seller/settlements") public class SellerSettlementController{
 private final SellerSettlementService service;public SellerSettlementController(SellerSettlementService service){this.service=service;}
 @GetMapping public java.util.List<SellerSettlementService.SettlementView> list(@AuthenticationPrincipal Jwt jwt){return service.own(Long.valueOf(jwt.getSubject()));}
}
