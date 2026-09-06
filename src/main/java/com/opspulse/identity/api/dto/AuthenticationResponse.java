package com.opspulse.identity.api.dto;

import com.opspulse.identity.application.AuthenticationResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record AuthenticationResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String accessToken,
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String refreshToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "900") long expiresIn,
        UserResponse user) {

    public static AuthenticationResponse from(AuthenticationResult result) {
        return new AuthenticationResponse(
                result.accessToken(),
                result.refreshToken(),
                "Bearer",
                result.expiresIn(),
                UserResponse.from(result.user()));
    }
}
