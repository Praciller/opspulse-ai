package com.opspulse.risk.domain;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record RiskFinding(
        RiskType riskType,
        RiskSeverity severity,
        RiskEntityType entityType,
        UUID entityId,
        Map<String, Object> sourceMetrics,
        String explanation,
        String recommendedAction) {

    public RiskFinding {
        Objects.requireNonNull(riskType, "riskType must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(entityId, "entityId must not be null");
        sourceMetrics = Map.copyOf(Objects.requireNonNull(sourceMetrics, "sourceMetrics must not be null"));
        if (sourceMetrics.isEmpty() || explanation == null || explanation.isBlank()
                || recommendedAction == null || recommendedAction.isBlank()) {
            throw new IllegalArgumentException("risk finding content is invalid");
        }
    }
}
