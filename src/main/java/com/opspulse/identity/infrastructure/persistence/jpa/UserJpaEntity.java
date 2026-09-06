package com.opspulse.identity.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
class UserJpaEntity {

    @Id
    UUID id;

    @Column(nullable = false, unique = true, columnDefinition = "citext")
    String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    String fullName;

    @Column(nullable = false)
    boolean active;

    @Column(name = "organization_id")
    UUID organizationId;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "created_by")
    UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Column(name = "updated_by")
    UUID updatedBy;

    @Version
    @Column(nullable = false)
    long version;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    Set<RoleJpaEntity> roles = new HashSet<>();

    protected UserJpaEntity() {}
}
