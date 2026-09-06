package com.opspulse.audit.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs")
class AuditLogJpaEntity {

    @Id
    UUID id;

    @Column(name = "actor_user_id")
    UUID actorUserId;

    @Column(nullable = false, length = 64)
    String action;

    @Column(name = "entity_type", nullable = false, length = 32)
    String entityType;

    @Column(name = "entity_id")
    UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_snapshot", columnDefinition = "jsonb")
    Map<String, Object> beforeSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_snapshot", columnDefinition = "jsonb")
    Map<String, Object> afterSnapshot;

    @Column(name = "request_id", length = 64)
    String requestId;

    @Column(name = "ip_address", length = 64)
    String ipAddress;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected AuditLogJpaEntity() {}
}
