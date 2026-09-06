package com.opspulse.audit.api.dto;

import com.opspulse.audit.domain.AuditEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorUserId,
        String action,
        String entityType,
        UUID entityId,
        Map<String, Object> beforeSnapshot,
        Map<String, Object> afterSnapshot,
        String requestId,
        Instant createdAt) {

    public static AuditLogResponse from(AuditEvent event) {
        return new AuditLogResponse(
                event.id(),
                event.actorUserId(),
                event.action(),
                event.entityType(),
                event.entityId(),
                event.beforeSnapshot(),
                event.afterSnapshot(),
                event.requestId(),
                event.createdAt());
    }
}
