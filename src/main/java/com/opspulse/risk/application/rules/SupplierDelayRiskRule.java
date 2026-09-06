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
public class SupplierDelayRiskRule implements RiskRule {

    @Override
    public RiskType type() {
        return RiskType.SUPPLIER_DELAY_RISK;
    }

    @Override
    public Optional<RiskFinding> evaluate(RiskContext context) {
        var supplier = context.supplier();
        if (supplier == null || !supplier.active() || supplier.completedDeliveries() <= 0
                || supplier.lateDeliveryRatePct() == null) {
            return Optional.empty();
        }
        var threshold = context.config().lateDeliveryRateThresholdPct();
        var ratio = RuleSupport.ratio(supplier.lateDeliveryRatePct(), threshold);
        if (ratio.compareTo(new BigDecimal("0.5")) < 0) {
            return Optional.empty();
        }
        var metrics = RuleSupport.metrics("1", supplier.lateDeliveryRatePct(), threshold, ratio,
                context.evaluatedAt().toString(), Map.of(
                        "supplierName", supplier.name(),
                        "completedDeliveries", supplier.completedDeliveries()));
        return Optional.of(new RiskFinding(type(), RuleSupport.severity(ratio), RiskEntityType.SUPPLIER,
                supplier.id(), metrics,
                "Supplier " + supplier.name() + " is exceeding the late-delivery tolerance.",
                "Contact " + supplier.name() + " and review purchase-order SLA performance."));
    }
}
