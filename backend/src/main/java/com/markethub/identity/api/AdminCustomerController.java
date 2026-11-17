package com.markethub.identity.api;
import jakarta.validation.Valid;
import com.markethub.identity.application.AdminCustomerService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/customers")
public class AdminCustomerController {
    private final AdminCustomerService customers;
    public AdminCustomerController(AdminCustomerService customers){this.customers=customers;}
    @GetMapping public Page<AdminCustomerResponse> list(@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return customers.list(status,page,size);}
    @PatchMapping("/{id}/status") public AdminCustomerResponse status(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@Valid @RequestBody AdminCustomerStatusRequest request){return customers.update(Long.valueOf(jwt.getSubject()),id,request.status());}
}
