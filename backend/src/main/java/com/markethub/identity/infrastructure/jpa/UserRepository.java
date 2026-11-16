package com.markethub.identity.infrastructure.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmail(String email);

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
