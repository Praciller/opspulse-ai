package com.opspulse.unit.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.opspulse.identity.application.IdentityAuthenticationException;
import com.opspulse.identity.domain.EmailAddress;
import com.opspulse.identity.domain.Role;
import com.opspulse.identity.domain.User;
import com.opspulse.identity.infrastructure.security.IdentitySecurityProperties;
import com.opspulse.identity.infrastructure.security.NimbusTokenService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenServiceTest {

    private static final String SECRET =
            "test-only-jwt-secret-with-at-least-32-characters";

    @Test
    void issuesSignedAccessAndRefreshTokens() {
        Instant now = Instant.now();
        NimbusTokenService service = service(Clock.fixed(now, ZoneOffset.UTC), 900, 604800);

        var issued = service.issue(user(now));
        var refresh = service.parseRefreshToken(issued.refreshToken());

        assertThat(issued.accessToken()).isNotBlank();
        assertThat(issued.refreshToken()).isNotEqualTo(issued.accessToken());
        assertThat(issued.accessExpiresInSeconds()).isEqualTo(900);
        assertThat(refresh.userId()).isEqualTo(user(now).id());
        assertThat(service.hashRefreshToken(issued.refreshToken())).hasSize(43);
    }

    @Test
    void rejectsAccessTokenWhenRefreshTokenIsRequired() {
        Instant now = Instant.now();
        NimbusTokenService service = service(Clock.fixed(now, ZoneOffset.UTC), 900, 604800);
        var issued = service.issue(user(now));

        assertThatThrownBy(() -> service.parseRefreshToken(issued.accessToken()))
                .isInstanceOf(IdentityAuthenticationException.class);
    }

    @Test
    void rejectsExpiredRefreshToken() {
        Instant old = Instant.now().minusSeconds(180);
        NimbusTokenService service = service(Clock.fixed(old, ZoneOffset.UTC), 1, 1);
        var issued = service.issue(user(old));

        assertThatThrownBy(() -> service.parseRefreshToken(issued.refreshToken()))
                .isInstanceOf(IdentityAuthenticationException.class);
    }

    private static NimbusTokenService service(
            Clock clock, long accessTtl, long refreshTtl) {
        var key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        var properties = new IdentitySecurityProperties(
                new IdentitySecurityProperties.Jwt(SECRET, accessTtl, refreshTtl),
                4,
                IdentitySecurityProperties.RegistrationMode.ADMIN_ONLY);
        return new NimbusTokenService(encoder, decoder, properties, clock);
    }

    private static User user(Instant now) {
        UUID id = UUID.fromString("20000000-0000-0000-0000-000000000001");
        return new User(
                id,
                EmailAddress.of("admin@opspulse.demo"),
                "$2a$04$placeholder",
                "Admin",
                true,
                Set.of(Role.ADMIN),
                null,
                now,
                id,
                now,
                id,
                0);
    }
}
