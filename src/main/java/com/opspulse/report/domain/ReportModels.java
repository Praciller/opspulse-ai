package com.opspulse.report.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReportModels {
    private ReportModels() {}

    public record DailyOpsBriefReport(UUID id, String generatedBy, String status, String summary,
                                      String actionsJson, String messageDraftsJson, Instant generatedAt, Instant asOf) {}

    public record InventoryRiskReport(Instant asOf, Map<String, Long> bySeverity, Map<String, Long> byType,
                                      List<RiskRow> topRisks) {}

    public record RiskRow(UUID id, String riskType, String severity, String entityType, UUID entityId,
                          String explanation, String recommendedAction, Instant createdAt) {}

    public record SupplierSlaReport(Instant asOf, List<SupplierSlaRow> suppliers) {}

    public record SupplierSlaRow(UUID supplierId, String name, long receivedCount, long lateCount,
                                 BigDecimal lateRatePct, BigDecimal averageLeadTimeDays) {}

    public record OrderDelayReport(Instant asOf, Map<String, Long> byStatus, List<DelayedOrderRow> delayedOrders) {}

    public record DelayedOrderRow(UUID orderId, String orderNumber, String customerName,
                                  LocalDate expectedShipDate, long daysLate, String status) {}

    public record ProductMarginReport(Instant asOf, long lowMarginCount, List<ProductMarginRow> products) {}

    public record ProductMarginRow(UUID productId, String sku, String name, BigDecimal cost,
                                   BigDecimal sellingPrice, BigDecimal marginAmount, BigDecimal marginPct,
                                   String riskLevel) {}
}
