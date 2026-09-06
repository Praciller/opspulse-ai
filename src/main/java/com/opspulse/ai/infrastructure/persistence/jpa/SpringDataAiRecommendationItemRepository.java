package com.opspulse.ai.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAiRecommendationItemRepository extends JpaRepository<AiRecommendationItemJpaEntity, UUID> {
    List<AiRecommendationItemJpaEntity> findAllByRecommendationIdOrderById(UUID recommendationId);
    void deleteAllByRecommendationId(UUID recommendationId);
}
