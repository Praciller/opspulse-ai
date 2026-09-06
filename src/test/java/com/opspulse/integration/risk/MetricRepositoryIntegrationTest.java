package com.opspulse.integration.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import com.opspulse.risk.infrastructure.persistence.jdbc.JdbcMetricRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class MetricRepositoryIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void loadsBatchedProductOrderSupplierAggregatesWithoutPerEntityQueries() {
        var now = Instant.parse("2026-07-17T00:00:00Z");
        var actor = UUID.fromString("10000000-0000-0000-0000-000000000003");
        var product = UUID.randomUUID();
        var supplier = UUID.randomUUID();
        var order = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into products(id, sku, name, unit, current_stock, safety_stock, reorder_point, cost, selling_price, active, created_at, updated_at)
                values (?, ?, 'Aggregate Product', 'PCS', 12, 0, 0, 4, 8, true, ?, ?)
                """, product, ("METRIC-" + product).toUpperCase(), java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
        jdbcTemplate.update("""
                insert into inventory_movements(id, product_id, movement_type, quantity, balance_before, balance_after, created_at, created_by)
                values (?, ?, 'OUTBOUND', -14, 26, 12, ?, ?)
                """, UUID.randomUUID(), product, java.sql.Timestamp.from(now.minusSeconds(86400)), actor);
        jdbcTemplate.update("""
                insert into suppliers(id, name, average_lead_time_days, expected_sla_days, active)
                values (?, 'Aggregate Supplier', 4, 4, true)
                """, supplier);
        jdbcTemplate.update("""
                insert into purchase_orders(id, po_number, supplier_id, status, expected_delivery_date, actual_delivery_date)
                values (?, ?, ?, 'RECEIVED', ?, ?), (?, ?, ?, 'RECEIVED', ?, ?)
                """, UUID.randomUUID(), "METRIC-PO-LATE-" + product, supplier,
                LocalDate.ofInstant(now, ZoneOffset.UTC).minusDays(4), LocalDate.ofInstant(now, ZoneOffset.UTC).minusDays(2),
                UUID.randomUUID(), "METRIC-PO-ON-TIME-" + product, supplier,
                LocalDate.ofInstant(now, ZoneOffset.UTC).minusDays(4), LocalDate.ofInstant(now, ZoneOffset.UTC).minusDays(4));
        jdbcTemplate.update("""
                insert into orders(id, order_number, customer_name, status, expected_ship_date, total_amount)
                values (?, ?, 'Aggregate Customer', 'CONFIRMED', ?, 8)
                """, order, "METRIC-ORDER-" + product, LocalDate.ofInstant(now, ZoneOffset.UTC).plusDays(1));
        jdbcTemplate.update("""
                insert into order_items(id, order_id, product_id, quantity, unit_price)
                values (?, ?, ?, 1, 8)
                """, UUID.randomUUID(), order, product);

        var counted = spy(jdbcTemplate);

        var snapshot = new JdbcMetricRepository(counted).load(now);

        var productMetrics = snapshot.products().stream().filter(value -> value.id().equals(product)).findFirst().orElseThrow();
        assertThat(productMetrics.averageDailySales7d()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(productMetrics.averageDailySales30d()).isEqualByComparingTo(new BigDecimal("0.46666666666666666667"));
        assertThat(snapshot.orders().stream().map(value -> value.id())).contains(order);
        var supplierMetrics = snapshot.suppliers().stream().filter(value -> value.id().equals(supplier)).findFirst().orElseThrow();
        assertThat(supplierMetrics.completedDeliveries()).isEqualTo(2);
        assertThat(supplierMetrics.lateDeliveryRatePct()).isEqualByComparingTo(new BigDecimal("50"));
        verify(counted, times(1)).query(anyString(), any(RowMapper.class), any(Object[].class));
        verify(counted, times(2)).query(anyString(), any(RowMapper.class));
    }
}
