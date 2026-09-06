package com.opspulse.ai.domain;

import com.opspulse.risk.domain.RiskEntityType;
import com.opspulse.risk.domain.RiskEvent;
import com.opspulse.risk.domain.RiskSeverity;
import com.opspulse.risk.domain.RiskType;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record BriefRisk(
        UUID id,
        RiskType riskType,
        RiskSeverity severity,
        RiskEntityType entityType,
        UUID entityId,
        Map<String, Object> sourceMetrics,
        String explanation,
        String recommendedAction,
        Instant createdAt) {

    public BriefRisk {
        sourceMetrics = Map.copyOf(new LinkedHashMap<>(sourceMetrics));
    }

    public static BriefRisk from(RiskEvent event) {
        return new BriefRisk(event.id(), event.riskType(), event.severity(), event.entityType(),
                event.entityId(), event.sourceMetrics(), event.explanation(),
                event.recommendedAction(), event.createdAt());
    }
}
