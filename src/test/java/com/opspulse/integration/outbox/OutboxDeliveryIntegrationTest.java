package com.opspulse.integration.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import com.opspulse.outbox.application.OutboxDeliveryService;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = "opspulse.outbox.poll-interval-ms=3600000")
class OutboxDeliveryIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxDeliveryService delivery;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void resetOutboxFixture() {
        jdbcTemplate.update("delete from processed_events");
        jdbcTemplate.update("delete from outbox_events");
    }

    @Test
    void validCloudEventMovesToProcessedAndDuplicateCloudEventIdIsSkipped() throws Exception {
        double processedBefore = meterRegistry.counter("opspulse.outbox.processed").count();
        long lagTimerBefore = meterRegistry.timer("opspulse.outbox.delivery.lag").count();
        var eventId = UUID.randomUUID();
        var firstRow = UUID.randomUUID();
        insert(firstRow, eventId, "opspulse.product.created");
        processUntil(firstRow);
        assertThat(status(firstRow)).isEqualTo("PROCESSED");
        assertThat(countProcessed(eventId)).isEqualTo(1);

        var duplicateRow = UUID.randomUUID();
        insert(duplicateRow, eventId, "opspulse.product.created");
        processUntil(duplicateRow);
        assertThat(status(duplicateRow)).isEqualTo("PROCESSED");
        assertThat(countProcessed(eventId)).isEqualTo(1);
        assertThat(meterRegistry.counter("opspulse.outbox.processed").count()).isGreaterThan(processedBefore);
        assertThat(meterRegistry.timer("opspulse.outbox.delivery.lag").count()).isGreaterThan(lagTimerBefore);
    }

    @Test
    void unsupportedEventReachesDeadAfterFiveAttemptsWithSanitizedError() throws Exception {
        var row = UUID.randomUUID();
        insert(row, UUID.randomUUID(), "opspulse.unknown.event");
        for (int attempt = 0; attempt < 100 && !"DEAD".equals(status(row)); attempt++) {
            delivery.processOne();
            jdbcTemplate.update("update outbox_events set next_attempt_at=now() where id=?", row);
        }
        assertThat(status(row)).isEqualTo("DEAD");
        Integer retryCount = jdbcTemplate.queryForObject("select retry_count from outbox_events where id=?", Integer.class, row);
        String error = jdbcTemplate.queryForObject("select error_message from outbox_events where id=?", String.class, row);
        assertThat(retryCount).isEqualTo(5);
        assertThat(error).contains("Unsupported outbox event type").doesNotContain("\n");
    }

    @Test
    void retriesFollowOneTwoFourEightSixteenSecondBackoffAndRedactSecrets() throws Exception {
        var row = UUID.randomUUID();
        insert(row, UUID.randomUUID(), "opspulse.password=supersecret");
        long[] delays = {1, 2, 4, 8, 16};
        double retryBefore = meterRegistry.counter("opspulse.outbox.retry").count();
        double deadBefore = meterRegistry.counter("opspulse.outbox.dead").count();

        for (int attempt = 0; attempt < delays.length; attempt++) {
            jdbcTemplate.update("update outbox_events set next_attempt_at=now() - interval '1 hour' where id=?", row);
            Instant before = Instant.now();
            delivery.processOne();
            Instant nextAttempt = jdbcTemplate.queryForObject(
                    "select next_attempt_at from outbox_events where id=?", Instant.class, row);
            Duration scheduledDelay = Duration.between(before, nextAttempt);
            assertThat(scheduledDelay).isBetween(
                    Duration.ofSeconds(delays[attempt] - 1), Duration.ofSeconds(delays[attempt] + 3));
            if (attempt < delays.length - 1) {
                assertThat(status(row)).isEqualTo("RETRY");
            } else {
                assertThat(status(row)).isEqualTo("DEAD");
            }
        }
        String error = jdbcTemplate.queryForObject(
                "select error_message from outbox_events where id=?", String.class, row);
        assertThat(error).doesNotContain("supersecret").contains("[REDACTED]").doesNotContain("\n");
        assertThat(error.length()).isLessThanOrEqualTo(500);
        assertThat(meterRegistry.counter("opspulse.outbox.retry").count()).isGreaterThan(retryBefore);
        assertThat(meterRegistry.counter("opspulse.outbox.dead").count()).isGreaterThan(deadBefore);
    }

    @Test
    void malformedCloudEventsRetryWithoutCreatingAProcessedMarkerAndDoNotBlockNextRow() throws Exception {
        var malformed = UUID.randomUUID();
        insert(malformed, UUID.randomUUID(), "opspulse.product.created", false);
        var valid = UUID.randomUUID();
        var validEventId = UUID.randomUUID();
        insert(valid, validEventId, "opspulse.product.created");
        jdbcTemplate.update("update outbox_events set next_attempt_at=now() + interval '1 hour' where id=?", valid);

        delivery.processOne();
        assertThat(status(malformed)).isEqualTo("RETRY");
        assertThat(countProcessed(UUID.fromString(
                jdbcTemplate.queryForObject("select payload->>'id' from outbox_events where id=?", String.class, malformed))))
                .isZero();

        jdbcTemplate.update("update outbox_events set next_attempt_at=now() where id=?", valid);
        for (int attempt = 0; attempt < 3 && !"PROCESSED".equals(status(valid)); attempt++) {
            delivery.processOne();
            jdbcTemplate.update("update outbox_events set next_attempt_at=now() where id=? and status in ('NEW','RETRY')", valid);
        }
        assertThat(status(valid)).isEqualTo("PROCESSED");
        assertThat(countProcessed(validEventId)).isEqualTo(1);
    }

    private void processUntil(UUID row) {
        for (int i = 0; i < 100 && !"PROCESSED".equals(status(row)); i++) {
            delivery.processOne();
            jdbcTemplate.update("update outbox_events set next_attempt_at=now() where id=? and status in ('NEW','RETRY')", row);
        }
    }

    private void insert(UUID rowId, UUID eventId, String eventType) throws Exception {
        insert(rowId, eventId, eventType, true);
    }

    private void insert(UUID rowId, UUID eventId, String eventType, boolean includeSource) throws Exception {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("specversion", "1.0");
        if (includeSource) {
            payload.put("source", "/opspulse/products");
        }
        payload.put("id", eventId.toString());
        payload.put("type", eventType);
        payload.put("subject", "product/" + UUID.randomUUID());
        payload.put("time", Instant.now().toString());
        payload.put("datacontenttype", "application/json");
        payload.put("data", Map.of("id", UUID.randomUUID().toString()));
        var serialized = payload;
        jdbcTemplate.update("""
                insert into outbox_events(id, aggregate_type, aggregate_id, event_type, payload, status, retry_count, next_attempt_at, created_at)
                values (?, 'PRODUCT', ?, ?, ?::jsonb, 'NEW', 0, now(), now())
                """, rowId, UUID.randomUUID(), eventType, objectMapper.writeValueAsString(serialized));
    }

    private String status(UUID row) {
        return jdbcTemplate.queryForObject("select status from outbox_events where id=?", String.class, row);
    }

    private int countProcessed(UUID eventId) {
        return jdbcTemplate.queryForObject("select count(*) from processed_events where event_id=?", Integer.class, eventId);
    }
}
