package com.opspulse.report.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.opspulse.report.domain.ReportModels;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ReportResponses {
    private ReportResponses() {}

    public record DailyOpsBriefResponse(UUID id, String generatedBy, String status, String summary,
                                        JsonNode actions, JsonNode messageDrafts, Instant generatedAt, Instant asOf) {
        public static DailyOpsBriefResponse from(ReportModels.DailyOpsBriefReport value) {
            return new DailyOpsBriefResponse(value.id(), value.generatedBy(), value.status(), value.summary(),
                    parse(value.actionsJson()), parse(value.messageDraftsJson()), value.generatedAt(), value.asOf());
        }

        private static JsonNode parse(String value) {
            try { return new ObjectMapper().readTree(value == null ? "[]" : value); }
            catch (Exception exception) { return new ObjectMapper().createArrayNode(); }
        }
    }

    public record InventoryRiskResponse(Instant asOf, Map<String, Long> bySeverity, Map<String, Long> byType,
                                        List<RiskResponse> topRisks) {
        public static InventoryRiskResponse from(ReportModels.InventoryRiskReport value) {
            return new InventoryRiskResponse(value.asOf(), value.bySeverity(), value.byType(),
                    value.topRisks().stream().map(RiskResponse::from).toList());
        }
    }

    public record RiskResponse(UUID id, String riskType, String severity, String entityType, UUID entityId,
                               String explanation, String recommendedAction, Instant createdAt) {
        static RiskResponse from(ReportModels.RiskRow value) {
            return new RiskResponse(value.id(), value.riskType(), value.severity(), value.entityType(), value.entityId(),
                    value.explanation(), value.recommendedAction(), value.createdAt());
        }
    }

    public record SupplierSlaResponse(Instant asOf, List<SupplierSlaRow> suppliers) {
        public static SupplierSlaResponse from(ReportModels.SupplierSlaReport value) {
            return new SupplierSlaResponse(value.asOf(), value.suppliers().stream().map(SupplierSlaRow::from).toList());
        }
    }

    public record SupplierSlaRow(UUID supplierId, String name, long receivedCount, long lateCount,
                                 BigDecimal lateRatePct, BigDecimal averageLeadTimeDays) {
        static SupplierSlaRow from(ReportModels.SupplierSlaRow value) {
            return new SupplierSlaRow(value.supplierId(), value.name(), value.receivedCount(), value.lateCount(),
                    value.lateRatePct(), value.averageLeadTimeDays());
        }
    }

    public record OrderDelayResponse(Instant asOf, Map<String, Long> byStatus, List<DelayedOrderRow> delayedOrders) {
        public static OrderDelayResponse from(ReportModels.OrderDelayReport value) {
            return new OrderDelayResponse(value.asOf(), value.byStatus(), value.delayedOrders().stream().map(DelayedOrderRow::from).toList());
        }
    }

    public record DelayedOrderRow(UUID orderId, String orderNumber, String customerName,
                                  LocalDate expectedShipDate, long daysLate, String status) {
        static DelayedOrderRow from(ReportModels.DelayedOrderRow value) {
            return new DelayedOrderRow(value.orderId(), value.orderNumber(), value.customerName(), value.expectedShipDate(),
                    value.daysLate(), value.status());
        }
    }

    public record ProductMarginResponse(Instant asOf, long lowMarginCount, List<ProductMarginRow> products) {
        public static ProductMarginResponse from(ReportModels.ProductMarginReport value) {
            return new ProductMarginResponse(value.asOf(), value.lowMarginCount(), value.products().stream().map(ProductMarginRow::from).toList());
        }
    }

    public record ProductMarginRow(UUID productId, String sku, String name, BigDecimal cost,
                                   BigDecimal sellingPrice, BigDecimal marginAmount, BigDecimal marginPct,
                                   String riskLevel) {
        static ProductMarginRow from(ReportModels.ProductMarginRow value) {
            return new ProductMarginRow(value.productId(), value.sku(), value.name(), value.cost(), value.sellingPrice(),
                    value.marginAmount(), value.marginPct(), value.riskLevel());
        }
    }
}
