package com.opspulse.identity.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    @EntityGraph(attributePaths = "roles")
    Optional<UserJpaEntity> findOneByEmailIgnoreCase(String email);

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<UserJpaEntity> findById(UUID id);

    boolean existsByEmailIgnoreCase(String email);
}
