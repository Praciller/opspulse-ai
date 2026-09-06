package com.opspulse.audit.infrastructure.persistence.jpa;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAuditLogRepository extends JpaRepository<AuditLogJpaEntity, UUID> {

    Page<AuditLogJpaEntity> findAllByEntityType(
            String entityType, Pageable pageable);

    Page<AuditLogJpaEntity> findAllByEntityId(
            UUID entityId, Pageable pageable);

    Page<AuditLogJpaEntity> findAllByEntityTypeAndEntityId(
            String entityType, UUID entityId, Pageable pageable);
}
