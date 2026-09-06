package com.opspulse.risk.application.port.out;

import com.opspulse.risk.domain.RiskContext;
import java.time.Instant;
import java.util.List;

public interface MetricRepository {

    RiskMetricsSnapshot load(Instant asOf);

    record RiskMetricsSnapshot(
            List<RiskContext.ProductMetrics> products,
            List<RiskContext.OrderMetrics> orders,
            List<RiskContext.SupplierMetrics> suppliers) {
        public RiskMetricsSnapshot {
            products = products == null ? List.of() : List.copyOf(products);
            orders = orders == null ? List.of() : List.copyOf(orders);
            suppliers = suppliers == null ? List.of() : List.copyOf(suppliers);
        }

        public int evaluatedEntities() {
            return products.size() + orders.size() + suppliers.size();
        }
    }
}
