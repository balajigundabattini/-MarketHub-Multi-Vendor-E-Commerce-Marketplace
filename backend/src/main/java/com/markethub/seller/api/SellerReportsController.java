package com.markethub.seller.api;
import java.time.LocalDate;
import com.markethub.seller.application.SellerReportsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/seller")
public class SellerReportsController {
    private final SellerReportsService reports;
    public SellerReportsController(SellerReportsService reports){this.reports=reports;}
    @GetMapping("/reports") public SellerReportResponse report(@AuthenticationPrincipal Jwt jwt,@RequestParam LocalDate from,@RequestParam LocalDate to){return reports.report(Long.valueOf(jwt.getSubject()),from,to);}
    @GetMapping("/dashboard") public SellerDashboardResponse dashboard(@AuthenticationPrincipal Jwt jwt){return reports.dashboard(Long.valueOf(jwt.getSubject()));}
}
