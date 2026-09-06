package com.opspulse.unit.risk;

import static org.assertj.core.api.Assertions.assertThat;

import com.opspulse.risk.application.rules.LowMarginRiskRule;
import com.opspulse.risk.application.rules.OrderDelayRiskRule;
import com.opspulse.risk.application.rules.OverstockRiskRule;
import com.opspulse.risk.application.rules.SlowMovingRiskRule;
import com.opspulse.risk.application.rules.StockoutRiskRule;
import com.opspulse.risk.application.rules.SupplierDelayRiskRule;
import com.opspulse.risk.domain.RiskConfig;
import com.opspulse.risk.domain.RiskContext;
import com.opspulse.risk.domain.RiskSeverity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RiskRuleTest {

    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");
    private static final RiskConfig CONFIG = new RiskConfig(30, 90,
            new BigDecimal("20"), new BigDecimal("20"), 2, "0 0 7 * * *");
    private static final UUID PRODUCT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Test
    void stockoutUsesLeadTimeDemandAndCriticalActiveOrderSignal() {
        var product = product(new BigDecimal("0"), new BigDecimal("10"), new BigDecimal("10"),
                new BigDecimal("3"), new BigDecimal("10"), NOW.minusSeconds(86400), true);
        var finding = new StockoutRiskRule().evaluate(context(product)).orElseThrow();
        assertThat(finding.severity()).isEqualTo(RiskSeverity.CRITICAL);
        assertThat(finding.sourceMetrics()).containsEntry("supplierLeadTimeDays", new BigDecimal("3"));
    }

    @Test
    void stockoutSkipsMissingMetricsAndUsesLowBoundaryAtHalfCoverageRatio() {
        var missing = product(new BigDecimal("1"), null, new BigDecimal("10"),
                new BigDecimal("3"), new BigDecimal("10"), NOW.minusSeconds(86400), false);
        assertThat(new StockoutRiskRule().evaluate(context(missing))).isEmpty();

        var boundary = product(new BigDecimal("60"), new BigDecimal("10"), new BigDecimal("10"),
                new BigDecimal("3"), new BigDecimal("10"), NOW.minusSeconds(86400), false);
        assertThat(new StockoutRiskRule().evaluate(context(boundary)).orElseThrow().severity())
                .isEqualTo(RiskSeverity.LOW);
    }

    @Test
    void overstockSkipsZeroDemandProducts() {
        var product = product(new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1"), new BigDecimal("2"), NOW.minusSeconds(86400), false);
        assertThat(new OverstockRiskRule().evaluate(context(product))).isEmpty();
    }

    @Test
    void overstockUsesSharedSeverityBoundaries() {
        var low = product(new BigDecimal("45"), BigDecimal.ZERO, new BigDecimal("1"),
                new BigDecimal("1"), new BigDecimal("2"), NOW, false);
        assertThat(new OverstockRiskRule().evaluate(context(low)).orElseThrow().severity())
                .isEqualTo(RiskSeverity.LOW);
        var critical = product(new BigDecimal("180"), BigDecimal.ZERO, new BigDecimal("1"),
                new BigDecimal("1"), new BigDecimal("2"), NOW, false);
        assertThat(new OverstockRiskRule().evaluate(context(critical)).orElseThrow().severity())
                .isEqualTo(RiskSeverity.CRITICAL);
    }

    @Test
    void slowMovingUsesCreationWhenNoMovementExists() {
        var product = product(new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1"), new BigDecimal("2"), NOW.minusSeconds(31L * 86400), false);
        assertThat(new SlowMovingRiskRule().evaluate(context(product)).orElseThrow().severity())
                .isEqualTo(RiskSeverity.HIGH);
    }

    @Test
    void slowMovingDoesNotTriggerBeforeConfiguredDayBoundary() {
        var product = product(new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1"), new BigDecimal("2"), NOW.minusSeconds(29L * 86400), false);
        assertThat(new SlowMovingRiskRule().evaluate(context(product))).isEmpty();
    }

    @Test
    void orderDelayHasStableBoundarySeverity() {
        var order = new RiskContext.OrderMetrics(UUID.randomUUID(), "ORD-1", "CONFIRMED",
                LocalDate.ofInstant(NOW, ZoneOffset.UTC).plusDays(1));
        var finding = new OrderDelayRiskRule().evaluate(new RiskContext(null, order, null, CONFIG, NOW)).orElseThrow();
        assertThat(finding.severity()).isEqualTo(RiskSeverity.MEDIUM);
    }

    @Test
    void orderDelaySkipsTerminalOrdersAndEscalatesPastDueDates() {
        var shipped = new RiskContext.OrderMetrics(UUID.randomUUID(), "ORD-SHIPPED", "SHIPPED",
                LocalDate.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(new OrderDelayRiskRule().evaluate(new RiskContext(null, shipped, null, CONFIG, NOW))).isEmpty();
        var overdue = new RiskContext.OrderMetrics(UUID.randomUUID(), "ORD-LATE", "CONFIRMED",
                LocalDate.ofInstant(NOW, ZoneOffset.UTC).minusDays(3));
        assertThat(new OrderDelayRiskRule().evaluate(new RiskContext(null, overdue, null, CONFIG, NOW))
                .orElseThrow().severity()).isEqualTo(RiskSeverity.CRITICAL);
    }

    @Test
    void supplierDelayUsesPercentagePoints() {
        var supplier = new RiskContext.SupplierMetrics(UUID.randomUUID(), "Late Supplier",
                new BigDecimal("40"), 5, true);
        assertThat(new SupplierDelayRiskRule().evaluate(new RiskContext(null, null, supplier, CONFIG, NOW))
                .orElseThrow().severity()).isEqualTo(RiskSeverity.CRITICAL);
    }

    @Test
    void supplierDelaySkipsMissingOrIncompleteDeliveryMetrics() {
        var missing = new RiskContext.SupplierMetrics(UUID.randomUUID(), "Unknown", null, 2, true);
        var incomplete = new RiskContext.SupplierMetrics(UUID.randomUUID(), "Incomplete", new BigDecimal("40"), 0, true);
        assertThat(new SupplierDelayRiskRule().evaluate(new RiskContext(null, null, missing, CONFIG, NOW))).isEmpty();
        assertThat(new SupplierDelayRiskRule().evaluate(new RiskContext(null, null, incomplete, CONFIG, NOW))).isEmpty();
    }

    @Test
    void lowMarginFlagsNonPositiveMargin() {
        var product = product(new BigDecimal("1"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("10"), new BigDecimal("5"), NOW.minusSeconds(86400), false);
        assertThat(new LowMarginRiskRule().evaluate(context(product)).orElseThrow().severity())
                .isEqualTo(RiskSeverity.CRITICAL);
    }

    @Test
    void lowMarginUsesTargetWindowBoundaryAndSkipsHealthyMargins() {
        var boundary = product(new BigDecimal("1"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("6"), new BigDecimal("10"), NOW, false);
        assertThat(new LowMarginRiskRule().evaluate(context(boundary)).orElseThrow().severity())
                .isEqualTo(RiskSeverity.LOW);
        var healthy = product(new BigDecimal("1"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("5"), new BigDecimal("10"), NOW, false);
        assertThat(new LowMarginRiskRule().evaluate(context(healthy))).isEmpty();
    }

    @Test
    void ruleOutputIsDeterministicForTheSameContext() {
        var product = product(new BigDecimal("1"), new BigDecimal("10"), new BigDecimal("10"),
                new BigDecimal("10"), new BigDecimal("10"), NOW, false);
        var first = new LowMarginRiskRule().evaluate(context(product)).orElseThrow();
        var second = new LowMarginRiskRule().evaluate(context(product)).orElseThrow();
        assertThat(first).isEqualTo(second);
    }

    @Test
    void healthyProductDoesNotProduceStockoutOrSlowMovingFinding() {
        var product = product(new BigDecimal("100"), new BigDecimal("2"), new BigDecimal("2"),
                new BigDecimal("1"), new BigDecimal("10"), NOW.minusSeconds(86400), false);
        var context = context(product);
        assertThat(new StockoutRiskRule().evaluate(context)).isEmpty();
        assertThat(new SlowMovingRiskRule().evaluate(context)).isEmpty();
    }

    private static RiskContext context(RiskContext.ProductMetrics product) {
        return new RiskContext(product, null, null, CONFIG, NOW);
    }

    private static RiskContext.ProductMetrics product(
            BigDecimal stock,
            BigDecimal avg7,
            BigDecimal avg30,
            BigDecimal cost,
            BigDecimal selling,
            Instant createdAt,
            boolean activeOrder) {
        return new RiskContext.ProductMetrics(PRODUCT_ID, "SKU-1", "Product 1", stock,
                cost, selling, avg7, avg30, new BigDecimal("3"), null, createdAt, true, activeOrder);
    }
}
