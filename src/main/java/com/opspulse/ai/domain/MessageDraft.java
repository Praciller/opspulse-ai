package com.opspulse.ai.domain;

public record MessageDraft(String type, String to, String subject, String body) {
    public MessageDraft {
        if (type == null || type.isBlank() || body == null || body.isBlank()) {
            throw new IllegalArgumentException("message draft type and body must be present");
        }
        if (type.length() > 64 || (to != null && to.length() > 320)
                || (subject != null && subject.length() > 300) || body.length() > 4000) {
            throw new IllegalArgumentException("message draft is too long");
        }
    }
}
