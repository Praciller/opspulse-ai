package com.opspulse.dashboard.application;

import com.opspulse.ai.application.port.in.BriefUseCase;
import com.opspulse.ai.domain.AiRecommendation;
import java.time.Clock;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final JdbcTemplate jdbc;
    private final BriefUseCase briefs;
    private final Clock clock;

    public DashboardService(JdbcTemplate jdbc, BriefUseCase briefs, Clock clock) {
        this.jdbc = jdbc;
        this.briefs = briefs;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Snapshot snapshot() {
        var counts = jdbc.queryForObject("""
                select
                    (select count(*) from products where active = true) as total_products,
                    (select count(*) from orders where status not in ('SHIPPED', 'CANCELLED')) as total_open_orders,
                    (select count(*) from orders where status = 'DELAYED') as delayed_orders,
                    (select count(*) from risk_events where status in ('OPEN', 'ACKNOWLEDGED')) as open_risks
                """, (rs, row) -> new Counts(
                rs.getLong("total_products"),
                rs.getLong("total_open_orders"),
                rs.getLong("delayed_orders"),
                rs.getLong("open_risks")));
        AiRecommendation latestBrief = briefs.findAll(0, 1, null, "createdAt", false)
                .items().stream().findFirst().orElse(null);
        return new Snapshot(counts.totalProducts(), counts.totalOpenOrders(), counts.delayedOrders(),
                counts.openRisks(), latestBrief, clock.instant());
    }

    public record Snapshot(long totalProducts, long totalOpenOrders, long delayedOrdersCount,
                           long openRiskEventsCount, AiRecommendation latestBrief, Instant asOf) {}

    private record Counts(long totalProducts, long totalOpenOrders, long delayedOrders, long openRisks) {}
}
