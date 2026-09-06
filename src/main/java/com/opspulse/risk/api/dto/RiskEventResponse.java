package com.opspulse.risk.api.dto;

import com.opspulse.risk.domain.RiskEvent;
import com.opspulse.risk.domain.RiskEntityType;
import com.opspulse.risk.domain.RiskSeverity;
import com.opspulse.risk.domain.RiskStatus;
import com.opspulse.risk.domain.RiskType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RiskEventResponse(
        UUID id,
        RiskType riskType,
        RiskSeverity severity,
        RiskEntityType entityType,
        UUID entityId,
        Map<String, Object> sourceMetrics,
        String explanation,
        String recommendedAction,
        RiskStatus status,
        Instant createdAt,
        Instant resolvedAt,
        UUID resolvedBy) {

    public static RiskEventResponse from(RiskEvent event) {
        return new RiskEventResponse(event.id(), event.riskType(), event.severity(), event.entityType(),
                event.entityId(), event.sourceMetrics(), event.explanation(), event.recommendedAction(),
                event.status(), event.createdAt(), event.resolvedAt(), event.resolvedBy());
    }
}
