package com.opspulse.identity.api.dto;

import com.opspulse.identity.api.validation.ValidPassword;
import com.opspulse.identity.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 320) @Schema(example = "operator@opspulse.demo")
                String email,
        @NotBlank @ValidPassword @Schema(example = "DemoPassword!2026", format = "password")
                String password,
        @NotBlank @Size(max = 120) @Schema(example = "Demo Operator") String fullName,
        @Schema(
                        description =
                                "Defaults to VIEWER. Elevated roles require an authenticated ADMIN.",
                        example = "[\"OPERATOR\"]")
                Set<Role> roles) {}
