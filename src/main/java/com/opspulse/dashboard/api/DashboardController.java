package com.opspulse.dashboard.api;

import com.opspulse.ai.api.dto.AiRecommendationResponse;
import com.opspulse.dashboard.application.DashboardService;
import com.opspulse.shared.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT_SCHEME)
@PreAuthorize("isAuthenticated()")
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping
    @Operation(summary = "Read the operations dashboard snapshot")
    public DashboardResponse get() {
        return DashboardResponse.from(dashboard.snapshot());
    }

    public record DashboardResponse(long totalProducts, long totalOpenOrders, long delayedOrdersCount,
                                    long openRiskEventsCount, AiRecommendationResponse latestBrief,
                                    Instant asOf) {
        static DashboardResponse from(DashboardService.Snapshot snapshot) {
            return new DashboardResponse(snapshot.totalProducts(), snapshot.totalOpenOrders(),
                    snapshot.delayedOrdersCount(), snapshot.openRiskEventsCount(),
                    snapshot.latestBrief() == null ? null : AiRecommendationResponse.from(snapshot.latestBrief()),
                    snapshot.asOf());
        }
    }
}
