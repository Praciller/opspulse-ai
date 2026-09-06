package com.opspulse.identity.api.dto;

import com.opspulse.identity.domain.Role;
import com.opspulse.identity.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        @Schema(example = "b3d5e61f-9c8b-4a4d-8fb0-1dc9f0b79701") UUID id,
        @Schema(example = "admin@opspulse.demo") String email,
        @Schema(example = "Demo Administrator") String fullName,
        boolean active,
        Set<Role> roles,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.id(),
                user.email().value(),
                user.fullName(),
                user.active(),
                user.roles(),
                user.createdAt());
    }
}
