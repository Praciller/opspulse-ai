package com.opspulse.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
        @NotBlank
                @Size(max = 4096)
                @Schema(description = "Refresh JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
                String refreshToken) {}
