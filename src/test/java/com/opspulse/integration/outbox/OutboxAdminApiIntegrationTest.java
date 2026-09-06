package com.opspulse.integration.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspulse.integration.support.Phase2ApiIntegrationTestSupport;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = "opspulse.outbox.poll-interval-ms=3600000")
@AutoConfigureMockMvc
class OutboxAdminApiIntegrationTest extends Phase2ApiIntegrationTestSupport {

    @Test
    void onlyAdminCanReplayDeadOutboxRows() throws Exception {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into outbox_events(id, aggregate_type, aggregate_id, event_type, payload, status, retry_count, next_attempt_at, error_message)
                values (?, 'PRODUCT', ?, 'opspulse.unknown.event', '{"specversion":"1.0"}'::jsonb, 'DEAD', 5, ?, 'poison')
                """, id, UUID.randomUUID(), java.sql.Timestamp.from(Instant.now()));
        mockMvc.perform(post("/api/admin/outbox-events/{id}/replay", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login("viewer@opspulse.demo")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/outbox-events/{id}/replay", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login("admin@opspulse.demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETRY"))
                .andExpect(jsonPath("$.retryCount").value(0));
    }

    @Test
    void adminCanDiscardDeadRowsWithoutExposingOrDeletingPayload() throws Exception {
        UUID id = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into outbox_events(id, aggregate_type, aggregate_id, event_type, payload, status, retry_count, next_attempt_at, error_message)
                values (?, 'PRODUCT', ?, 'opspulse.unknown.event', '{"secret":"do-not-return"}'::jsonb, 'DEAD', 5, ?, 'poison')
                """, id, aggregateId, java.sql.Timestamp.from(Instant.now()));
        String token = login("admin@opspulse.demo");

        mockMvc.perform(post("/api/admin/outbox-events/{id}/discard", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-Request-Id", "outbox-discard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEAD"))
                .andExpect(jsonPath("$.payload").doesNotExist());
        assertThat(jdbcTemplate.queryForObject(
                "select payload->>'secret' from outbox_events where id=?", String.class, id))
                .isEqualTo("do-not-return");
        assertThat(jdbcTemplate.queryForObject(
                "select error_message from outbox_events where id=?", String.class, id))
                .isEqualTo("discarded by administrator");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where entity_id=? and action='outbox.discard'",
                Integer.class, id)).isEqualTo(1);
    }

    @Test
    void replayRejectsNonDeadRowsWithConflict() throws Exception {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into outbox_events(id, aggregate_type, aggregate_id, event_type, payload, status, retry_count, next_attempt_at)
                values (?, 'PRODUCT', ?, 'opspulse.product.created', '{"specversion":"1.0"}'::jsonb, 'PROCESSED', 0, ?)
                """, id, UUID.randomUUID(), java.sql.Timestamp.from(Instant.now()));
        mockMvc.perform(post("/api/admin/outbox-events/{id}/replay", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login("admin@opspulse.demo")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUTBOX_NOT_REPLAYABLE"));
    }
}
