package com.opspulse.risk.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record RiskEvent(
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
        UUID resolvedBy,
        UUID organizationId) {

    public RiskEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(riskType, "riskType must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(entityId, "entityId must not be null");
        sourceMetrics = sourceMetrics == null ? Map.of() : Map.copyOf(sourceMetrics);
        if (sourceMetrics.isEmpty()) {
            throw new IllegalArgumentException("sourceMetrics must not be empty");
        }
        explanation = required(explanation, "explanation");
        recommendedAction = required(recommendedAction, "recommendedAction");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (status == RiskStatus.OPEN || status == RiskStatus.ACKNOWLEDGED) {
            if (resolvedAt != null || resolvedBy != null) {
                throw new IllegalArgumentException("active risk must not have resolution metadata");
            }
        } else if (resolvedAt == null) {
            throw new IllegalArgumentException("terminal risk must have resolvedAt");
        }
    }

    public static RiskEvent open(
            UUID id,
            RiskType riskType,
            RiskSeverity severity,
            RiskEntityType entityType,
            UUID entityId,
            Map<String, Object> sourceMetrics,
            String explanation,
            String recommendedAction,
            UUID organizationId,
            Instant now) {
        return new RiskEvent(id, riskType, severity, entityType, entityId, sourceMetrics,
                explanation, recommendedAction, RiskStatus.OPEN, now, null, null, organizationId);
    }

    public String dedupKey() {
        return riskType.name() + ":" + entityType.name() + ":" + entityId;
    }

    public RiskEvent refresh(
            RiskSeverity nextSeverity,
            Map<String, Object> nextMetrics,
            String nextExplanation,
            String nextAction) {
        if (status != RiskStatus.OPEN && status != RiskStatus.ACKNOWLEDGED) {
            throw new InvalidRiskStatusTransitionException("Only active risks can be refreshed");
        }
        return new RiskEvent(id, riskType, nextSeverity, entityType, entityId, nextMetrics,
                nextExplanation, nextAction, status, createdAt, null, null, organizationId);
    }

    public RiskEvent acknowledge() {
        if (status != RiskStatus.OPEN) {
            throw new InvalidRiskStatusTransitionException("Risk can only be acknowledged from OPEN");
        }
        return new RiskEvent(id, riskType, severity, entityType, entityId, sourceMetrics,
                explanation, recommendedAction, RiskStatus.ACKNOWLEDGED, createdAt, null, null, organizationId);
    }

    public RiskEvent resolve(Instant now, UUID actor) {
        if (status != RiskStatus.OPEN && status != RiskStatus.ACKNOWLEDGED) {
            throw new InvalidRiskStatusTransitionException("Risk can only be resolved while active");
        }
        return terminal(RiskStatus.RESOLVED, now, actor);
    }

    public RiskEvent dismiss(Instant now, UUID actor) {
        if (status != RiskStatus.OPEN && status != RiskStatus.ACKNOWLEDGED) {
            throw new InvalidRiskStatusTransitionException("Risk can only be dismissed while active");
        }
        return terminal(RiskStatus.DISMISSED, now, actor);
    }

    public RiskEvent systemResolve(Instant now) {
        if (status != RiskStatus.OPEN && status != RiskStatus.ACKNOWLEDGED) {
            throw new InvalidRiskStatusTransitionException("Risk can only be system-resolved while active");
        }
        return terminal(RiskStatus.RESOLVED, now, null);
    }

    public Map<String, Object> snapshot() {
        var value = new LinkedHashMap<String, Object>();
        value.put("riskType", riskType.name());
        value.put("severity", severity.name());
        value.put("entityType", entityType.name());
        value.put("entityId", entityId.toString());
        value.put("sourceMetrics", sourceMetrics);
        value.put("explanation", explanation);
        value.put("recommendedAction", recommendedAction);
        value.put("status", status.name());
        value.put("createdAt", createdAt.toString());
        value.put("resolvedAt", resolvedAt == null ? null : resolvedAt.toString());
        value.put("resolvedBy", resolvedBy == null ? null : resolvedBy.toString());
        return value;
    }

    private RiskEvent terminal(RiskStatus nextStatus, Instant now, UUID actor) {
        return new RiskEvent(id, riskType, severity, entityType, entityId, sourceMetrics,
                explanation, recommendedAction, nextStatus, createdAt, now, actor, organizationId);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
