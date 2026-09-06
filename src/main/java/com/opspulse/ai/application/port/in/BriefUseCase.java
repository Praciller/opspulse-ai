package com.opspulse.ai.application.port.in;

import com.opspulse.ai.domain.AiRecommendation;
import com.opspulse.ai.domain.RecommendationStatus;
import com.opspulse.shared.web.PagedResponse;
import java.util.UUID;

public interface BriefUseCase {
    PagedResponse<AiRecommendation> findAll(int page, int size, RecommendationStatus status, String sortField, boolean ascending);
    AiRecommendation findById(UUID id);
    AiRecommendation generate(int topN, String promptVersion, UUID actorUserId, String requestId, String ipAddress);
    AiRecommendation approve(UUID id, UUID actorUserId, String requestId, String ipAddress);
    AiRecommendation reject(UUID id, UUID actorUserId, String requestId, String ipAddress);
    AiRecommendation feedback(UUID id, String feedback, UUID actorUserId, String requestId, String ipAddress);
}
