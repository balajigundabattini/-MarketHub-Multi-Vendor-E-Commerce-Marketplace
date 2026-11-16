package com.markethub.identity.infrastructure.jpa;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshTokenEntity token join fetch token.user where token.tokenHash = :hash")
    Optional<RefreshTokenEntity> findByHashForUpdate(@Param("hash") String hash);

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshTokenEntity token set token.revokedAt = :now " +
            "where token.familyId = :familyId and token.revokedAt is null")
    int revokeFamily(@Param("familyId") String familyId, @Param("now") Instant now);
}
