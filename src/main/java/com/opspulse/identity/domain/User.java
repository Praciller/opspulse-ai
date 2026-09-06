package com.opspulse.identity.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record User(
        UUID id,
        EmailAddress email,
        String passwordHash,
        String fullName,
        boolean active,
        Set<Role> roles,
        UUID organizationId,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy,
        long version) {

    public User {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        Objects.requireNonNull(fullName, "fullName must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        roles = Set.copyOf(roles);
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("user must have at least one role");
        }
    }
}
