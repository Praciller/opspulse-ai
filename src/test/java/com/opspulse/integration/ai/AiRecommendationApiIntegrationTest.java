package com.opspulse.integration.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspulse.integration.support.Phase2ApiIntegrationTestSupport;
import com.opspulse.outbox.application.OutboxDeliveryService;
import io.micrometer.core.instrument.MeterRegistry;
import com.opspulse.shared.observability.RequestIdContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;

@ActiveProfiles("test")
@SpringBootTest(properties = "opspulse.outbox.poll-interval-ms=3600000")
@AutoConfigureMockMvc
class AiRecommendationApiIntegrationTest extends Phase2ApiIntegrationTestSupport {

    @Autowired
    private OutboxDeliveryService outboxDelivery;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void fallbackGenerationPersistsLinksAuditOutboxAndSupportsLifecycleActions() throws Exception {
        UUID riskId = UUID.randomUUID();
        insertRisk(riskId);
        double generationBefore = meterRegistry.counter("opspulse.ai.generation.count").count();
        double fallbackBefore = meterRegistry.counter("opspulse.ai.generation.fallback").count();
        double providerFailureBefore = meterRegistry.counter("opspulse.ai.provider.failure").count();
        String manager = login("manager@opspulse.demo");
        String requestId = "ai-generate-" + riskId.toString().substring(0, 8);

        var generated = mockMvc.perform(post("/api/ai/recommendations/generate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + manager)
                        .header(RequestIdContext.HEADER_NAME, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topN\":5}"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdContext.HEADER_NAME, requestId))
                .andExpect(jsonPath("$.generatedBy").value("RULE_BASED"))
                .andExpect(jsonPath("$.status").value("GENERATED"))
                .andExpect(jsonPath("$.inputRiskEventIds[0]").value(riskId.toString()))
                .andReturn();
        UUID briefId = UUID.fromString(objectMapper.readTree(generated.getResponse().getContentAsString()).get("id").asText());

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from ai_recommendation_items where recommendation_id=? and risk_event_id=?",
                Integer.class, briefId, riskId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from ai_usage_audit where recommendation_id=? and success=false",
                Integer.class, briefId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from outbox_events where aggregate_id=? and event_type='opspulse.brief.generated'",
                Integer.class, briefId)).isEqualTo(1);
        assertThat(outboxDelivery.processOne()).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from outbox_events where aggregate_id=? and event_type='opspulse.brief.generated' and status='PROCESSED'",
                Integer.class, briefId)).isEqualTo(1);
        assertThat(meterRegistry.counter("opspulse.ai.generation.count").count()).isGreaterThan(generationBefore);
        assertThat(meterRegistry.counter("opspulse.ai.generation.fallback").count()).isGreaterThan(fallbackBefore);
        assertThat(meterRegistry.counter("opspulse.ai.provider.failure").count()).isGreaterThan(providerFailureBefore);
        assertThat(meterRegistry.timer("opspulse.ai.generation.duration").count()).isGreaterThan(0);

        mockMvc.perform(post("/api/ai/recommendations/{id}/approve", briefId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + manager)
                        .header(RequestIdContext.HEADER_NAME, "ai-approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(post("/api/ai/recommendations/{id}/reject", briefId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + manager)
                        .header(RequestIdContext.HEADER_NAME, "ai-reject"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_AI_STATUS"));
        mockMvc.perform(patch("/api/ai/recommendations/{id}/feedback", briefId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"Useful prioritization\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userFeedback").value("Useful prioritization"));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where entity_id=? and action like 'ai.recommendation.%'",
                Integer.class, briefId)).isEqualTo(3);
    }

    @Test
    void readsAreOpenToAuthenticatedUsersButWritesAreManagerOrAdminOnly() throws Exception {
        String viewer = login("viewer@opspulse.demo");
        String operator = login("operator1@opspulse.demo");
        mockMvc.perform(get("/api/ai/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewer)
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
        mockMvc.perform(post("/api/ai/recommendations/generate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topN\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/ai/recommendations/generate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topN\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validatesTopNFeedbackAndWhitelistedSort() throws Exception {
        String manager = login("manager@opspulse.demo");
        mockMvc.perform(post("/api/ai/recommendations/generate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topN\":21}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(get("/api/ai/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + manager)
                        .param("sort", "summary,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void openApiDocumentsAiRecommendationPathsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/ai/recommendations'].get").exists())
                .andExpect(jsonPath("$.paths['/api/ai/recommendations/generate'].post").exists())
                .andExpect(jsonPath("$.paths['/api/ai/recommendations/{id}/feedback'].patch").exists())
                .andExpect(jsonPath("$.components.schemas.AiRecommendationResponse").exists());
    }

    private void insertRisk(UUID riskId) {
        UUID productId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into products(id, sku, name, unit, current_stock, safety_stock, reorder_point, cost, selling_price, active)
                values (?, ?, 'AI Test Product', 'PCS', 1, 0, 0, 1, 2, true)
                """, productId, "AI-" + riskId.toString().substring(0, 8).toUpperCase());
        jdbcTemplate.update("""
                insert into risk_events(id, risk_type, severity, entity_type, entity_id, source_metrics, explanation, recommended_action, status, created_at)
                values (?, 'LOW_MARGIN_RISK', 'HIGH', 'PRODUCT', ?, '{"ruleVersion":"1.0.0","ratio":1.2,"threshold":1}'::jsonb,
                        'AI integration risk', 'Review product margin', 'OPEN', now())
                """, riskId, productId);
    }
}
