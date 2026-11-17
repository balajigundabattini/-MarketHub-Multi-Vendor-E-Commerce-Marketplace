package com.markethub.seller.application;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;
import com.markethub.catalog.domain.ProductStatus;
import com.markethub.catalog.infrastructure.jpa.InventoryRepository;
import com.markethub.catalog.infrastructure.jpa.ProductRepository;
import com.markethub.seller.api.SellerDashboardResponse;
import com.markethub.seller.api.SellerReportResponse;
import com.markethub.seller.domain.SellerStatus;
import com.markethub.seller.infrastructure.jpa.SellerRepository;
import com.markethub.orders.infrastructure.jpa.SellerOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class SellerReportsService {
    private static final java.util.List<String> SALES=java.util.List.of("CONFIRMED","ACCEPTED","PROCESSING","SHIPPED","DELIVERED");
    private static final java.util.List<String> OPEN=java.util.List.of("CONFIRMED","ACCEPTED","PROCESSING","CANCELLATION_REQUESTED");
    private final SellerRepository sellers;private final SellerOrderRepository orders;private final ProductRepository products;private final InventoryRepository inventory;private final Clock clock;
    public SellerReportsService(SellerRepository sellers,SellerOrderRepository orders,ProductRepository products,InventoryRepository inventory,Clock clock){this.sellers=sellers;this.orders=orders;this.products=products;this.inventory=inventory;this.clock=clock;}
    @Transactional(readOnly=true) public SellerReportResponse report(Long userId,LocalDate from,LocalDate to){
        validate(from,to); approved(userId);var rows=orders.findAllBySeller_User_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId,from.atStartOfDay().toInstant(ZoneOffset.UTC),to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
        Map<LocalDate,Totals> daily=new TreeMap<>();BigDecimal gross=BigDecimal.ZERO,commission=BigDecimal.ZERO;long count=0;String currency="INR";
        for(var row:rows){if(!SALES.contains(row.getStatus()))continue;var order=row.getOrder();currency=order.getCurrency();gross=gross.add(row.getSubtotal());commission=commission.add(row.getCommission());count++;daily.computeIfAbsent(order.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(),k->new Totals()).add(row.getSubtotal(),row.getCommission());}
        var dailyRows=daily.entrySet().stream().map(e->new SellerReportResponse.Daily(e.getKey(),e.getValue().count,e.getValue().gross,e.getValue().commission,e.getValue().gross.subtract(e.getValue().commission))).toList();
        return new SellerReportResponse(gross,commission,gross.subtract(commission),count,currency,dailyRows);
    }
    @Transactional(readOnly=true) public SellerDashboardResponse dashboard(Long userId){
        approved(userId);LocalDate today=LocalDate.now(clock);LocalDate first=today.withDayOfMonth(1);var rows=orders.findAllBySeller_User_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId,first.atStartOfDay().toInstant(ZoneOffset.UTC),today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
        BigDecimal sales=rows.stream().filter(r->SALES.contains(r.getStatus())).map(r->r.getSubtotal()).reduce(BigDecimal.ZERO,BigDecimal::add);
        var productsOwned=products.findAllByStore_Seller_User_Id(userId);var ids=productsOwned.stream().map(p->p.getId()).toList();long low=inventory.findAllByProductIdIn(ids).stream().filter(i->i.getAvailable()<=5).count();
        return new SellerDashboardResponse(sales,orders.countBySeller_User_IdAndStatusIn(userId,OPEN),products.countByStore_Seller_User_IdAndStatus(userId,ProductStatus.ACTIVE),low,"INR");
    }
    private void approved(Long userId){var seller=sellers.findByUserId(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Approved seller access is required"));if(seller.getStatus()!=SellerStatus.APPROVED)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Approved seller access is required");}
    private void validate(LocalDate from,LocalDate to){if(from==null||to==null||to.isBefore(from)||to.isAfter(LocalDate.now(clock))||to.toEpochDay()-from.toEpochDay()>365)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a date range of up to 366 days ending today or earlier");}
    private static class Totals{long count;BigDecimal gross=BigDecimal.ZERO,commission=BigDecimal.ZERO;void add(BigDecimal g,BigDecimal c){count++;gross=gross.add(g);commission=commission.add(c);}}
}
