package com.opspulse.identity.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "roles")
class RoleJpaEntity {

    @Id
    UUID id;

    @Column(nullable = false, unique = true, length = 32)
    String name;

    protected RoleJpaEntity() {}
}
