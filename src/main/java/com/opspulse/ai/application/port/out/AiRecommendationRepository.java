package com.opspulse.ai.application.port.out;

import com.opspulse.ai.domain.AiRecommendation;
import com.opspulse.ai.domain.RecommendationStatus;
import com.opspulse.shared.web.PagedResponse;
import java.util.Optional;
import java.util.UUID;

public interface AiRecommendationRepository {
    AiRecommendation save(AiRecommendation recommendation);
    Optional<AiRecommendation> findById(UUID id);
    PagedResponse<AiRecommendation> findAll(int page, int size, RecommendationStatus status, String sortField, boolean ascending);
}
