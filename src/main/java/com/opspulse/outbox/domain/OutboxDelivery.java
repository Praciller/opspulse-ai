package com.opspulse.outbox.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OutboxDelivery(
        UUID id,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        Map<String, Object> payload,
        String status,
        int retryCount,
        Instant nextAttemptAt,
        Instant createdAt) {}
