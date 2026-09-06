package com.opspulse.ai.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai_recommendations")
class AiRecommendationJpaEntity {
    @Id UUID id;
    @Column(name = "prompt_version", nullable = false, length = 32) String promptVersion;
    @Column(name = "model_provider_name", nullable = false, length = 64) String modelProviderName;
    @Column(name = "generated_by", nullable = false, length = 16) String generatedBy;
    @Column(name = "generated_summary", nullable = false, columnDefinition = "text") String generatedSummary;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generated_actions", nullable = false, columnDefinition = "jsonb")
    List<Map<String, Object>> generatedActions;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generated_message_drafts", nullable = false, columnDefinition = "jsonb")
    List<Map<String, Object>> generatedMessageDrafts;
    @Column(columnDefinition = "numeric(4,3)") Double confidence;
    @Column(nullable = false, length = 16) String status;
    @Column(name = "user_feedback", columnDefinition = "text") String userFeedback;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "created_by") UUID createdBy;
    @Column(name = "created_by_type", nullable = false, length = 16) String createdByType;
    @Column(name = "organization_id") UUID organizationId;

    protected AiRecommendationJpaEntity() {}
}
