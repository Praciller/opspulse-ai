package com.opspulse.ai.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "ai_recommendation_items")
class AiRecommendationItemJpaEntity {
    @Id UUID id;
    @Column(name = "recommendation_id", nullable = false) UUID recommendationId;
    @Column(name = "risk_event_id") UUID riskEventId;
    @Column(nullable = false, length = 32) String role;

    protected AiRecommendationItemJpaEntity() {}
}
