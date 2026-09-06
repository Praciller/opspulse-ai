package com.opspulse.outbox.application.port.out;

import java.util.Map;
import java.util.UUID;

public interface CloudEventsEnvelopeFactory {

    Map<String, Object> build(
            UUID eventId,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            Map<String, Object> data);
}
