package com.opspulse.risk.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.risk.application.port.out.RiskConfigurationProvider;
import com.opspulse.risk.domain.RiskConfig;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRiskConfigurationProvider implements RiskConfigurationProvider {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcRiskConfigurationProvider(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public RiskConfig current() {
        var values = jdbcTemplate.query(
                "select key, value::text from app_config where key in (?, ?, ?, ?, ?, ?)",
                result -> {
                    var map = new java.util.HashMap<String, String>();
                    while (result.next()) {
                        map.put(result.getString(1), result.getString(2));
                    }
                    return map;
                },
                "slowMovingDays", "excessiveDaysThreshold", "lowMarginThresholdPct",
                "lateDeliveryRateThresholdPct", "orderDelayNearDays", "riskScanCron");
        return new RiskConfig(
                integer(values, "slowMovingDays", 30),
                integer(values, "excessiveDaysThreshold", 90),
                decimal(values, "lowMarginThresholdPct", "20"),
                decimal(values, "lateDeliveryRateThresholdPct", "20"),
                integer(values, "orderDelayNearDays", 2),
                text(values, "riskScanCron", "0 0 7 * * *"));
    }

    private int integer(java.util.Map<String, String> values, String key, int fallback) {
        return node(values, key).isMissingNode() ? fallback : node(values, key).asInt(fallback);
    }

    private BigDecimal decimal(java.util.Map<String, String> values, String key, String fallback) {
        return node(values, key).isMissingNode()
                ? new BigDecimal(fallback) : new BigDecimal(node(values, key).asText());
    }

    private String text(java.util.Map<String, String> values, String key, String fallback) {
        return node(values, key).isMissingNode() ? fallback : node(values, key).asText(fallback);
    }

    private JsonNode node(java.util.Map<String, String> values, String key) {
        try {
            var raw = values.get(key);
            return raw == null ? objectMapper.getNodeFactory().missingNode() : objectMapper.readTree(raw);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid app_config value for " + key, exception);
        }
    }
}
