package com.markethub.orders.infrastructure.jpa;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationRepository extends JpaRepository<NotificationEntity,Long>{
 Page<NotificationEntity> findAllByUser_Id(Long userId,Pageable pageable);
 java.util.Optional<NotificationEntity> findByIdAndUser_Id(Long id,Long userId);
 @org.springframework.data.jpa.repository.Modifying @org.springframework.data.jpa.repository.Query("update NotificationEntity n set n.read=true where n.user.id=:uid and n.read=false") int markAllRead(@org.springframework.data.repository.query.Param("uid") Long userId);
}
