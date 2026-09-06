package com.opspulse.identity.infrastructure.persistence.jpa;

import com.opspulse.identity.application.port.out.RefreshTokenRepository;
import com.opspulse.identity.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaRefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository repository;

    JpaRefreshTokenRepositoryAdapter(SpringDataRefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        var entity = new RefreshTokenJpaEntity();
        entity.id = token.id();
        entity.userId = token.userId();
        entity.familyId = token.familyId();
        entity.tokenHash = token.tokenHash();
        entity.expiresAt = token.expiresAt();
        entity.revokedAt = token.revokedAt();
        entity.createdAt = token.createdAt();
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<RefreshToken> findByHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByHashForUpdate(String tokenHash) {
        return repository.findOneByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public void revokeFamilyAt(UUID familyId, Instant revokedAt) {
        repository.revokeFamilyAt(familyId, revokedAt);
    }

    @Override
    public int deleteExpiredBefore(Instant expiresBefore) {
        return repository.deleteExpiredBefore(expiresBefore);
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return new RefreshToken(
                entity.id,
                entity.userId,
                entity.familyId,
                entity.tokenHash,
                entity.expiresAt,
                entity.revokedAt,
                entity.createdAt);
    }
}
