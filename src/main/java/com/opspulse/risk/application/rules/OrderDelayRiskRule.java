package com.opspulse.risk.application.rules;

import com.opspulse.risk.application.port.in.RiskRule;
import com.opspulse.risk.domain.RiskContext;
import com.opspulse.risk.domain.RiskEntityType;
import com.opspulse.risk.domain.RiskFinding;
import com.opspulse.risk.domain.RiskSeverity;
import com.opspulse.risk.domain.RiskType;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OrderDelayRiskRule implements RiskRule {

    @Override
    public RiskType type() {
        return RiskType.ORDER_DELAY_RISK;
    }

    @Override
    public Optional<RiskFinding> evaluate(RiskContext context) {
        var order = context.order();
        if (order == null || "SHIPPED".equals(order.status()) || "CANCELLED".equals(order.status())) {
            return Optional.empty();
        }
        long daysUntilDue = ChronoUnit.DAYS.between(context.evaluatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate(),
                order.expectedShipDate());
        if (daysUntilDue > context.config().orderDelayNearDays()) {
            return Optional.empty();
        }
        RiskSeverity severity;
        if ("DELAYED".equals(order.status()) || daysUntilDue < -2) {
            severity = RiskSeverity.CRITICAL;
        } else if (daysUntilDue <= 0) {
            severity = RiskSeverity.HIGH;
        } else if (daysUntilDue == 1) {
            severity = RiskSeverity.MEDIUM;
        } else {
            severity = RiskSeverity.LOW;
        }
        var metrics = RuleSupport.metrics("1", BigDecimal.valueOf(-daysUntilDue), BigDecimal.ZERO,
                BigDecimal.valueOf(Math.max(0, -daysUntilDue)), context.evaluatedAt().toString(), Map.of(
                        "orderNumber", order.orderNumber(),
                        "status", order.status(),
                        "expectedShipDate", order.expectedShipDate().toString(),
                        "daysUntilDue", daysUntilDue,
                        "nearDays", context.config().orderDelayNearDays()));
        return Optional.of(new RiskFinding(type(), severity, RiskEntityType.ORDER, order.id(), metrics,
                "Order " + order.orderNumber() + " is approaching or past its expected ship date.",
                "Confirm fulfillment status and notify the customer for order " + order.orderNumber() + "."));
    }
}
