package com.opspulse.risk.application.rules;

import com.opspulse.risk.application.port.in.RiskRule;
import com.opspulse.risk.domain.RiskContext;
import com.opspulse.risk.domain.RiskEntityType;
import com.opspulse.risk.domain.RiskFinding;
import com.opspulse.risk.domain.RiskSeverity;
import com.opspulse.risk.domain.RiskType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class LowMarginRiskRule implements RiskRule {

    @Override
    public RiskType type() {
        return RiskType.LOW_MARGIN_RISK;
    }

    @Override
    public Optional<RiskFinding> evaluate(RiskContext context) {
        var product = context.product();
        if (product == null || !product.active()) {
            return Optional.empty();
        }
        var margin = product.sellingPrice().subtract(product.cost());
        BigDecimal marginPct;
        RiskSeverity severity = null;
        if (product.sellingPrice().signum() <= 0) {
            marginPct = BigDecimal.ZERO;
            severity = RiskSeverity.CRITICAL;
        } else {
            marginPct = margin.multiply(BigDecimal.valueOf(100))
                    .divide(product.sellingPrice(), 6, RoundingMode.HALF_UP);
        }
        var threshold = context.config().lowMarginThresholdPct();
        var ratio = marginPct.signum() <= 0
                ? new BigDecimal("2") : RuleSupport.ratio(threshold, marginPct);
        if (severity == null && ratio.compareTo(new BigDecimal("0.5")) < 0) {
            return Optional.empty();
        }
        if (severity == null) {
            severity = RuleSupport.severity(ratio);
        }
        var metrics = RuleSupport.metrics("1", marginPct, threshold, ratio,
                context.evaluatedAt().toString(), Map.of(
                        "sku", product.sku(),
                        "cost", product.cost(),
                        "sellingPrice", product.sellingPrice(),
                        "absoluteMargin", margin));
        return Optional.of(new RiskFinding(type(), severity, RiskEntityType.PRODUCT, product.id(), metrics,
                "Product " + product.sku() + " is below the target gross-margin window.",
                "Review the price or cost basis for " + product.sku() + " before scaling sales."));
    }
}
