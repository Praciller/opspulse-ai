package com.opspulse.ai.api.dto;

import com.opspulse.ai.domain.BriefAction;
import java.util.UUID;

public record BriefActionResponse(UUID riskEventId, String action, String priority) {
    public static BriefActionResponse from(BriefAction action) {
        return new BriefActionResponse(action.riskEventId(), action.action(), action.priority());
    }
}
