package com.opspulse.ai.domain;

import java.util.UUID;

public record BriefAction(UUID riskEventId, String action, String priority) {
    public BriefAction {
        if (riskEventId == null || action == null || action.isBlank() || priority == null || priority.isBlank()) {
            throw new IllegalArgumentException("brief action fields must be present");
        }
        if (action.length() > 2000 || priority.length() > 32) {
            throw new IllegalArgumentException("brief action is too long");
        }
    }
}
