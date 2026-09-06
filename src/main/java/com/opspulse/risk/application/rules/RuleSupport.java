package com.opspulse.risk.application.rules;

import com.opspulse.risk.domain.RiskSeverity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

final class RuleSupport {

    private static final BigDecimal LOW_BOUND = new BigDecimal("0.5");
    private static final BigDecimal MEDIUM_BOUND = new BigDecimal("0.667");
    private static final BigDecimal HIGH_BOUND = BigDecimal.ONE;
    private static final BigDecimal CRITICAL_BOUND = new BigDecimal("2");

    private RuleSupport() {}

    static RiskSeverity severity(BigDecimal ratio) {
        if (ratio.compareTo(CRITICAL_BOUND) >= 0) {
            return RiskSeverity.CRITICAL;
        }
        if (ratio.compareTo(HIGH_BOUND) >= 0) {
            return RiskSeverity.HIGH;
        }
        if (ratio.compareTo(MEDIUM_BOUND) >= 0) {
            return RiskSeverity.MEDIUM;
        }
        return RiskSeverity.LOW;
    }

    static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() <= 0) {
            return CRITICAL_BOUND;
        }
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP);
    }

    static Map<String, Object> metrics(
            String ruleVersion,
            BigDecimal observed,
            BigDecimal threshold,
            BigDecimal ratio,
            String evaluatedAt,
            Map<String, Object> extra) {
        var result = new LinkedHashMap<String, Object>();
        result.put("ruleVersion", ruleVersion);
        result.put("observed", observed);
        result.put("threshold", threshold);
        result.put("ratio", ratio);
        result.put("evaluatedAt", evaluatedAt);
        if (extra != null) {
            result.putAll(extra);
        }
        return result;
    }
}
