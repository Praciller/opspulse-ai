package com.opspulse.unit.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.opspulse.identity.infrastructure.security.IdentitySecurityProperties;
import com.opspulse.identity.infrastructure.security.JwtConfiguration;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class IdentitySecurityPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(JwtConfiguration.class)
            .withPropertyValues(
                    "opspulse.security.jwt.access-token-ttl-seconds=900",
                    "opspulse.security.jwt.refresh-token-ttl-seconds=604800",
                    "opspulse.security.bcrypt-strength=12",
                    "opspulse.security.registration-mode=ADMIN_ONLY");

    @Test
    void rejectsMissingJwtSecret() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var properties = new IdentitySecurityProperties(
                    new IdentitySecurityProperties.Jwt("", 900, 604800),
                    12,
                    IdentitySecurityProperties.RegistrationMode.ADMIN_ONLY);

            assertThat(factory.getValidator().validate(properties))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("jwt.secret");
        }
    }

    @Test
    void acceptsProductionSafeDefaults() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var properties = new IdentitySecurityProperties(
                    new IdentitySecurityProperties.Jwt(
                            "production-like-secret-with-at-least-32-characters", 900, 604800),
                    12,
                    IdentitySecurityProperties.RegistrationMode.ADMIN_ONLY);

            assertThat(factory.getValidator().validate(properties)).isEmpty();
        }
    }

    @Test
    void securityConfigurationStartupFailsWithoutJwtSecret() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(BindValidationException.class);
        });
    }

    @Test
    void prodProfileSecurityConfigurationFailsWithoutJwtSecret() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "opspulse.security.jwt.secret=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class);
                });
    }

    @Test
    void securityConfigurationStartsWithExternalJwtSecret() {
        contextRunner
                .withPropertyValues(
                        "opspulse.security.jwt.secret="
                                + "production-like-secret-with-at-least-32-characters")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
