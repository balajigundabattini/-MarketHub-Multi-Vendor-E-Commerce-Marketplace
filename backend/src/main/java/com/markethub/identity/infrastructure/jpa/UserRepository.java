package com.markethub.identity.infrastructure.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
