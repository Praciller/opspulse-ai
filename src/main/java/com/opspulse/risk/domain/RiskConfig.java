package com.opspulse.risk.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record RiskConfig(
        int slowMovingDays,
        int excessiveDaysThreshold,
        BigDecimal lowMarginThresholdPct,
        BigDecimal lateDeliveryRateThresholdPct,
        int orderDelayNearDays,
        String riskScanCron) {

    public RiskConfig {
        if (slowMovingDays <= 0 || excessiveDaysThreshold <= 0 || orderDelayNearDays < 0) {
            throw new IllegalArgumentException("risk day thresholds are invalid");
        }
        lowMarginThresholdPct = positive(lowMarginThresholdPct, "lowMarginThresholdPct");
        lateDeliveryRateThresholdPct = positive(lateDeliveryRateThresholdPct, "lateDeliveryRateThresholdPct");
        riskScanCron = Objects.requireNonNull(riskScanCron, "riskScanCron must not be null");
        if (riskScanCron.isBlank()) {
            throw new IllegalArgumentException("riskScanCron must not be blank");
        }
    }

    private static BigDecimal positive(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value.stripTrailingZeros();
    }
}
