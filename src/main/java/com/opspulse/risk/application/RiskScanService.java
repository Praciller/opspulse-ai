package com.opspulse.risk.application;

import com.opspulse.risk.application.port.in.RiskUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Synchronous scan boundary used by the scheduler and ad-hoc scan endpoint.
 * Lifecycle actions remain on {@link RiskService}; this class keeps the scan
 * transaction boundary explicit for future callers.
 */
@Service
public class RiskScanService {

    private final RiskUseCase risks;

    public RiskScanService(RiskUseCase risks) {
        this.risks = risks;
    }

    @Transactional
    public RiskUseCase.RiskScanResult runScan() {
        return risks.scan();
    }
}
