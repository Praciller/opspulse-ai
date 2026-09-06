package com.opspulse.risk.application.port.out;

import com.opspulse.risk.domain.RiskEvent;
import com.opspulse.risk.domain.RiskSeverity;
import com.opspulse.risk.domain.RiskStatus;
import com.opspulse.risk.domain.RiskType;
import com.opspulse.risk.domain.RiskEntityType;
import com.opspulse.shared.web.PagedResponse;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface RiskEventRepository {

    Optional<RiskEvent> findById(UUID id);

    Optional<RiskEvent> findActiveByDedupKey(String dedupKey);

    Optional<RiskEvent> findLatestByDedupKey(String dedupKey);

    List<RiskEvent> findActive();

    List<RiskEvent> findTopOpenForBrief(int limit);

    RiskEvent save(RiskEvent event);

    PagedResponse<RiskEvent> findAll(
            int page,
            int size,
            RiskType riskType,
            RiskSeverity severity,
            RiskStatus status,
            RiskEntityType entityType,
            UUID entityId,
            Instant from,
            Instant to,
            String sortField,
            boolean ascending);
}
