package com.opspulse.identity.application.port.out;

import com.opspulse.identity.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByHash(String tokenHash);

    Optional<RefreshToken> findByHashForUpdate(String tokenHash);

    void revokeFamilyAt(UUID familyId, Instant revokedAt);

    int deleteExpiredBefore(Instant expiresBefore);
}
