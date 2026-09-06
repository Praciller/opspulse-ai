package com.opspulse.ai.api.dto;

import com.opspulse.ai.domain.MessageDraft;

public record MessageDraftResponse(String type, String to, String subject, String body) {
    public static MessageDraftResponse from(MessageDraft draft) {
        return new MessageDraftResponse(draft.type(), draft.to(), draft.subject(), draft.body());
    }
}
