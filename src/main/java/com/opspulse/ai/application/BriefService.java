package com.opspulse.ai.application;

import com.opspulse.ai.application.port.in.BriefUseCase;
import com.opspulse.ai.application.port.out.AiRecommendationRepository;
import com.opspulse.ai.application.port.out.AiUsageAuditRepository;
import com.opspulse.ai.application.port.out.OperationsBriefClient;
import com.opspulse.ai.application.port.out.PromptVersionRepository;
import com.opspulse.ai.domain.AiClientException;
import com.opspulse.ai.domain.AiRecommendation;
import com.opspulse.ai.domain.AiUsageAudit;
import com.opspulse.ai.domain.BriefRequest;
import com.opspulse.ai.domain.BriefResponse;
import com.opspulse.ai.domain.BriefRisk;
import com.opspulse.ai.domain.CreatedByType;
import com.opspulse.ai.domain.InvalidRecommendationStatusTransitionException;
import com.opspulse.ai.domain.RecommendationStatus;
import com.opspulse.audit.application.port.in.RecordAuditEventUseCase;
import com.opspulse.audit.domain.AuditEvent;
import com.opspulse.outbox.application.port.in.OutboxPublisher;
import com.opspulse.risk.application.port.out.RiskEventRepository;
import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import com.opspulse.shared.web.PagedResponse;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BriefService implements BriefUseCase {

    private static final Logger log = LoggerFactory.getLogger(BriefService.class);
    private static final int MAX_TOP_N = 20;

    private final AiRecommendationRepository recommendations;
    private final AiUsageAuditRepository usage;
    private final PromptVersionRepository promptVersions;
    private final RiskEventRepository risks;
    private final OperationsBriefClient primary;
    private final OperationsBriefClient fallback;
    private final OutboxPublisher outbox;
    private final RecordAuditEventUseCase audit;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public BriefService(
            AiRecommendationRepository recommendations,
            AiUsageAuditRepository usage,
            PromptVersionRepository promptVersions,
            RiskEventRepository risks,
            @org.springframework.beans.factory.annotation.Qualifier("springAiBriefClient") OperationsBriefClient primary,
            @org.springframework.beans.factory.annotation.Qualifier("ruleBasedBriefClient") OperationsBriefClient fallback,
            OutboxPublisher outbox,
            RecordAuditEventUseCase audit,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.recommendations = recommendations;
        this.usage = usage;
        this.promptVersions = promptVersions;
        this.risks = risks;
        this.primary = primary;
        this.fallback = fallback;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AiRecommendation> findAll(
            int page, int size, RecommendationStatus status, String sortField, boolean ascending) {
        return recommendations.findAll(page, size, status, sortField, ascending);
    }

    @Override
    @Transactional(readOnly = true)
    public AiRecommendation findById(UUID id) {
        return recommendations.findById(id).orElseThrow(() -> new ApiException(
                ErrorCode.AI_RECOMMENDATION_NOT_FOUND, HttpStatus.NOT_FOUND,
                "AI recommendation not found"));
    }

    @Override
    @Transactional
    public AiRecommendation generate(
            int topN, String promptVersion, UUID actorUserId, String requestId, String ipAddress) {
        if (topN < 1 || topN > MAX_TOP_N) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                    "topN must be between 1 and " + MAX_TOP_N);
        }
        Instant started = clock.instant();
        var version = (promptVersion == null || promptVersion.isBlank()
                ? promptVersions.findActive()
                : promptVersions.findByVersion(promptVersion))
                .orElseThrow(() -> new ApiException(ErrorCode.AI_PROMPT_VERSION_NOT_FOUND,
                        HttpStatus.BAD_REQUEST, "AI prompt version not found"));
        var selected = risks.findTopOpenForBrief(topN).stream().map(BriefRisk::from).toList();
        var request = new BriefRequest(version.version(), selected, "default", java.util.Locale.ENGLISH);
        BriefResponse response;
        boolean fallbackUsed = false;
        String errorClass = null;
        try {
            response = primary.generate(request);
            validateResponse(response, selected, version.version());
        } catch (AiClientException exception) {
            fallbackUsed = true;
            errorClass = sanitizeErrorClass(exception.errorClass());
            response = fallback.generate(request);
        } catch (RuntimeException exception) {
            fallbackUsed = true;
            errorClass = "AI_INVALID_RESPONSE";
            response = fallback.generate(request);
        }
        validateResponse(response, selected, version.version());
        var recommendation = AiRecommendation.generated(UUID.randomUUID(), response, clock.instant(),
                actorUserId, actorUserId == null ? CreatedByType.SYSTEM : CreatedByType.USER, null);
        recommendation = recommendations.save(recommendation);
        usage.save(new AiUsageAudit(UUID.randomUUID(), recommendation.id(),
                fallbackUsed ? "openai" : response.modelProviderName(), response.modelProviderName(),
                response.promptVersion(), response.inputTokenCount(), response.outputTokenCount(),
                Math.toIntExact(Math.max(0, Duration.between(started, clock.instant()).toMillis())),
                !fallbackUsed, errorClass, requestId, clock.instant()));
        audit.record(new AuditEvent(UUID.randomUUID(), actorUserId, "ai.recommendation.generate",
                "AI_RECOMMENDATION", recommendation.id(), null, recommendation.snapshot(), requestId,
                ipAddress, clock.instant()));
        outbox.publish("AI_RECOMMENDATION", recommendation.id(), "opspulse.brief.generated", java.util.Map.of(
                "briefId", recommendation.id().toString(),
                "promptVersion", recommendation.promptVersion(),
                "generatedBy", recommendation.generatedBy().name(),
                "inputRiskEventIds", recommendation.inputRiskEventIds().stream().map(UUID::toString).toList()));
        meterRegistry.counter("opspulse.ai.generation.count").increment();
        if (fallbackUsed) {
            meterRegistry.counter("opspulse.ai.generation.fallback").increment();
            meterRegistry.counter("opspulse.ai.provider.failure").increment();
        }
        if (response.inputTokenCount() != null) {
            meterRegistry.counter("opspulse.ai.tokens.input").increment(response.inputTokenCount());
        }
        if (response.outputTokenCount() != null) {
            meterRegistry.counter("opspulse.ai.tokens.output").increment(response.outputTokenCount());
        }
        meterRegistry.timer("opspulse.ai.generation.duration").record(Duration.between(started, clock.instant()));
        log.info("AI brief generated id={} generatedBy={} riskCount={} fallback={}",
                recommendation.id(), recommendation.generatedBy(), recommendation.inputRiskEventIds().size(), fallbackUsed);
        return recommendation;
    }

    @Override
    @Transactional
    public AiRecommendation approve(UUID id, UUID actorUserId, String requestId, String ipAddress) {
        return changeStatus(id, actorUserId, requestId, ipAddress, "ai.recommendation.approve", true);
    }

    @Override
    @Transactional
    public AiRecommendation reject(UUID id, UUID actorUserId, String requestId, String ipAddress) {
        return changeStatus(id, actorUserId, requestId, ipAddress, "ai.recommendation.reject", false);
    }

    @Override
    @Transactional
    public AiRecommendation feedback(UUID id, String feedback, UUID actorUserId, String requestId, String ipAddress) {
        if (feedback == null || feedback.isBlank() || feedback.length() > 2000) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                    "feedback must be between 1 and 2000 characters");
        }
        var current = findById(id);
        AiRecommendation updated;
        try {
            updated = current.withFeedback(feedback.trim());
        } catch (InvalidRecommendationStatusTransitionException exception) {
            throw new ApiException(ErrorCode.INVALID_AI_STATUS, HttpStatus.CONFLICT, exception.getMessage());
        }
        recommendations.save(updated);
        audit.record(new AuditEvent(UUID.randomUUID(), actorUserId, "ai.recommendation.feedback",
                "AI_RECOMMENDATION", id, current.snapshot(), updated.snapshot(), requestId, ipAddress, clock.instant()));
        return updated;
    }

    private AiRecommendation changeStatus(
            UUID id, UUID actorUserId, String requestId, String ipAddress, String action, boolean approve) {
        var current = findById(id);
        AiRecommendation updated;
        try {
            updated = approve ? current.approve() : current.reject();
        } catch (InvalidRecommendationStatusTransitionException exception) {
            throw new ApiException(ErrorCode.INVALID_AI_STATUS, HttpStatus.CONFLICT, exception.getMessage());
        }
        recommendations.save(updated);
        audit.record(new AuditEvent(UUID.randomUUID(), actorUserId, action,
                "AI_RECOMMENDATION", id, current.snapshot(), updated.snapshot(), requestId, ipAddress, clock.instant()));
        return updated;
    }

    private static void validateResponse(BriefResponse response, List<BriefRisk> selected, String promptVersion) {
        if (!promptVersion.equals(response.promptVersion())) {
            throw new AiClientException("AI prompt version mismatch", "AI_INVALID_RESPONSE", false);
        }
        Set<UUID> allowed = selected.stream().map(BriefRisk::id).collect(java.util.stream.Collectors.toSet());
        Set<UUID> referenced = new HashSet<>(response.riskEventIds());
        if (referenced.size() != response.riskEventIds().size()
                || referenced.size() > selected.size()
                || response.actions().size() > selected.size()
                || response.messageDrafts().size() > selected.size() * 2
                || !allowed.containsAll(referenced)) {
            throw new AiClientException("AI response referenced an unknown risk", "AI_HALLUCINATED_RISK", false);
        }
        if (response.actions().stream().anyMatch(action -> !allowed.contains(action.riskEventId()))) {
            throw new AiClientException("AI action referenced an unknown risk", "AI_HALLUCINATED_RISK", false);
        }
    }

    private static String sanitizeErrorClass(String value) {
        if (value == null || value.isBlank()) return "AI_PROVIDER_FAILURE";
        return value.replaceAll("[^A-Za-z0-9_.-]", "_").substring(0, Math.min(120, value.length()));
    }
}
