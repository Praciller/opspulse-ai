package com.opspulse.identity.infrastructure.persistence.jpa;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataRefreshTokenRepository
        extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshTokenJpaEntity> findOneByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update RefreshTokenJpaEntity token
               set token.revokedAt = :revokedAt
             where token.familyId = :familyId
               and token.revokedAt is null
            """)
    int revokeFamilyAt(
            @Param("familyId") UUID familyId, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("""
            delete from RefreshTokenJpaEntity token
             where token.expiresAt < :expiresBefore
            """)
    int deleteExpiredBefore(@Param("expiresBefore") Instant expiresBefore);
}
