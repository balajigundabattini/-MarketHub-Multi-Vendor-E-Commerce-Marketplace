package com.markethub.orders.api;
import com.markethub.orders.application.NotificationService; import jakarta.validation.constraints.*; import org.springframework.data.domain.Page; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.security.oauth2.jwt.Jwt; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/notifications") public class NotificationsController{
 private final NotificationService service; public NotificationsController(NotificationService service){this.service=service;}
 @GetMapping public Page<NotificationService.NotificationView> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return service.list(uid(jwt),page,size);}
 @PutMapping("/{id}/read") public void read(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){service.read(uid(jwt),id);}
 @PutMapping("/read-all") public void readAll(@AuthenticationPrincipal Jwt jwt){service.readAll(uid(jwt));}
 private Long uid(Jwt jwt){return Long.valueOf(jwt.getSubject());}
}
