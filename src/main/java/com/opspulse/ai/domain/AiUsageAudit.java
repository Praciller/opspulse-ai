package com.opspulse.ai.domain;

import java.time.Instant;
import java.util.UUID;

public record AiUsageAudit(
        UUID id,
        UUID recommendationId,
        String provider,
        String model,
        String promptVersion,
        Integer inputTokenCount,
        Integer outputTokenCount,
        Integer latencyMs,
        boolean success,
        String errorClass,
        String requestId,
        Instant createdAt) {}
