package com.opspulse.report.api;

import com.opspulse.report.api.dto.ReportResponses.DailyOpsBriefResponse;
import com.opspulse.report.api.dto.ReportResponses.InventoryRiskResponse;
import com.opspulse.report.api.dto.ReportResponses.OrderDelayResponse;
import com.opspulse.report.api.dto.ReportResponses.ProductMarginResponse;
import com.opspulse.report.api.dto.ReportResponses.SupplierSlaResponse;
import com.opspulse.report.application.ReportService;
import com.opspulse.shared.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/reports")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT_SCHEME)
@PreAuthorize("isAuthenticated()")
public class ReportController {
    private final ReportService reports;
    private final MeterRegistry metrics;

    public ReportController(ReportService reports, MeterRegistry metrics) {
        this.reports = reports;
        this.metrics = metrics;
    }

    @GetMapping("/daily-ops-brief")
    @Operation(summary = "Latest daily operations brief")
    public DailyOpsBriefResponse dailyOpsBrief() { return timed("daily-ops-brief", () -> DailyOpsBriefResponse.from(reports.dailyOpsBrief())); }

    @GetMapping("/inventory-risk")
    public InventoryRiskResponse inventoryRisk() { return timed("inventory-risk", () -> InventoryRiskResponse.from(reports.inventoryRisk())); }

    @GetMapping("/supplier-sla")
    public SupplierSlaResponse supplierSla() { return timed("supplier-sla", () -> SupplierSlaResponse.from(reports.supplierSla())); }

    @GetMapping("/order-delay")
    public OrderDelayResponse orderDelay() { return timed("order-delay", () -> OrderDelayResponse.from(reports.orderDelay())); }

    @GetMapping("/product-margin")
    public ProductMarginResponse productMargin() { return timed("product-margin", () -> ProductMarginResponse.from(reports.productMargin())); }

    private <T> T timed(String report, Supplier<T> action) {
        Timer.Sample sample = Timer.start(metrics);
        try {
            metrics.counter("opspulse.report.requests", "report", report).increment();
            return action.get();
        } finally {
            sample.stop(metrics.timer("opspulse.report.duration", "report", report));
        }
    }
}
