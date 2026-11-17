package com.markethub.identity.application;
import com.markethub.identity.api.AdminCustomerResponse;
import com.markethub.identity.domain.UserStatus;
import com.markethub.identity.infrastructure.jpa.AdminAuditEntity;
import com.markethub.identity.infrastructure.jpa.AdminAuditRepository;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class AdminCustomerService {
    private final UserRepository users;private final AdminAuditRepository audit;
    public AdminCustomerService(UserRepository users,AdminAuditRepository audit){this.users=users;this.audit=audit;}
    @Transactional(readOnly=true) public Page<AdminCustomerResponse> list(String rawStatus,int page,int size){
        if(page<0||size<1||size>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Page or page size is invalid");
        String status=rawStatus==null||rawStatus.isBlank()?null:rawStatus;
        UserStatus parsed=null;if(status!=null)try{parsed="SUSPENDED".equals(status)?UserStatus.LOCKED:UserStatus.valueOf(status);}catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Customer status filter is invalid");}
        var pageable=PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt"));
        var rows=parsed==null?users.findDistinctByUserRoles_Role_Code("CUSTOMER",pageable):users.findDistinctByUserRoles_Role_CodeAndStatus("CUSTOMER",parsed,pageable);
        return rows.map(this::response);
    }
    @Transactional public AdminCustomerResponse update(Long adminId,Long id,String requested){
        var customer=users.findCustomerByIdForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Customer was not found"));
        var admin=users.findById(adminId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Administrator was not found"));
        UserStatus target="SUSPENDED".equals(requested)?UserStatus.LOCKED:UserStatus.ACTIVE;String previous=customer.getStatus().name();customer.setStatus(target);
        audit.save(new AdminAuditEntity(admin,"CUSTOMER",id,"STATUS_CHANGED",previous+" -> "+requested));return response(customer);
    }
    private AdminCustomerResponse response(com.markethub.identity.infrastructure.jpa.UserEntity u){return new AdminCustomerResponse(u.getId(),u.getDisplayName(),u.getEmail(),u.getStatus()==UserStatus.LOCKED?"SUSPENDED":u.getStatus().name(),u.getCreatedAt());}
}
