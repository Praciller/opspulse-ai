package com.opspulse.risk.application.port.in;

import com.opspulse.risk.domain.RiskEvent;
import com.opspulse.risk.domain.RiskEntityType;
import com.opspulse.risk.domain.RiskSeverity;
import com.opspulse.risk.domain.RiskStatus;
import com.opspulse.risk.domain.RiskType;
import com.opspulse.shared.web.PagedResponse;
import java.time.Instant;
import java.util.UUID;

public interface RiskUseCase {

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

    RiskEvent findById(UUID id);

    RiskScanResult scan();

    RiskEvent acknowledge(UUID id, UUID actorUserId, String requestId, String ipAddress);

    RiskEvent resolve(UUID id, UUID actorUserId, String requestId, String ipAddress);

    RiskEvent dismiss(UUID id, UUID actorUserId, String requestId, String ipAddress);

    record RiskScanResult(int evaluatedEntities, int createdRiskEvents, Instant completedAt) {}
}
