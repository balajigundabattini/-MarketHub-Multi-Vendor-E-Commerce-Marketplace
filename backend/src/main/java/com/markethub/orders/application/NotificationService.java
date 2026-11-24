package com.markethub.orders.application;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.orders.infrastructure.jpa.*;
import org.springframework.data.domain.*; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.server.ResponseStatusException;
@Service public class NotificationService {
 private final NotificationRepository notifications; private final UserRepository users;
 public NotificationService(NotificationRepository notifications,UserRepository users){this.notifications=notifications;this.users=users;}
 @Transactional public void create(Long userId,String title,String message,OrderEntity order){users.findById(userId).ifPresent(user->notifications.save(new NotificationEntity(user,title,message,order,null)));}
 @Transactional public void create(Long userId,String title,String message){create(userId,title,message,null);}
 @Transactional(readOnly=true) public Page<NotificationView> list(Long userId,int page,int size){validate(page,size);return notifications.findAllByUser_Id(userId,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt"))).map(NotificationView::from);}
 @Transactional public void read(Long userId,Long id){var row=notifications.findByIdAndUser_Id(id,userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification was not found"));row.markRead();}
 @Transactional public void readAll(Long userId){notifications.markAllRead(userId);}
 private void validate(int page,int size){if(page<0||size<1||size>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Page or page size is invalid");}
 public record NotificationView(Long id,String title,String message,java.time.Instant createdAt,boolean read,Long orderId,Long paymentId){static NotificationView from(NotificationEntity n){return new NotificationView(n.getId(),n.getTitle(),n.getMessage(),n.getCreatedAt(),n.isRead(),n.getOrder()==null?null:n.getOrder().getId(),n.getPayment()==null?null:n.getPayment().getId());}}
}
