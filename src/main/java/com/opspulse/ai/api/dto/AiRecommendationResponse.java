package com.opspulse.ai.api.dto;

import com.opspulse.ai.domain.AiRecommendation;
import com.opspulse.ai.domain.CreatedByType;
import com.opspulse.ai.domain.GeneratedBy;
import com.opspulse.ai.domain.RecommendationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiRecommendationResponse(
        UUID id,
        String promptVersion,
        String modelProviderName,
        GeneratedBy generatedBy,
        String summary,
        List<BriefActionResponse> actions,
        List<MessageDraftResponse> messageDrafts,
        Double confidence,
        List<UUID> inputRiskEventIds,
        Instant createdAt,
        RecommendationStatus status,
        String userFeedback,
        UUID createdBy,
        CreatedByType createdByType) {

    public static AiRecommendationResponse from(AiRecommendation recommendation) {
        return new AiRecommendationResponse(recommendation.id(), recommendation.promptVersion(),
                recommendation.modelProviderName(), recommendation.generatedBy(), recommendation.generatedSummary(),
                recommendation.generatedActions().stream().map(BriefActionResponse::from).toList(),
                recommendation.generatedMessageDrafts().stream().map(MessageDraftResponse::from).toList(),
                recommendation.confidence(), recommendation.inputRiskEventIds(), recommendation.createdAt(),
                recommendation.status(), recommendation.userFeedback(), recommendation.createdBy(),
                recommendation.createdByType());
    }
}
