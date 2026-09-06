package com.opspulse.outbox.application;

import com.opspulse.outbox.application.port.out.OutboxDeliveryRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxDeliveryService {

    private static final int MAX_RETRIES = 5;
    private static final int MAX_ERROR_LENGTH = 500;
    private static final Logger log = LoggerFactory.getLogger(OutboxDeliveryService.class);

    private final OutboxDeliveryRepository repository;
    private final EventConsumerRegistry consumers;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public OutboxDeliveryService(
            OutboxDeliveryRepository repository,
            EventConsumerRegistry consumers,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.consumers = consumers;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public boolean processOne() {
        Instant now = clock.instant();
        var delivery = repository.lockNextDue(now);
        if (delivery.isEmpty()) {
            return false;
        }
        var row = delivery.get();
        UUID eventId = null;
        boolean markerInserted = false;
        try {
            validateEnvelope(row);
            eventId = UUID.fromString(String.valueOf(row.payload().get("id")));
            markerInserted = repository.markProcessedEventIfNew(eventId, now);
            if (markerInserted) {
                consumers.dispatch(row.eventType(), row.payload());
            }
            repository.markProcessed(row.id(), now);
            meterRegistry.counter("opspulse.outbox.processed").increment();
            meterRegistry.timer("opspulse.outbox.delivery.lag")
                    .record(Duration.between(row.createdAt(), now).isNegative()
                            ? Duration.ZERO : Duration.between(row.createdAt(), now));
            log.info("outbox delivery processed id={} eventType={} duplicate={}",
                    row.id(), row.eventType(), !markerInserted);
        } catch (Exception exception) {
            if (markerInserted && eventId != null) {
                repository.deleteProcessedEvent(eventId);
            }
            int retryCount = row.retryCount() + 1;
            String status = retryCount >= MAX_RETRIES ? "DEAD" : "RETRY";
            long backoffSeconds = 1L << Math.max(0, retryCount - 1);
            repository.markFailure(row.id(), retryCount, status,
                    now.plusSeconds(backoffSeconds), sanitize(exception));
            meterRegistry.counter("opspulse.outbox." + status.toLowerCase()).increment();
            log.warn("outbox delivery failed id={} eventType={} status={} retryCount={} error={}",
                    row.id(), row.eventType(), status, retryCount, sanitize(exception));
        }
        return true;
    }

    private static void validateEnvelope(com.opspulse.outbox.domain.OutboxDelivery row) {
        var payload = row.payload();
        if (!"1.0".equals(payload.get("specversion"))
                || payload.get("source") == null
                || String.valueOf(payload.get("source")).isBlank()
                || payload.get("id") == null
                || !row.eventType().equals(payload.get("type"))
                || !(payload.get("data") instanceof java.util.Map<?, ?>)) {
            throw new IllegalArgumentException("Invalid CloudEvents envelope");
        }
        UUID.fromString(String.valueOf(payload.get("id")));
    }

    private static String sanitize(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        message = message.replaceAll("[\\r\\n\\t]", " ").trim();
        message = message.replaceAll(
                "(?i)(password|token|secret|api[_-]?key)\\s*[:=]\\s*[^,;\\s]+",
                "$1=[REDACTED]");
        return message.length() <= MAX_ERROR_LENGTH
                ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
