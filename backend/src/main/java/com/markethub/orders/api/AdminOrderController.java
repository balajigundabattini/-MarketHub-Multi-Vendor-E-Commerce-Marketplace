package com.markethub.orders.api;
import jakarta.validation.Valid;
import com.markethub.orders.application.AdminOrderService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {
    private final AdminOrderService orders;
    public AdminOrderController(AdminOrderService orders){this.orders=orders;}
    @GetMapping public Page<AdminOrderResponse> list(@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return orders.list(status,page,size);}
    @PostMapping("/{orderId}/refunds") public AdminRefundResponse refund(@AuthenticationPrincipal Jwt jwt,@PathVariable Long orderId,@Valid @RequestBody AdminRefundRequest request){return orders.refund(Long.valueOf(jwt.getSubject()),orderId,request.reason());}
}
