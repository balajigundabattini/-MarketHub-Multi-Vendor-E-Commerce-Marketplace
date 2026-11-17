package com.markethub.orders.api;
import java.time.LocalDate;
import com.markethub.orders.application.AdminReportsService;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin")
public class AdminReportsController {
    private final AdminReportsService reports;
    public AdminReportsController(AdminReportsService reports){this.reports=reports;}
    @GetMapping("/reports") public AdminReportResponse report(@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to){return reports.report(from,to);}
    @GetMapping("/dashboard") public AdminDashboardResponse dashboard(){return reports.dashboard();}
}
