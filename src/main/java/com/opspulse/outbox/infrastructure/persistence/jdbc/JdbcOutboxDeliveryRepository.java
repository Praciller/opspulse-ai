package com.opspulse.outbox.infrastructure.persistence.jdbc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.outbox.application.port.out.OutboxDeliveryRepository;
import com.opspulse.outbox.domain.OutboxDelivery;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOutboxDeliveryRepository implements OutboxDeliveryRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcOutboxDeliveryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<OutboxDelivery> lockNextDue(Instant now) {
        var rows = jdbcTemplate.query(
                """
                select id, aggregate_type, aggregate_id, event_type, payload::text as payload_text,
                       status, retry_count, next_attempt_at, created_at
                  from outbox_events
                 where status in ('NEW', 'RETRY')
                   and next_attempt_at <= ?
                 order by created_at, id
                 limit 1
                 for update skip locked
                """,
                (rs, rowNum) -> map(rs.getObject("id", UUID.class), rs.getString("aggregate_type"),
                        rs.getObject("aggregate_id", UUID.class), rs.getString("event_type"),
                        rs.getString("payload_text"), rs.getString("status"), rs.getInt("retry_count"),
                        rs.getTimestamp("next_attempt_at"), rs.getTimestamp("created_at")),
                Timestamp.from(now));
        return rows.stream().findFirst();
    }

    @Override
    public boolean markProcessedEventIfNew(UUID eventId, Instant processedAt) {
        return jdbcTemplate.update(
                "insert into processed_events(event_id, processed_at) values (?, ?) on conflict (event_id) do nothing",
                eventId, Timestamp.from(processedAt)) == 1;
    }

    @Override
    public void deleteProcessedEvent(UUID eventId) {
        jdbcTemplate.update("delete from processed_events where event_id = ?", eventId);
    }

    @Override
    public void markProcessed(UUID outboxId, Instant processedAt) {
        jdbcTemplate.update(
                "update outbox_events set status='PROCESSED', processed_at=?, error_message=null where id=?",
                Timestamp.from(processedAt), outboxId);
    }

    @Override
    public void markFailure(UUID outboxId, int retryCount, String status, Instant nextAttemptAt, String errorMessage) {
        jdbcTemplate.update(
                "update outbox_events set status=?, retry_count=?, next_attempt_at=?, error_message=? where id=?",
                status, retryCount, Timestamp.from(nextAttemptAt), errorMessage, outboxId);
    }

    @Override
    public void replay(UUID outboxId, Instant nextAttemptAt) {
        jdbcTemplate.update(
                "update outbox_events set status='RETRY', retry_count=0, next_attempt_at=?, error_message=null, processed_at=null where id=? and status='DEAD'",
                Timestamp.from(nextAttemptAt), outboxId);
    }

    @Override
    public void discard(UUID outboxId, String errorMessage) {
        jdbcTemplate.update(
                "update outbox_events set status='DEAD', error_message=?, processed_at=null where id=? and status='DEAD'",
                errorMessage, outboxId);
    }

    @Override
    public Optional<OutboxDelivery> findById(UUID id) {
        var rows = jdbcTemplate.query(
                "select id, aggregate_type, aggregate_id, event_type, payload::text as payload_text, status, retry_count, next_attempt_at, created_at from outbox_events where id=?",
                (rs, rowNum) -> map(rs.getObject("id", UUID.class), rs.getString("aggregate_type"),
                        rs.getObject("aggregate_id", UUID.class), rs.getString("event_type"),
                        rs.getString("payload_text"), rs.getString("status"), rs.getInt("retry_count"),
                        rs.getTimestamp("next_attempt_at"), rs.getTimestamp("created_at")), id);
        return rows.stream().findFirst();
    }

    private OutboxDelivery map(
            UUID id, String aggregateType, UUID aggregateId, String eventType, String payload,
            String status, int retryCount, Timestamp nextAttemptAt, Timestamp createdAt) {
        try {
            return new OutboxDelivery(id, aggregateType, aggregateId, eventType,
                    objectMapper.readValue(payload, MAP_TYPE), status, retryCount,
                    nextAttemptAt.toInstant(), createdAt.toInstant());
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid outbox payload", exception);
        }
    }
}
