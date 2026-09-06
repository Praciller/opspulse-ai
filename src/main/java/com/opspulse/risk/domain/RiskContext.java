package com.opspulse.risk.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record RiskContext(
        ProductMetrics product,
        OrderMetrics order,
        SupplierMetrics supplier,
        RiskConfig config,
        Instant evaluatedAt) {

    public RiskContext {
        Objects.requireNonNull(config, "config must not be null");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        if (product == null && order == null && supplier == null) {
            throw new IllegalArgumentException("risk context must have an entity");
        }
    }

    public record ProductMetrics(
            UUID id,
            String sku,
            String name,
            BigDecimal currentStock,
            BigDecimal cost,
            BigDecimal sellingPrice,
            BigDecimal averageDailySales7d,
            BigDecimal averageDailySales30d,
            BigDecimal supplierLeadTimeDays,
            Instant lastMovementAt,
            Instant createdAt,
            boolean active,
            boolean activeOrderConsuming) {}

    public record OrderMetrics(
            UUID id,
            String orderNumber,
            String status,
            LocalDate expectedShipDate) {}

    public record SupplierMetrics(
            UUID id,
            String name,
            BigDecimal lateDeliveryRatePct,
            int completedDeliveries,
            boolean active) {}
}
