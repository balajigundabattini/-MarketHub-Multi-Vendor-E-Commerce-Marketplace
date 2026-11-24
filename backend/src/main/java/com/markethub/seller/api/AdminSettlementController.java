package com.markethub.seller.api;
import com.markethub.seller.application.SellerSettlementService; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.time.LocalDate; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/settlements") public class AdminSettlementController{
 private final SellerSettlementService service;public AdminSettlementController(SellerSettlementService service){this.service=service;}
 @GetMapping public java.util.List<SellerSettlementService.SettlementView> list(){return service.adminList();}
 @PostMapping public SellerSettlementService.SettlementView create(@Valid @RequestBody CreateSettlement request){return service.create(request.sellerId(),request.periodFrom(),request.periodTo());}
 @PatchMapping("/{id}/paid") public SellerSettlementService.SettlementView paid(@PathVariable Long id,@Valid @RequestBody PaidSettlement request){return service.markPaid(id,request.payoutReference());}
 public record CreateSettlement(@NotNull Long sellerId,@NotNull LocalDate periodFrom,@NotNull LocalDate periodTo){}
 public record PaidSettlement(@NotBlank @Size(max=120) String payoutReference){}
}
