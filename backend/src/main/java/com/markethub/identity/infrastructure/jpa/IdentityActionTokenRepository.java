package com.markethub.identity.infrastructure.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdentityActionTokenRepository extends JpaRepository<IdentityActionTokenEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from IdentityActionTokenEntity t join fetch t.user where t.tokenHash = :hash")
    Optional<IdentityActionTokenEntity> findByHashForUpdate(@Param("hash") String hash);

    @Query("select t from IdentityActionTokenEntity t where t.user.id = :userId and t.purpose = :purpose and t.consumedAt is null")
    List<IdentityActionTokenEntity> findOpen(@Param("userId") Long userId, @Param("purpose") String purpose);
}
