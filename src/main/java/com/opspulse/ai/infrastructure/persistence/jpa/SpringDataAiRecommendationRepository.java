package com.opspulse.ai.infrastructure.persistence.jpa;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAiRecommendationRepository extends JpaRepository<AiRecommendationJpaEntity, UUID> {
    Page<AiRecommendationJpaEntity> findAllByStatus(String status, Pageable pageable);
}
