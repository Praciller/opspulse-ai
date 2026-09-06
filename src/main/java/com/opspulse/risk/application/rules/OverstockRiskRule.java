package com.opspulse.risk.application.rules;

import com.opspulse.risk.application.port.in.RiskRule;
import com.opspulse.risk.domain.RiskContext;
import com.opspulse.risk.domain.RiskEntityType;
import com.opspulse.risk.domain.RiskFinding;
import com.opspulse.risk.domain.RiskType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OverstockRiskRule implements RiskRule {

    @Override
    public RiskType type() {
        return RiskType.OVERSTOCK_RISK;
    }

    @Override
    public Optional<RiskFinding> evaluate(RiskContext context) {
        var product = context.product();
        if (product == null || !product.active() || product.currentStock().signum() <= 0
                || product.averageDailySales30d() == null || product.averageDailySales30d().signum() <= 0) {
            return Optional.empty();
        }
        var threshold = product.averageDailySales30d()
                .multiply(BigDecimal.valueOf(context.config().excessiveDaysThreshold()));
        var ratio = RuleSupport.ratio(product.currentStock(), threshold);
        if (ratio.compareTo(new BigDecimal("0.5")) < 0) {
            return Optional.empty();
        }
        var metrics = RuleSupport.metrics("1", product.currentStock(), threshold, ratio,
                context.evaluatedAt().toString(), Map.of(
                        "sku", product.sku(),
                        "averageDailySales30d", product.averageDailySales30d(),
                        "excessiveDaysThreshold", context.config().excessiveDaysThreshold()));
        return Optional.of(new RiskFinding(type(), RuleSupport.severity(ratio), RiskEntityType.PRODUCT,
                product.id(), metrics,
                "Stock for " + product.sku() + " exceeds the expected demand horizon.",
                "Review purchasing and promotion plans for excess " + product.sku() + " stock."));
    }
}
