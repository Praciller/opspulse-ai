package com.opspulse.ai.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AiRecommendation(
        UUID id,
        String promptVersion,
        String modelProviderName,
        GeneratedBy generatedBy,
        String generatedSummary,
        List<BriefAction> generatedActions,
        List<MessageDraft> generatedMessageDrafts,
        Double confidence,
        RecommendationStatus status,
        String userFeedback,
        Instant createdAt,
        UUID createdBy,
        CreatedByType createdByType,
        UUID organizationId,
        List<UUID> inputRiskEventIds) {

    public AiRecommendation {
        generatedActions = List.copyOf(generatedActions == null ? List.of() : generatedActions);
        generatedMessageDrafts = List.copyOf(generatedMessageDrafts == null ? List.of() : generatedMessageDrafts);
        inputRiskEventIds = List.copyOf(inputRiskEventIds == null ? List.of() : inputRiskEventIds);
    }

    public static AiRecommendation generated(
            UUID id, BriefResponse response, Instant createdAt, UUID createdBy,
            CreatedByType createdByType, UUID organizationId) {
        return new AiRecommendation(id, response.promptVersion(), response.modelProviderName(),
                response.generatedBy(), response.summary(), response.actions(), response.messageDrafts(),
                response.confidence(), RecommendationStatus.GENERATED, null, createdAt, createdBy,
                createdByType, organizationId, response.riskEventIds());
    }

    public AiRecommendation approve() {
        ensureGenerated();
        return withStatus(RecommendationStatus.APPROVED);
    }

    public AiRecommendation reject() {
        ensureGenerated();
        return withStatus(RecommendationStatus.REJECTED);
    }

    public AiRecommendation withFeedback(String feedback) {
        if (status == RecommendationStatus.ARCHIVED) {
            throw new InvalidRecommendationStatusTransitionException(status, status);
        }
        return new AiRecommendation(id, promptVersion, modelProviderName, generatedBy, generatedSummary,
                generatedActions, generatedMessageDrafts, confidence, status, feedback, createdAt, createdBy,
                createdByType, organizationId, inputRiskEventIds);
    }

    public Map<String, Object> snapshot() {
        var result = new LinkedHashMap<String, Object>();
        result.put("id", id.toString());
        result.put("promptVersion", promptVersion);
        result.put("modelProviderName", modelProviderName);
        result.put("generatedBy", generatedBy.name());
        result.put("status", status.name());
        result.put("inputRiskEventIds", inputRiskEventIds.stream().map(UUID::toString).toList());
        result.put("userFeedback", userFeedback);
        return result;
    }

    private void ensureGenerated() {
        if (status != RecommendationStatus.GENERATED) {
            throw new InvalidRecommendationStatusTransitionException(status, RecommendationStatus.APPROVED);
        }
    }

    private AiRecommendation withStatus(RecommendationStatus target) {
        return new AiRecommendation(id, promptVersion, modelProviderName, generatedBy, generatedSummary,
                generatedActions, generatedMessageDrafts, confidence, target, userFeedback, createdAt, createdBy,
                createdByType, organizationId, inputRiskEventIds);
    }
}
