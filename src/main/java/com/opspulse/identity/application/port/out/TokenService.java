package com.opspulse.identity.application.port.out;

import com.opspulse.identity.domain.User;
import java.time.Instant;
import java.util.UUID;

public interface TokenService {

    IssuedTokenPair issue(User user);

    ParsedRefreshToken parseRefreshToken(String token);

    String hashRefreshToken(String token);

    record IssuedTokenPair(
            String accessToken,
            String refreshToken,
            long accessExpiresInSeconds,
            Instant refreshExpiresAt) {}

    record ParsedRefreshToken(UUID userId, Instant expiresAt) {}
}
