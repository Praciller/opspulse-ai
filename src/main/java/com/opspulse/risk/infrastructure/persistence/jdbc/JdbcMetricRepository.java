package com.opspulse.risk.infrastructure.persistence.jdbc;

import com.opspulse.risk.application.port.out.MetricRepository;
import com.opspulse.risk.domain.RiskContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMetricRepository implements MetricRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMetricRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RiskMetricsSnapshot load(Instant asOf) {
        var cutoff7 = Timestamp.from(asOf.minusSeconds(7 * 24 * 60 * 60));
        var cutoff30 = Timestamp.from(asOf.minusSeconds(30 * 24 * 60 * 60));
        var products = jdbcTemplate.query(
                """
                with movement_metrics as (
                    select product_id,
                           coalesce(sum(case when movement_type = 'OUTBOUND' and created_at >= ? then -quantity else 0 end) / 7, 0) as avg7,
                           coalesce(sum(case when movement_type = 'OUTBOUND' and created_at >= ? then -quantity else 0 end) / 30, 0) as avg30,
                           max(created_at) as last_movement_at
                      from inventory_movements
                     group by product_id
                ), supplier_metrics as (
                    select poi.product_id,
                           avg(coalesce(s.average_lead_time_days, s.expected_sla_days::numeric)) as lead_time_days
                      from purchase_order_items poi
                      join purchase_orders po on po.id = poi.purchase_order_id
                      join suppliers s on s.id = po.supplier_id
                     where po.status <> 'CANCELLED'
                     group by poi.product_id
                ), active_order_products as (
                    select distinct oi.product_id
                      from order_items oi
                      join orders o on o.id = oi.order_id
                     where o.status not in ('SHIPPED', 'CANCELLED')
                )
                select p.id, p.sku, p.name, p.current_stock, p.cost, p.selling_price,
                       p.created_at, coalesce(mm.avg7, 0) as avg7, coalesce(mm.avg30, 0) as avg30,
                       sm.lead_time_days as lead_time_days, mm.last_movement_at,
                       exists (select 1 from active_order_products aop where aop.product_id = p.id)
                  from products p
                  left join movement_metrics mm on mm.product_id = p.id
                  left join supplier_metrics sm on sm.product_id = p.id
                 where p.active = true
                 order by p.id
                """,
                (rs, rowNum) -> new RiskContext.ProductMetrics(
                        rs.getObject("id", UUID.class),
                        rs.getString("sku"),
                        rs.getString("name"),
                        rs.getBigDecimal("current_stock"),
                        rs.getBigDecimal("cost"),
                        rs.getBigDecimal("selling_price"),
                        rs.getBigDecimal("avg7"),
                        rs.getBigDecimal("avg30"),
                        rs.getBigDecimal("lead_time_days"),
                        timestamp(rs.getTimestamp("last_movement_at")),
                        rs.getTimestamp("created_at").toInstant(),
                        true,
                        rs.getBoolean(12)),
                cutoff7, cutoff30);

        var orders = jdbcTemplate.query(
                "select id, order_number, status, expected_ship_date from orders where status not in ('SHIPPED', 'CANCELLED') order by id",
                (rs, rowNum) -> new RiskContext.OrderMetrics(
                        rs.getObject("id", UUID.class),
                        rs.getString("order_number"),
                        rs.getString("status"),
                        rs.getDate("expected_ship_date").toLocalDate()));

        var suppliers = jdbcTemplate.query(
                """
                select s.id, s.name, s.active,
                       count(po.id) as completed_deliveries,
                       coalesce(sum(case when po.actual_delivery_date > po.expected_delivery_date then 1 else 0 end)::numeric * 100
                                / nullif(count(po.id), 0), 0) as late_rate_pct
                  from suppliers s
                  left join purchase_orders po on po.supplier_id = s.id and po.status = 'RECEIVED'
                 where s.active = true
                 group by s.id, s.name, s.active
                 order by s.id
                """,
                (rs, rowNum) -> new RiskContext.SupplierMetrics(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getBigDecimal("late_rate_pct"),
                        rs.getInt("completed_deliveries"),
                        rs.getBoolean("active")));
        return new RiskMetricsSnapshot(products, orders, suppliers);
    }

    private static Instant timestamp(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
