package com.opspulse.outbox.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        Map<String, Object> payload,
        Instant createdAt) {

    public OutboxEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        payload = Map.copyOf(payload);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
