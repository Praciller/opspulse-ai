package com.opspulse.ai.domain;

import java.util.List;
import java.util.UUID;

public record BriefResponse(
        String promptVersion,
        String modelProviderName,
        GeneratedBy generatedBy,
        String summary,
        List<BriefAction> actions,
        List<MessageDraft> messageDrafts,
        Double confidence,
        List<UUID> riskEventIds,
        Integer inputTokenCount,
        Integer outputTokenCount) {

    public BriefResponse {
        if (promptVersion == null || promptVersion.isBlank()
                || modelProviderName == null || modelProviderName.isBlank()
                || generatedBy == null || summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("brief response required fields are missing");
        }
        if (summary.length() > 12000 || modelProviderName.length() > 64) {
            throw new IllegalArgumentException("brief response is too long");
        }
        if (confidence != null && (confidence < 0 || confidence > 1)) {
            throw new IllegalArgumentException("confidence must be between 0 and 1");
        }
        actions = List.copyOf(actions == null ? List.of() : actions);
        messageDrafts = List.copyOf(messageDrafts == null ? List.of() : messageDrafts);
        riskEventIds = List.copyOf(riskEventIds == null ? List.of() : riskEventIds);
    }

    public static BriefResponse ruleBased(
            String promptVersion,
            String summary,
            List<BriefAction> actions,
            List<MessageDraft> messageDrafts,
            List<UUID> riskEventIds) {
        return new BriefResponse(promptVersion, "RULE_BASED", GeneratedBy.RULE_BASED,
                summary, actions, messageDrafts, null, riskEventIds, null, null);
    }
}
