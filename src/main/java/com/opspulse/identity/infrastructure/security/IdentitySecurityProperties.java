package com.opspulse.identity.infrastructure.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("opspulse.security")
public record IdentitySecurityProperties(
        @Valid @NotNull Jwt jwt,
        @Min(4) @Max(16) int bcryptStrength,
        @NotNull RegistrationMode registrationMode) {

    public record Jwt(
            @NotBlank @Size(min = 32) String secret,
            @Min(1) long accessTokenTtlSeconds,
            @Min(1) long refreshTokenTtlSeconds) {}

    public enum RegistrationMode {
        ADMIN_ONLY,
        PUBLIC_VIEWER
    }
}
