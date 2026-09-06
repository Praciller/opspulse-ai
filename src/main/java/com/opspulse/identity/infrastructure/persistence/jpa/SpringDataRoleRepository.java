package com.opspulse.identity.infrastructure.persistence.jpa;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRoleRepository extends JpaRepository<RoleJpaEntity, UUID> {

    List<RoleJpaEntity> findAllByNameIn(Collection<String> names);
}
