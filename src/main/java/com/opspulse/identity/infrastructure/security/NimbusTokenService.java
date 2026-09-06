package com.opspulse.identity.infrastructure.security;

import com.opspulse.identity.application.IdentityAuthenticationException;
import com.opspulse.identity.application.port.out.TokenService;
import com.opspulse.identity.domain.Role;
import com.opspulse.identity.domain.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
public class NimbusTokenService implements TokenService {

    static final String TOKEN_USE_CLAIM = "token_use";
    static final String ACCESS_TOKEN_USE = "access";
    static final String REFRESH_TOKEN_USE = "refresh";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final IdentitySecurityProperties properties;
    private final Clock clock;

    public NimbusTokenService(
            JwtEncoder encoder,
            JwtDecoder decoder,
            IdentitySecurityProperties properties,
            Clock clock) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public IssuedTokenPair issue(User user) {
        Instant issuedAt = clock.instant();
        Instant accessExpiresAt =
                issuedAt.plusSeconds(properties.jwt().accessTokenTtlSeconds());
        Instant refreshExpiresAt =
                issuedAt.plusSeconds(properties.jwt().refreshTokenTtlSeconds());
        var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        var roles = user.roles().stream().map(Role::name).sorted().toList();

        var accessClaims = JwtClaimsSet.builder()
                .subject(user.id().toString())
                .issuedAt(issuedAt)
                .expiresAt(accessExpiresAt)
                .claim("email", user.email().value())
                .claim("roles", roles)
                .claim(TOKEN_USE_CLAIM, ACCESS_TOKEN_USE)
                .id(UUID.randomUUID().toString())
                .build();
        var refreshClaims = JwtClaimsSet.builder()
                .subject(user.id().toString())
                .issuedAt(issuedAt)
                .expiresAt(refreshExpiresAt)
                .claim(TOKEN_USE_CLAIM, REFRESH_TOKEN_USE)
                .id(UUID.randomUUID().toString())
                .build();

        String accessToken =
                encoder.encode(JwtEncoderParameters.from(header, accessClaims)).getTokenValue();
        String refreshToken =
                encoder.encode(JwtEncoderParameters.from(header, refreshClaims)).getTokenValue();
        return new IssuedTokenPair(
                accessToken,
                refreshToken,
                properties.jwt().accessTokenTtlSeconds(),
                refreshExpiresAt);
    }

    @Override
    public ParsedRefreshToken parseRefreshToken(String token) {
        try {
            var jwt = decoder.decode(token);
            if (!REFRESH_TOKEN_USE.equals(jwt.getClaimAsString(TOKEN_USE_CLAIM))
                    || jwt.getExpiresAt() == null) {
                throw new IdentityAuthenticationException();
            }
            return new ParsedRefreshToken(UUID.fromString(jwt.getSubject()), jwt.getExpiresAt());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new IdentityAuthenticationException();
        }
    }

    @Override
    public String hashRefreshToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
