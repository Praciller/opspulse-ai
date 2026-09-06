package com.opspulse.ai.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_usage_audit")
class AiUsageAuditJpaEntity {
    @Id UUID id;
    @Column(name = "recommendation_id") UUID recommendationId;
    @Column(nullable = false, length = 64) String provider;
    @Column(nullable = false, length = 64) String model;
    @Column(name = "prompt_version", nullable = false, length = 32) String promptVersion;
    @Column(name = "input_token_count") Integer inputTokenCount;
    @Column(name = "output_token_count") Integer outputTokenCount;
    @Column(name = "latency_ms") Integer latencyMs;
    @Column(nullable = false) boolean success;
    @Column(name = "error_class", length = 120) String errorClass;
    @Column(name = "request_id", length = 64) String requestId;
    @Column(name = "created_at", nullable = false) Instant createdAt;

    protected AiUsageAuditJpaEntity() {}
}
