package com.opspulse.risk.infrastructure.persistence.jpa;

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
@Table(name = "risk_events")
class RiskEventJpaEntity {

    @Id UUID id;
    @Column(name = "risk_type", nullable = false, length = 32) String riskType;
    @Column(nullable = false, length = 12) String severity;
    @Column(name = "entity_type", nullable = false, length = 32) String entityType;
    @Column(name = "entity_id", nullable = false) UUID entityId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_metrics", nullable = false, columnDefinition = "jsonb")
    Map<String, Object> sourceMetrics;
    @Column(nullable = false, columnDefinition = "text") String explanation;
    @Column(name = "recommended_action", nullable = false, columnDefinition = "text") String recommendedAction;
    @Column(nullable = false, length = 16) String status;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "resolved_at") Instant resolvedAt;
    @Column(name = "resolved_by") UUID resolvedBy;
    @Column(name = "organization_id") UUID organizationId;
    @Column(name = "dedup_key", insertable = false, updatable = false, length = 128) String dedupKey;

    protected RiskEventJpaEntity() {}
}
