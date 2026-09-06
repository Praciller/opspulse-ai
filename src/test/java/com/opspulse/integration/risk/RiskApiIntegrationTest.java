package com.opspulse.integration.risk;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspulse.integration.support.Phase2ApiIntegrationTestSupport;
import com.opspulse.shared.observability.RequestIdContext;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class RiskApiIntegrationTest extends Phase2ApiIntegrationTestSupport {

    @Test
    void riskReadIsAuthenticatedAndScanIsManagerOrAdminOnly() throws Exception {
        mockMvc.perform(get("/api/risks")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/risks").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + login("viewer@opspulse.demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
        mockMvc.perform(post("/api/risks/scan").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + login("viewer@opspulse.demo")))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanAcknowledgeAndResolveRiskWithAudit() throws Exception {
        UUID riskId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into products(id, sku, name, unit, current_stock, safety_stock, reorder_point, cost, selling_price, active)
                values (?, ?, 'API Risk Product', 'PCS', 1, 0, 0, 1, 2, true)
                """, productId, "API-RISK-" + riskId.toString().substring(0, 8).toUpperCase());
        jdbcTemplate.update("""
                insert into risk_events(id, risk_type, severity, entity_type, entity_id, source_metrics, explanation, recommended_action, status, created_at)
                values (?, 'LOW_MARGIN_RISK', 'HIGH', 'PRODUCT', ?, '{"ruleVersion":"1","ratio":1}'::jsonb,
                        'API test risk', 'Review price', 'OPEN', ?)
                """, riskId, productId, java.sql.Timestamp.from(Instant.now()));

        String token = login("manager@opspulse.demo");
        mockMvc.perform(post("/api/risks/{id}/acknowledge", riskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(RequestIdContext.HEADER_NAME, "risk-ack"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
        mockMvc.perform(post("/api/risks/{id}/resolve", riskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(RequestIdContext.HEADER_NAME, "risk-resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
        Integer auditCount = jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where entity_id=? and action in ('risk.acknowledge','risk.resolve')",
                Integer.class, riskId);
        org.assertj.core.api.Assertions.assertThat(auditCount).isEqualTo(2);
    }

    @Test
    void manualLifecycleRejectsInvalidTransitionsAndPreservesRequestIds() throws Exception {
        UUID entityId = UUID.randomUUID();
        UUID acknowledgeId = UUID.randomUUID();
        UUID dismissId = UUID.randomUUID();
        UUID resolveId = UUID.randomUUID();
        insertRisk(acknowledgeId, "STOCKOUT_RISK", "HIGH", entityId, "OPEN");
        insertRisk(dismissId, "OVERSTOCK_RISK", "MEDIUM", entityId, "OPEN");
        insertRisk(resolveId, "LOW_MARGIN_RISK", "LOW", entityId, "OPEN");

        String token = login("manager@opspulse.demo");
        mockMvc.perform(post("/api/risks/{id}/acknowledge", acknowledgeId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(RequestIdContext.HEADER_NAME, "risk-acknowledge"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdContext.HEADER_NAME, "risk-acknowledge"))
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
        mockMvc.perform(post("/api/risks/{id}/acknowledge", acknowledgeId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(RequestIdContext.HEADER_NAME, "risk-acknowledge-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RISK_STATUS"))
                .andExpect(jsonPath("$.requestId").value("risk-acknowledge-conflict"));

        mockMvc.perform(post("/api/risks/{id}/dismiss", dismissId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(RequestIdContext.HEADER_NAME, "risk-dismiss"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));
        mockMvc.perform(post("/api/risks/{id}/resolve", dismissId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RISK_STATUS"));

        mockMvc.perform(post("/api/risks/{id}/resolve", resolveId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
        mockMvc.perform(post("/api/risks/{id}/dismiss", resolveId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RISK_STATUS"));

        Integer auditCount = jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where entity_id in (?, ?, ?) and action in ('risk.acknowledge','risk.resolve','risk.dismiss')",
                Integer.class, acknowledgeId, dismissId, resolveId);
        org.assertj.core.api.Assertions.assertThat(auditCount).isEqualTo(3);
    }

    @Test
    void listSupportsFiltersPaginationDatesAndWhitelistedSorts() throws Exception {
        UUID entityId = UUID.randomUUID();
        insertRisk(UUID.randomUUID(), "STOCKOUT_RISK", "CRITICAL", entityId, "OPEN");
        insertRisk(UUID.randomUUID(), "OVERSTOCK_RISK", "MEDIUM", entityId, "ACKNOWLEDGED");
        String token = login("viewer@opspulse.demo");
        String from = Instant.now().minusSeconds(60).toString();
        String to = Instant.now().plusSeconds(60).toString();

        mockMvc.perform(get("/api/risks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("entityId", entityId.toString())
                        .param("from", from)
                        .param("to", to)
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/api/risks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("riskType", "STOCKOUT_RISK")
                        .param("severity", "CRITICAL")
                        .param("status", "OPEN")
                        .param("entityType", "PRODUCT")
                        .param("entityId", entityId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].riskType").value("STOCKOUT_RISK"));
        mockMvc.perform(get("/api/risks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("sort", "sourceMetrics,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void openApiDocumentsRiskAndOutboxContracts() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/risks'].get").exists())
                .andExpect(jsonPath("$.paths['/api/risks/{id}/dismiss'].post").exists())
                .andExpect(jsonPath("$.paths['/api/admin/outbox-events/{id}/discard'].post").exists())
                .andExpect(jsonPath("$.components.schemas.RiskEventResponse").exists())
                .andExpect(jsonPath("$.components.schemas.RiskScanResponse").exists());
    }

    private void insertRisk(UUID id, String riskType, String severity, UUID entityId, String status) {
        jdbcTemplate.update("""
                insert into risk_events(id, risk_type, severity, entity_type, entity_id, source_metrics, explanation, recommended_action, status, created_at)
                values (?, ?, ?, 'PRODUCT', ?, '{"ruleVersion":"1","observed":1,"threshold":1,"ratio":1,"evaluatedAt":"2026-07-17T00:00:00Z"}'::jsonb,
                        'API test risk', 'Review risk', ?, now())
                """, id, riskType, severity, entityId, status);
    }
}
