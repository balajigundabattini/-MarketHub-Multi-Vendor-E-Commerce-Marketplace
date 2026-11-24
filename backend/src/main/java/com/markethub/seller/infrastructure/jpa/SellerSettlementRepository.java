package com.markethub.seller.infrastructure.jpa;
import java.time.LocalDate; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface SellerSettlementRepository extends JpaRepository<SellerSettlementEntity,Long>{
 List<SellerSettlementEntity> findAllBySeller_User_IdOrderByCreatedAtDesc(Long userId);
 List<SellerSettlementEntity> findAllByOrderByCreatedAtDesc();
 boolean existsBySeller_IdAndPeriodFromAndPeriodTo(Long sellerId,LocalDate from,LocalDate to);
 @org.springframework.data.jpa.repository.Query("select (count(s)>0) from SellerSettlementEntity s where s.seller.id=:sellerId and s.periodFrom<=:to and s.periodTo>=:from")
 boolean existsOverlappingPeriod(@org.springframework.data.repository.query.Param("sellerId")Long sellerId,@org.springframework.data.repository.query.Param("from")LocalDate from,@org.springframework.data.repository.query.Param("to")LocalDate to);
 Optional<SellerSettlementEntity> findByIdAndSeller_User_Id(Long id,Long userId);
}
