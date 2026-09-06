package com.opspulse.audit.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record AuditEvent(
        UUID id,
        UUID actorUserId,
        String action,
        String entityType,
        UUID entityId,
        Map<String, Object> beforeSnapshot,
        Map<String, Object> afterSnapshot,
        String requestId,
        String ipAddress,
        Instant createdAt) {

    public static final String LOGIN_SUCCESS = "auth.login.success";
    public static final String LOGIN_FAILURE = "auth.login.failure";
    public static final String USER_REGISTERED = "auth.register";
    public static final String USER_ENTITY = "USER";

    public AuditEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        beforeSnapshot = immutableSnapshot(beforeSnapshot);
        afterSnapshot = immutableSnapshot(afterSnapshot);
    }

    public AuditEvent(
            UUID id,
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            String requestId,
            String ipAddress,
            Instant createdAt) {
        this(
                id,
                actorUserId,
                action,
                entityType,
                entityId,
                null,
                null,
                requestId,
                ipAddress,
                createdAt);
    }

    private static Map<String, Object> immutableSnapshot(Map<String, Object> snapshot) {
        return snapshot == null
                ? null
                : Collections.unmodifiableMap(new LinkedHashMap<>(snapshot));
    }
}
