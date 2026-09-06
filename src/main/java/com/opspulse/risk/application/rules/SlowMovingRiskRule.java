package com.opspulse.risk.application.rules;

import com.opspulse.risk.application.port.in.RiskRule;
import com.opspulse.risk.domain.RiskContext;
import com.opspulse.risk.domain.RiskEntityType;
import com.opspulse.risk.domain.RiskFinding;
import com.opspulse.risk.domain.RiskType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SlowMovingRiskRule implements RiskRule {

    @Override
    public RiskType type() {
        return RiskType.SLOW_MOVING_INVENTORY;
    }

    @Override
    public Optional<RiskFinding> evaluate(RiskContext context) {
        var product = context.product();
        if (product == null || !product.active() || product.currentStock().signum() <= 0) {
            return Optional.empty();
        }
        Instant lastMovement = product.lastMovementAt() == null
                ? product.createdAt() : product.lastMovementAt();
        long days = Math.max(0, Duration.between(lastMovement, context.evaluatedAt()).toDays());
        var threshold = BigDecimal.valueOf(context.config().slowMovingDays());
        var ratio = RuleSupport.ratio(BigDecimal.valueOf(days), threshold);
        // Slow-moving inventory is a hard elapsed-time condition: the product
        // must have had no movement for at least the configured day threshold.
        if (ratio.compareTo(BigDecimal.ONE) < 0) {
            return Optional.empty();
        }
        var metrics = RuleSupport.metrics("1", BigDecimal.valueOf(days), threshold, ratio,
                context.evaluatedAt().toString(), Map.of(
                        "sku", product.sku(),
                        "currentStock", product.currentStock(),
                        "lastMovementAt", lastMovement.toString()));
        return Optional.of(new RiskFinding(type(), RuleSupport.severity(ratio), RiskEntityType.PRODUCT,
                product.id(), metrics,
                "Product " + product.sku() + " has had stock without a recent movement.",
                "Review demand, pricing, and liquidation options for " + product.sku() + "."));
    }
}
