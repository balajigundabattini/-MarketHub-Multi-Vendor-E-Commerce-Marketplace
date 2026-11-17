package com.markethub.orders.application;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;
import com.markethub.catalog.domain.ProductStatus;
import com.markethub.catalog.infrastructure.jpa.ProductRepository;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.orders.api.AdminDashboardResponse;
import com.markethub.orders.api.AdminReportResponse;
import com.markethub.orders.infrastructure.jpa.OrderRepository;
import com.markethub.payments.infrastructure.jpa.RefundRepository;
import com.markethub.seller.domain.SellerStatus;
import com.markethub.seller.infrastructure.jpa.SellerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class AdminReportsService {
    private static final java.util.List<String> SALES=java.util.List.of("CONFIRMED","ACCEPTED","PROCESSING","SHIPPED","DELIVERED","PAYMENT_REVIEW_REQUIRED","REFUND_PENDING");
    private final OrderRepository orders;private final RefundRepository refunds;private final SellerRepository sellers;private final UserRepository users;private final ProductRepository products;private final Clock clock;
    public AdminReportsService(OrderRepository orders,RefundRepository refunds,SellerRepository sellers,UserRepository users,ProductRepository products,Clock clock){this.orders=orders;this.refunds=refunds;this.sellers=sellers;this.users=users;this.products=products;this.clock=clock;}
    @Transactional(readOnly=true) public AdminReportResponse report(LocalDate from,LocalDate to){LocalDate today=LocalDate.now(clock);if(to==null)to=today;if(from==null)from=to.withDayOfMonth(1);validate(from,to);var start=from.atStartOfDay().toInstant(ZoneOffset.UTC);var end=to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        var orderRows=orders.findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start,end);var refundRows=refunds.findAllByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan("PROCESSED",start,end);
        BigDecimal gross=BigDecimal.ZERO,commission=BigDecimal.ZERO,refundTotal=BigDecimal.ZERO;long count=0;Map<LocalDate,Totals> days=new TreeMap<>();
        for(var order:orderRows){if(!SALES.contains(order.getStatus()))continue;var day=order.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();gross=gross.add(order.getTotal());count++;var t=days.computeIfAbsent(day,k->new Totals());t.orders++;t.gross=t.gross.add(order.getTotal());for(var so:order.getSellerOrders()){if(java.util.List.of("REJECTED","CANCELLED","REFUNDED").contains(so.getStatus()))continue;commission=commission.add(so.getCommission());t.commission=t.commission.add(so.getCommission());}}
        for(var refund:refundRows){refundTotal=refundTotal.add(refund.getAmount());var day=refund.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();days.computeIfAbsent(day,k->new Totals()).refunds=days.get(day).refunds.add(refund.getAmount());}
        var daily=days.entrySet().stream().map(e->new AdminReportResponse.Daily(e.getKey(),e.getValue().orders,e.getValue().gross,e.getValue().commission,e.getValue().refunds)).toList();
        return new AdminReportResponse(gross,count,commission,refundTotal,"INR",daily);
    }
    @Transactional(readOnly=true) public AdminDashboardResponse dashboard(){LocalDate today=LocalDate.now(clock);var current=report(today.withDayOfMonth(1),today);return new AdminDashboardResponse(current.grossSales(),current.currency(),current.orderCount(),users.countByRoleCode("CUSTOMER"),sellers.countByStatus(SellerStatus.APPROVED),sellers.countByStatus(SellerStatus.PENDING_REVIEW),products.countByStatus(ProductStatus.PENDING_APPROVAL));}
    private void validate(LocalDate from,LocalDate to){if(from==null||to==null||to.isBefore(from)||to.isAfter(LocalDate.now(clock))||to.toEpochDay()-from.toEpochDay()>365)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a date range of up to 366 days ending today or earlier");}
    private static class Totals{long orders;BigDecimal gross=BigDecimal.ZERO,commission=BigDecimal.ZERO,refunds=BigDecimal.ZERO;}
}
