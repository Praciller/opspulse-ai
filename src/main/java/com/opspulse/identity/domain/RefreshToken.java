package com.opspulse.identity.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RefreshToken(
        UUID id,
        UUID userId,
        UUID familyId,
        String tokenHash,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt) {

    public RefreshToken {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(familyId, "familyId must not be null");
        Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public boolean isActiveAt(Instant instant) {
        return revokedAt == null && expiresAt.isAfter(instant);
    }

    public RefreshToken revokeAt(Instant instant) {
        return new RefreshToken(id, userId, familyId, tokenHash, expiresAt, instant, createdAt);
    }
}
