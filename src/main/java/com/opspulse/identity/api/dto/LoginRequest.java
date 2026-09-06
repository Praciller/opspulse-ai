package com.opspulse.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
                @Email
                @Size(max = 320)
                @Schema(example = "admin@opspulse.demo")
                String email,
        @NotBlank @Schema(example = "DemoPassword!2026", format = "password") String password) {}
