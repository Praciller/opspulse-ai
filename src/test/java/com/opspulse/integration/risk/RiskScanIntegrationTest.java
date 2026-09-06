package com.opspulse.integration.risk;

import static org.assertj.core.api.Assertions.assertThat;

import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import com.opspulse.outbox.application.OutboxDeliveryService;
import com.opspulse.risk.application.port.in.RiskUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = "opspulse.outbox.poll-interval-ms=3600000")
@Transactional
class RiskScanIntegrationTest extends PostgresIntegrationTestSupport {

    private static final UUID ACTOR = UUID.fromString("10000000-0000-0000-0000-000000000003");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RiskUseCase risks;

    @Autowired
    private OutboxDeliveryService delivery;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private EntityManager entityManager;

    @Test
    void scanProducesAllSixRulesAndSecondScanDoesNotDuplicateActiveEvents() {
        var now = Instant.parse("2026-07-17T00:00:00Z");
        var supplier = UUID.randomUUID();
        var stockout = UUID.randomUUID();
        var overstock = UUID.randomUUID();
        var order = UUID.randomUUID();
        var receivedPo = UUID.randomUUID();
        var openPo = UUID.randomUUID();
        insertSupplier(supplier);
        insertProduct(stockout, ("RISK-STOCKOUT-" + stockout).toUpperCase(), 0, 10, 5, now.minusSeconds(31L * 86400));
        insertProduct(overstock, ("RISK-OVERSTOCK-" + overstock).toUpperCase(), 100, 9, 10, now.minusSeconds(2L * 86400));
        insertMovement(stockout, -10, now.minusSeconds(86400));
        insertMovement(overstock, -1, now.minusSeconds(86400));
        insertOrder(order, stockout, now);
        insertPurchaseOrder(receivedPo, supplier, stockout, "RECEIVED", now.minusSeconds(30L * 86400));
        insertPurchaseOrder(openPo, supplier, stockout, "SENT", now.minusSeconds(86400));

        var first = risks.scan();
        flush();
        assertThat(first.evaluatedEntities()).isGreaterThanOrEqualTo(4);
        assertThat(first.createdRiskEvents()).isGreaterThanOrEqualTo(6);
        Integer eventCount = jdbcTemplate.queryForObject("select count(*) from risk_events", Integer.class);
        Integer outboxCount = jdbcTemplate.queryForObject(
                "select count(*) from outbox_events where event_type in ('opspulse.risk.created','opspulse.risk.resolved')",
                Integer.class);

        var second = risks.scan();
        flush();
        assertThat(second.createdRiskEvents()).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from risk_events", Integer.class))
                .isEqualTo(eventCount);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from outbox_events where event_type in ('opspulse.risk.created','opspulse.risk.resolved')",
                Integer.class)).isEqualTo(outboxCount);
    }

    @Test
    void scanSystemResolvesClearedRiskAndRecurrenceCreatesANewEvent() {
        var now = Instant.parse("2026-07-17T00:00:00Z");
        var product = UUID.randomUUID();
        insertProduct(product, ("RISK-LIFECYCLE-" + product).toUpperCase(), 1, 10, 10,
                now.minusSeconds(86400));

        double createdBefore = meterRegistry.counter("opspulse.risk.scan.created").count();
        double resolvedBefore = meterRegistry.counter("opspulse.risk.scan.resolved").count();
        long scanTimerBefore = meterRegistry.timer("opspulse.risk.scan.duration").count();

        risks.scan();
        flush();
        UUID firstRisk = jdbcTemplate.queryForObject(
                "select id from risk_events where entity_id=? and risk_type='LOW_MARGIN_RISK' and status='OPEN'",
                UUID.class, product);
        risks.scan();
        flush();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from risk_events where entity_id=? and risk_type='LOW_MARGIN_RISK' and status in ('OPEN','ACKNOWLEDGED')",
                Integer.class, product)).isEqualTo(1);

        jdbcTemplate.update("update products set selling_price=100 where id=?", product);
        risks.scan();
        flush();
        assertThat(jdbcTemplate.queryForObject(
                "select status from risk_events where id=?", String.class, firstRisk)).isEqualTo("RESOLVED");
        assertThat(jdbcTemplate.queryForObject(
                "select resolved_by from risk_events where id=?", UUID.class, firstRisk)).isNull();
        var systemAudit = jdbcTemplate.queryForMap(
                "select actor_user_id, request_id from audit_logs where entity_id=? and action='risk.resolve' order by created_at desc limit 1",
                firstRisk);
        assertThat(systemAudit.get("actor_user_id")).isNull();
        assertThat((String) systemAudit.get("request_id"))
                .startsWith("system-risk-scan-").hasSizeLessThanOrEqualTo(64);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from outbox_events where aggregate_id=? and event_type='opspulse.risk.resolved'",
                Integer.class, product)).isGreaterThanOrEqualTo(1);

        jdbcTemplate.update("update products set selling_price=10 where id=?", product);
        risks.scan();
        flush();
        var recurrence = jdbcTemplate.queryForList(
                "select id, status from risk_events where entity_id=? and risk_type='LOW_MARGIN_RISK' order by created_at",
                product);
        assertThat(recurrence).hasSize(2);
        assertThat(recurrence.getLast().get("id")).isNotEqualTo(firstRisk);
        assertThat(recurrence.getLast().get("status")).isEqualTo("OPEN");
        assertThat(meterRegistry.counter("opspulse.risk.scan.created").count()).isGreaterThan(createdBefore);
        assertThat(meterRegistry.counter("opspulse.risk.scan.resolved").count()).isGreaterThan(resolvedBefore);
        assertThat(meterRegistry.timer("opspulse.risk.scan.duration").count()).isGreaterThan(scanTimerBefore);
    }

    @Test
    void scanToOutboxProcessorFlowPersistsProcessedEventDeduplication() {
        var now = Instant.parse("2026-07-17T00:00:00Z");
        var product = UUID.randomUUID();
        insertProduct(product, ("RISK-E2E-" + product).toUpperCase(), 1, 10, 10,
                now.minusSeconds(86400));

        risks.scan();
        flush();
        UUID outboxId = jdbcTemplate.queryForObject(
                "select id from outbox_events where aggregate_id=? and event_type='opspulse.risk.created' order by created_at desc limit 1",
                UUID.class, product);
        for (int attempt = 0; attempt < 100 && !"PROCESSED".equals(outboxStatus(outboxId)); attempt++) {
            delivery.processOne();
            jdbcTemplate.update("update outbox_events set next_attempt_at=now() where id=? and status in ('NEW','RETRY')",
                    outboxId);
        }
        assertThat(outboxStatus(outboxId)).isEqualTo("PROCESSED");
        UUID cloudEventId = jdbcTemplate.queryForObject(
                "select (payload->>'id')::uuid from outbox_events where id=?", UUID.class, outboxId);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from processed_events where event_id=?", Integer.class, cloudEventId)).isEqualTo(1);
    }

    private String outboxStatus(UUID id) {
        return jdbcTemplate.queryForObject("select status from outbox_events where id=?", String.class, id);
    }

    private void flush() {
        entityManager.flush();
    }

    private void insertSupplier(UUID id) {
        jdbcTemplate.update("""
                insert into suppliers(id, name, contact_info, average_lead_time_days, expected_sla_days, active)
                values (?, ?, '{}'::jsonb, 3, 3, true)
                """, id, "Risk Supplier " + id);
    }

    private void insertProduct(UUID id, String sku, double stock, double cost, double selling, Instant createdAt) {
        jdbcTemplate.update("""
                insert into products(id, sku, name, unit, current_stock, safety_stock, reorder_point, cost, selling_price, active, created_at, updated_at)
                values (?, ?, ?, 'PCS', ?, 0, 0, ?, ?, true, ?, ?)
                """, id, sku, "Risk Product " + id, stock, cost, selling,
                java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(createdAt));
    }

    private void insertMovement(UUID productId, double quantity, Instant createdAt) {
        jdbcTemplate.update("""
                insert into inventory_movements(id, product_id, movement_type, quantity, balance_before, balance_after, created_at, created_by)
                values (?, ?, 'OUTBOUND', ?, 100, 90, ?, ?)
                """, UUID.randomUUID(), productId, quantity, java.sql.Timestamp.from(createdAt), ACTOR);
    }

    private void insertOrder(UUID orderId, UUID productId, Instant now) {
        jdbcTemplate.update("""
                insert into orders(id, order_number, customer_name, status, expected_ship_date, total_amount, created_by, updated_by)
                values (?, ?, 'Risk Customer', 'CONFIRMED', ?, 10, ?, ?)
                """, orderId, "RISK-ORDER-" + orderId,
                LocalDate.ofInstant(now, ZoneOffset.UTC).plusDays(1), ACTOR, ACTOR);
        jdbcTemplate.update("""
                insert into order_items(id, order_id, product_id, quantity, unit_price)
                values (?, ?, ?, 1, 10)
                """, UUID.randomUUID(), orderId, productId);
    }

    private void insertPurchaseOrder(UUID poId, UUID supplierId, UUID productId, String status, Instant date) {
        var expected = LocalDate.ofInstant(date, ZoneOffset.UTC);
        jdbcTemplate.update("""
                insert into purchase_orders(id, po_number, supplier_id, status, expected_delivery_date, actual_delivery_date, created_by, updated_by)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, poId, "RISK-PO-" + poId, supplierId, status, expected,
                "RECEIVED".equals(status) ? expected.plusDays(2) : null, ACTOR, ACTOR);
        jdbcTemplate.update("""
                insert into purchase_order_items(id, purchase_order_id, product_id, quantity, unit_cost)
                values (?, ?, ?, 10, 5)
                """, UUID.randomUUID(), poId, productId);
    }
}
