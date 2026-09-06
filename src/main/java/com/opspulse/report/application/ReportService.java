package com.opspulse.report.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.report.domain.ReportModels;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ReportService(JdbcTemplate jdbc, ObjectMapper objectMapper, Clock clock) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ReportModels.DailyOpsBriefReport dailyOpsBrief() {
        Instant asOf = clock.instant();
        var rows = jdbc.query("""
                select id, generated_by, status, generated_summary, generated_actions::text actions,
                       generated_message_drafts::text message_drafts, created_at
                from ai_recommendations order by created_at desc limit 1
                """, this::mapBrief);
        if (rows.isEmpty()) return new ReportModels.DailyOpsBriefReport(null, null, null, null, "[]", "[]", null, asOf);
        var value = rows.getFirst();
        return new ReportModels.DailyOpsBriefReport(value.id(), value.generatedBy(), value.status(), value.summary(),
                value.actionsJson(), value.messageDraftsJson(), value.generatedAt(), asOf);
    }

    @Transactional(readOnly = true)
    public ReportModels.InventoryRiskReport inventoryRisk() {
        Instant asOf = clock.instant();
        Map<String, Long> severity = grouped("select severity, count(*) from risk_events where status in ('OPEN','ACKNOWLEDGED') group by severity order by severity", "severity");
        Map<String, Long> types = grouped("select risk_type, count(*) from risk_events where status in ('OPEN','ACKNOWLEDGED') group by risk_type order by risk_type", "risk_type");
        var top = jdbc.query("""
                select id, risk_type, severity, entity_type, entity_id, explanation, recommended_action, created_at
                from risk_events where status in ('OPEN','ACKNOWLEDGED')
                order by case severity when 'CRITICAL' then 4 when 'HIGH' then 3 when 'MEDIUM' then 2 else 1 end desc,
                         created_at desc limit 20
                """, (rs, row) -> new ReportModels.RiskRow(rs.getObject("id", UUID.class), rs.getString("risk_type"),
                rs.getString("severity"), rs.getString("entity_type"), rs.getObject("entity_id", UUID.class),
                rs.getString("explanation"), rs.getString("recommended_action"), rs.getTimestamp("created_at").toInstant()));
        return new ReportModels.InventoryRiskReport(asOf, severity, types, top);
    }

    @Transactional(readOnly = true)
    public ReportModels.SupplierSlaReport supplierSla() {
        Instant asOf = clock.instant();
        var rows = jdbc.query("""
                select s.id, s.name,
                       count(po.id) filter (where po.status = 'RECEIVED') received_count,
                       count(po.id) filter (where po.status = 'RECEIVED' and po.actual_delivery_date > po.expected_delivery_date) late_count,
                       avg((po.actual_delivery_date - po.expected_delivery_date)::numeric)
                           filter (where po.status = 'RECEIVED') avg_lead
                from suppliers s left join purchase_orders po on po.supplier_id = s.id
                where s.active = true group by s.id, s.name order by s.name
                """, (rs, row) -> {
            long received = rs.getLong("received_count");
            long late = rs.getLong("late_count");
            BigDecimal rate = received == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(late * 100.0 / received).setScale(2, RoundingMode.HALF_UP);
            BigDecimal avg = rs.getBigDecimal("avg_lead");
            return new ReportModels.SupplierSlaRow(rs.getObject("id", UUID.class), rs.getString("name"), received, late, rate,
                    avg == null ? BigDecimal.ZERO : avg.setScale(2, RoundingMode.HALF_UP));
        });
        return new ReportModels.SupplierSlaReport(asOf, rows);
    }

    @Transactional(readOnly = true)
    public ReportModels.OrderDelayReport orderDelay() {
        Instant asOf = clock.instant();
        Map<String, Long> byStatus = grouped("select status, count(*) from orders group by status order by status", "status");
        LocalDate today = LocalDate.now(clock);
        var rows = jdbc.query("""
                select id, order_number, customer_name, expected_ship_date, status
                from orders where status not in ('SHIPPED','CANCELLED') and expected_ship_date <= ?
                order by expected_ship_date asc, order_number asc limit 100
                """, (rs, row) -> new ReportModels.DelayedOrderRow(rs.getObject("id", UUID.class), rs.getString("order_number"),
                rs.getString("customer_name"), rs.getObject("expected_ship_date", LocalDate.class),
                ChronoUnit.DAYS.between(rs.getObject("expected_ship_date", LocalDate.class), today), rs.getString("status")), today);
        return new ReportModels.OrderDelayReport(asOf, byStatus, rows);
    }

    @Transactional(readOnly = true)
    public ReportModels.ProductMarginReport productMargin() {
        Instant asOf = clock.instant();
        BigDecimal threshold = jdbc.query("select value::text from app_config where key = 'lowMarginThresholdPct'",
                rs -> rs.next() ? scalarDecimal(rs.getString(1), BigDecimal.valueOf(20)) : BigDecimal.valueOf(20));
        var rows = jdbc.query("""
                select id, sku, name, cost, selling_price, (selling_price - cost) margin_amount
                from products where active = true order by sku
                """, (rs, row) -> {
            BigDecimal cost = rs.getBigDecimal("cost");
            BigDecimal price = rs.getBigDecimal("selling_price");
            BigDecimal margin = rs.getBigDecimal("margin_amount");
            BigDecimal pct = price.signum() <= 0 ? null : margin.multiply(BigDecimal.valueOf(100)).divide(price, 2, RoundingMode.HALF_UP);
            String risk = marginRisk(price, margin, pct, threshold);
            return new ReportModels.ProductMarginRow(rs.getObject("id", UUID.class), rs.getString("sku"), rs.getString("name"),
                    cost, price, margin, pct, risk);
        });
        long lowMargin = rows.stream().filter(row -> !"NONE".equals(row.riskLevel())).count();
        return new ReportModels.ProductMarginReport(asOf, lowMargin, rows);
    }

    private static String marginRisk(BigDecimal price, BigDecimal margin, BigDecimal marginPct,
                                     BigDecimal threshold) {
        if (price.signum() <= 0 || margin.signum() <= 0) return "CRITICAL";
        if (threshold.signum() <= 0) return "NONE";
        BigDecimal ratio = threshold.divide(marginPct, 8, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.5")) < 0) return "NONE";
        if (ratio.compareTo(new BigDecimal("0.667")) < 0) return "LOW";
        if (ratio.compareTo(BigDecimal.ONE) < 0) return "MEDIUM";
        if (ratio.compareTo(BigDecimal.valueOf(2)) < 0) return "HIGH";
        return "CRITICAL";
    }

    private Map<String, Long> grouped(String sql, String keyColumn) {
        var result = new LinkedHashMap<String, Long>();
        jdbc.query(sql, rs -> { result.put(rs.getString(keyColumn), rs.getLong(2)); });
        return result;
    }

    private BriefRow mapBrief(ResultSet rs, int row) throws SQLException {
        return new BriefRow(rs.getObject("id", UUID.class), rs.getString("generated_by"), rs.getString("status"),
                rs.getString("generated_summary"), rs.getString("actions"), rs.getString("message_drafts"),
                rs.getTimestamp("created_at").toInstant());
    }

    private BigDecimal scalarDecimal(String value, BigDecimal fallback) {
        try {
            return new BigDecimal(objectMapper.readTree(value).asText());
        } catch (Exception exception) {
            return fallback;
        }
    }

    private record BriefRow(UUID id, String generatedBy, String status, String summary, String actionsJson,
                            String messageDraftsJson, Instant generatedAt) {}
}
