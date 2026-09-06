package com.opspulse.outbox.application.port.in;

import java.util.Map;
import java.util.UUID;

public interface OutboxPublisher {

    UUID publish(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            Map<String, Object> data);
}
