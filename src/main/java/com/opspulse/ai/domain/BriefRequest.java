package com.opspulse.ai.domain;

import java.util.List;
import java.util.Locale;

public record BriefRequest(
        String promptVersion,
        List<BriefRisk> risks,
        String tenantContext,
        Locale locale) {

    public BriefRequest {
        if (promptVersion == null || promptVersion.isBlank()) {
            throw new IllegalArgumentException("promptVersion must be present");
        }
        risks = List.copyOf(risks == null ? List.of() : risks);
        tenantContext = tenantContext == null ? "default" : tenantContext;
        locale = locale == null ? Locale.ENGLISH : locale;
    }
}
