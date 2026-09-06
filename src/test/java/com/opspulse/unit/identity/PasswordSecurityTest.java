package com.opspulse.unit.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.opspulse.identity.api.validation.PasswordValidator;
import com.opspulse.identity.domain.EmailAddress;
import com.opspulse.identity.infrastructure.security.BCryptPasswordHasher;
import com.opspulse.identity.infrastructure.security.IdentitySecurityProperties;
import org.junit.jupiter.api.Test;

class PasswordSecurityTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher(properties());

    @Test
    void hashesAndVerifiesPasswordsWithBCrypt() {
        String rawPassword = "ValidPassword!2026";

        String hash = hasher.hash(rawPassword);

        assertThat(hash).startsWith("$2");
        assertThat(hash).doesNotContain(rawPassword);
        assertThat(hasher.matches(rawPassword, hash)).isTrue();
        assertThat(hasher.matches("wrong-password", hash)).isFalse();
    }

    @Test
    void dummyHashIsAValidBcryptHash() {
        assertThat(hasher.dummyHash()).startsWith("$2");
        assertThat(hasher.matches("constant-style-dummy-password", hasher.dummyHash()))
                .isTrue();
    }

    @Test
    void passwordPolicyUsesUtf8ByteLength() {
        var validator = new PasswordValidator();

        assertThat(validator.isValid("123456789012", null)).isTrue();
        assertThat(validator.isValid("12345678901", null)).isFalse();
        assertThat(validator.isValid("ก".repeat(24), null)).isTrue();
        assertThat(validator.isValid("ก".repeat(25), null)).isFalse();
    }

    @Test
    void normalizesEmailIdentity() {
        assertThat(EmailAddress.of("  Admin@OpsPulse.Demo ").value())
                .isEqualTo("admin@opspulse.demo");
    }

    private static IdentitySecurityProperties properties() {
        return new IdentitySecurityProperties(
                new IdentitySecurityProperties.Jwt(
                        "test-only-jwt-secret-with-at-least-32-characters", 900, 604800),
                4,
                IdentitySecurityProperties.RegistrationMode.ADMIN_ONLY);
    }
}
