package com.markethub.payments.api;
import jakarta.validation.Valid;
import com.markethub.payments.application.PaymentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService payments;
    public PaymentController(PaymentService payments){this.payments=payments;}
    @GetMapping("/{id}") public PaymentResponse get(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return payments.get(userId(jwt),id);}
    @PostMapping("/{id}/retry") public PaymentResponse retry(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return payments.retry(userId(jwt),id);}
    @PostMapping("/{id}/verify") public PaymentResponse verify(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@Valid @RequestBody VerifyPaymentRequest request){return payments.verify(userId(jwt),id,request.razorpayPaymentId(),request.razorpaySignature());}
    private Long userId(Jwt jwt){return Long.valueOf(jwt.getSubject());}
}
