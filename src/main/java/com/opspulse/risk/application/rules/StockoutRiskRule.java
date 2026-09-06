package com.opspulse.risk.application.rules;

import com.opspulse.risk.application.port.in.RiskRule;
import com.opspulse.risk.domain.RiskContext;
import com.opspulse.risk.domain.RiskEntityType;
import com.opspulse.risk.domain.RiskFinding;
import com.opspulse.risk.domain.RiskSeverity;
import com.opspulse.risk.domain.RiskType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class StockoutRiskRule implements RiskRule {

    @Override
    public RiskType type() {
        return RiskType.STOCKOUT_RISK;
    }

    @Override
    public Optional<RiskFinding> evaluate(RiskContext context) {
        var product = context.product();
        if (product == null || !product.active() || product.averageDailySales7d() == null
                || product.averageDailySales7d().signum() <= 0 || product.supplierLeadTimeDays() == null
                || product.supplierLeadTimeDays().signum() <= 0) {
            return Optional.empty();
        }
        var requiredStock = product.averageDailySales7d().multiply(product.supplierLeadTimeDays());
        var ratio = product.currentStock().signum() <= 0
                ? new BigDecimal("2")
                : RuleSupport.ratio(requiredStock, product.currentStock());
        if (ratio.compareTo(new BigDecimal("0.5")) < 0) {
            return Optional.empty();
        }
        var severity = RuleSupport.severity(ratio);
        if (product.activeOrderConsuming() && product.currentStock().signum() <= 0) {
            severity = RiskSeverity.CRITICAL;
        }
        var metrics = RuleSupport.metrics("1", product.currentStock(), requiredStock, ratio,
                context.evaluatedAt().toString(), Map.of(
                        "sku", product.sku(),
                        "averageDailySales7d", product.averageDailySales7d(),
                        "supplierLeadTimeDays", product.supplierLeadTimeDays(),
                        "activeOrderConsuming", product.activeOrderConsuming()));
        return Optional.of(new RiskFinding(type(), severity, RiskEntityType.PRODUCT, product.id(), metrics,
                "Stock coverage for " + product.sku() + " is below the supplier lead-time demand window.",
                "Reorder " + product.sku() + " before the next supplier lead-time window."));
    }
}
