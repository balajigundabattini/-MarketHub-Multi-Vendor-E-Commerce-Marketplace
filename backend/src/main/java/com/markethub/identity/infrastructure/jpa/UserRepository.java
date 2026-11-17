package com.markethub.identity.infrastructure.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    org.springframework.data.domain.Page<UserEntity> findDistinctByUserRoles_Role_Code(String code,org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<UserEntity> findDistinctByUserRoles_Role_CodeAndStatus(String code,com.markethub.identity.domain.UserStatus status,org.springframework.data.domain.Pageable pageable);
    Optional<UserEntity> findByIdAndUserRoles_Role_Code(Long id,String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u join u.userRoles ur join ur.role r where u.id=:id and r.code='CUSTOMER'")
    Optional<UserEntity> findCustomerByIdForUpdate(@Param("id") Long id);

    boolean existsByEmail(String email);
    @Query("select count(distinct u.id) from UserEntity u join u.userRoles ur join ur.role r where r.code = :code")
    long countByRoleCode(@Param("code") String code);

    @Query("select distinct u from UserEntity u " +
            "left join fetch u.userRoles userRole " +
            "left join fetch userRole.role " +
            "where u.email = :email")
    Optional<UserEntity> findByEmailWithRoles(@Param("email") String email);

    @Query("select distinct u from UserEntity u " +
            "left join fetch u.userRoles userRole " +
            "left join fetch userRole.role " +
            "where u.id = :id")
    Optional<UserEntity> findByIdWithRoles(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);
}
