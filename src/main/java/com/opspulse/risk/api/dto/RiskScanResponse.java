package com.opspulse.risk.api.dto;

import com.opspulse.risk.application.port.in.RiskUseCase;
import java.time.Instant;

public record RiskScanResponse(int evaluatedEntities, int createdRiskEvents, Instant completedAt) {

    public static RiskScanResponse from(RiskUseCase.RiskScanResult result) {
        return new RiskScanResponse(result.evaluatedEntities(), result.createdRiskEvents(), result.completedAt());
    }
}
