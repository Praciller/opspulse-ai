package com.opspulse.outbox.infrastructure.persistence.jpa;

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
@Table(name = "outbox_events")
class OutboxEventJpaEntity {

    @Id
    UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 32)
    String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 64)
    String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    Map<String, Object> payload;

    @Column(nullable = false, length = 12)
    String status;

    @Column(name = "retry_count", nullable = false)
    int retryCount;

    @Column(name = "next_attempt_at", nullable = false)
    Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected OutboxEventJpaEntity() {}
}
