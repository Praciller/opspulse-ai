package com.opspulse.outbox.application;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class EventConsumerRegistry {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "opspulse.product.created",
            "opspulse.product.updated",
            "opspulse.product.stock.changed",
            "opspulse.order.created",
            "opspulse.order.status.changed",
            "opspulse.order.delayed",
            "opspulse.po.sent",
            "opspulse.po.received",
            "opspulse.supplier.created",
            "opspulse.risk.created",
            "opspulse.risk.resolved",
            "opspulse.brief.generated");

    public void dispatch(String eventType, Map<String, Object> envelope) {
        if (!SUPPORTED_TYPES.contains(eventType)) {
            throw new IllegalArgumentException("Unsupported outbox event type: " + eventType);
        }
        // Phase 3 intentionally acknowledges the hosted in-process delivery boundary.
        // Real consumers are added by the AI/dashboard phases without changing the envelope.
    }
}
