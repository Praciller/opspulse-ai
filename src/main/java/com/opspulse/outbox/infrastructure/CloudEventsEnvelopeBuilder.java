package com.opspulse.outbox.infrastructure;

import com.opspulse.outbox.application.port.out.CloudEventsEnvelopeFactory;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CloudEventsEnvelopeBuilder implements CloudEventsEnvelopeFactory {

    private final Clock clock;

    public CloudEventsEnvelopeBuilder(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Map<String, Object> build(
            UUID eventId,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            Map<String, Object> data) {
        String resource = resourceName(aggregateType);
        var envelope = new LinkedHashMap<String, Object>();
        envelope.put("specversion", "1.0");
        envelope.put("id", eventId.toString());
        envelope.put("source", "/opspulse/" + resource);
        envelope.put("type", eventType);
        envelope.put("subject", subjectName(aggregateType) + "/" + aggregateId);
        envelope.put("time", clock.instant().toString());
        envelope.put("datacontenttype", "application/json");
        envelope.put("data", Map.copyOf(data));
        return Map.copyOf(envelope);
    }

    private static String resourceName(String aggregateType) {
        return switch (aggregateType) {
            case "PRODUCT" -> "products";
            case "SUPPLIER" -> "suppliers";
            case "ORDER" -> "orders";
            case "PURCHASE_ORDER" -> "purchase-orders";
            case "RISK" -> "risk";
            case "AI_RECOMMENDATION" -> "ai";
            default -> throw new IllegalArgumentException("Unsupported aggregate type");
        };
    }

    private static String subjectName(String aggregateType) {
        return aggregateType.toLowerCase().replace('_', '-');
    }
}
