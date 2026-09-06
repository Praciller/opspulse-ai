package com.opspulse.unit.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.ai.application.PromptBuilder;
import com.opspulse.ai.application.RuleBasedBriefClient;
import com.opspulse.ai.application.port.out.PromptVersionRepository;
import com.opspulse.ai.domain.AiRecommendation;
import com.opspulse.ai.domain.BriefRequest;
import com.opspulse.ai.domain.BriefRisk;
import com.opspulse.ai.domain.CreatedByType;
import com.opspulse.ai.domain.RecommendationStatus;
import com.opspulse.risk.domain.RiskEntityType;
import com.opspulse.risk.domain.RiskSeverity;
import com.opspulse.risk.domain.RiskType;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiBriefUnitTest {

    private static final UUID RISK_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Test
    void promptContainsOnlyStableRiskEvidenceAndNeverSecrets() {
        var risk = risk("Place a purchase order", Map.of("ratio", 1.2));
        var request = new BriefRequest("1.0.0", List.of(risk), "default", Locale.ENGLISH);
        var prompt = new PromptBuilder(new ObjectMapper()).build(request,
                new PromptVersionRepository.PromptVersion("1.0.0", "Use the evidence."));
        assertThat(prompt).contains(RISK_ID.toString(), "ratio", "Use the evidence.");
        assertThat(prompt).doesNotContain("AI_API_KEY", "secret", "password");
    }

    @Test
    void ruleBasedBriefIsDeterministicAndGrounded() {
        var request = new BriefRequest("1.0.0", List.of(risk("Replenish stock", Map.of("stock", 2))),
                "default", Locale.ENGLISH);
        var client = new RuleBasedBriefClient();
        var first = client.generate(request);
        var second = client.generate(request);
        assertThat(first).isEqualTo(second);
        assertThat(first.generatedBy()).isEqualTo(com.opspulse.ai.domain.GeneratedBy.RULE_BASED);
        assertThat(first.actions()).extracting("riskEventId").containsExactly(RISK_ID);
    }

    @Test
    void recommendationLifecycleAllowsApprovalOrRejectionOnlyFromGenerated() {
        var response = new RuleBasedBriefClient().generate(new BriefRequest("1.0.0", List.of(risk("Act", Map.of())),
                "default", Locale.ENGLISH));
        var recommendation = AiRecommendation.generated(UUID.randomUUID(), response, Instant.EPOCH,
                UUID.randomUUID(), CreatedByType.USER, null);
        assertThat(recommendation.approve().status()).isEqualTo(RecommendationStatus.APPROVED);
        assertThatThrownBy(() -> recommendation.approve().approve())
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void emptyRiskSetProducesAStableNoRiskBrief() {
        var response = new RuleBasedBriefClient().generate(new BriefRequest("1.0.0", List.of(),
                "default", Locale.ENGLISH));
        assertThat(response.summary()).isEqualTo("No open risk events require attention.");
        assertThat(response.actions()).isEmpty();
        assertThat(response.riskEventIds()).isEmpty();
    }

    private static BriefRisk risk(String action, Map<String, Object> metrics) {
        return new BriefRisk(RISK_ID, RiskType.STOCKOUT_RISK, RiskSeverity.HIGH,
                RiskEntityType.PRODUCT, UUID.fromString("40000000-0000-0000-0000-000000000001"),
                metrics, "Stock is below required level", action, Instant.parse("2026-07-17T00:00:00Z"));
    }
}
