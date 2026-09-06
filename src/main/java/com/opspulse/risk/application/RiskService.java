package com.opspulse.risk.application;

import com.opspulse.audit.application.port.in.RecordAuditEventUseCase;
import com.opspulse.audit.domain.AuditEvent;
import com.opspulse.outbox.application.port.in.OutboxPublisher;
import com.opspulse.risk.application.port.in.RiskUseCase;
import com.opspulse.risk.application.port.out.MetricRepository;
import com.opspulse.risk.application.port.out.AppConfigProvider;
import com.opspulse.risk.application.port.out.RiskEventRepository;
import com.opspulse.risk.domain.InvalidRiskStatusTransitionException;
import com.opspulse.risk.domain.RiskContext;
import com.opspulse.risk.domain.RiskEntityType;
import com.opspulse.risk.domain.RiskEvent;
import com.opspulse.risk.domain.RiskFinding;
import com.opspulse.risk.domain.RiskSeverity;
import com.opspulse.risk.domain.RiskStatus;
import com.opspulse.risk.domain.RiskType;
import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import com.opspulse.shared.observability.RequestIdContext;
import com.opspulse.shared.web.PagedResponse;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
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
public class RiskService implements RiskUseCase {

    private static final Logger log = LoggerFactory.getLogger(RiskService.class);

    private final RiskEventRepository events;
    private final MetricRepository metrics;
    private final AppConfigProvider configuration;
    private final RiskEngine engine;
    private final OutboxPublisher outbox;
    private final RecordAuditEventUseCase audit;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public RiskService(
            RiskEventRepository events,
            MetricRepository metrics,
            AppConfigProvider configuration,
            RiskEngine engine,
            OutboxPublisher outbox,
            RecordAuditEventUseCase audit,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.events = events;
        this.metrics = metrics;
        this.configuration = configuration;
        this.engine = engine;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<RiskEvent> findAll(
            int page, int size, RiskType riskType, RiskSeverity severity, RiskStatus status,
            RiskEntityType entityType, UUID entityId, Instant from, Instant to,
            String sortField, boolean ascending) {
        return events.findAll(page, size, riskType, severity, status, entityType, entityId,
                from, to, sortField, ascending);
    }

    @Override
    @Transactional(readOnly = true)
    public RiskEvent findById(UUID id) {
        return events.findById(id).orElseThrow(() -> new ApiException(
                ErrorCode.RISK_NOT_FOUND, HttpStatus.NOT_FOUND, "Risk event not found"));
    }

    @Override
    @Transactional
    public RiskScanResult scan() {
        Instant started = clock.instant();
        String systemRequestId = "system-risk-scan-" + UUID.randomUUID();
        var config = configuration.current();
        var snapshot = metrics.load(started);
        var contexts = new ArrayList<RiskContext>();
        snapshot.products().forEach(value -> contexts.add(new RiskContext(value, null, null, config, started)));
        snapshot.orders().forEach(value -> contexts.add(new RiskContext(null, value, null, config, started)));
        snapshot.suppliers().forEach(value -> contexts.add(new RiskContext(null, null, value, config, started)));

        var findings = engine.evaluate(contexts);
        var activeKeys = new HashSet<String>();
        int created = 0;
        for (var finding : findings) {
            var candidate = RiskEvent.open(
                    UUID.randomUUID(), finding.riskType(), finding.severity(), finding.entityType(), finding.entityId(),
                    withEvaluationMetadata(finding.sourceMetrics(), started), finding.explanation(),
                    finding.recommendedAction(), null, started);
            activeKeys.add(candidate.dedupKey());
            var active = events.findActiveByDedupKey(candidate.dedupKey());
            if (active.isPresent()) {
                var current = active.get();
                var refreshed = current.refresh(candidate.severity(), candidate.sourceMetrics(),
                        candidate.explanation(), candidate.recommendedAction());
                if (!current.equals(refreshed)) {
                    events.save(refreshed);
                }
            } else {
                events.save(candidate);
                outbox.publish("RISK", candidate.entityId(), "opspulse.risk.created", java.util.Map.of(
                        "riskEventId", candidate.id().toString(),
                        "riskType", candidate.riskType().name(),
                        "severity", candidate.severity().name(),
                        "entityType", candidate.entityType().name(),
                        "entityId", candidate.entityId().toString()));
                created++;
            }
        }

        int resolved = 0;
        for (var active : events.findActive()) {
            if (!activeKeys.contains(active.dedupKey())) {
                var resolvedEvent = active.systemResolve(started);
                events.save(resolvedEvent);
                audit.record(new AuditEvent(
                        UUID.randomUUID(), null, "risk.resolve", "RISK_EVENT", resolvedEvent.id(),
                        active.snapshot(), resolvedEvent.snapshot(), systemRequestId, "system", started));
                outbox.publish("RISK", resolvedEvent.entityId(), "opspulse.risk.resolved", java.util.Map.of(
                        "riskEventId", resolvedEvent.id().toString(),
                        "resolver", "SYSTEM",
                        "status", resolvedEvent.status().name()));
                resolved++;
            }
        }
        meterRegistry.counter("opspulse.risk.scan.created").increment(created);
        meterRegistry.counter("opspulse.risk.scan.resolved").increment(resolved);
        meterRegistry.counter("opspulse.risk.scan.evaluated").increment(snapshot.evaluatedEntities());
        meterRegistry.timer("opspulse.risk.scan.duration").record(java.time.Duration.between(started, clock.instant()));
        log.info("risk scan completed evaluatedEntities={} created={} resolved={}",
                snapshot.evaluatedEntities(), created, resolved);
        return new RiskScanResult(snapshot.evaluatedEntities(), created, clock.instant());
    }

    @Override
    @Transactional
    public RiskEvent acknowledge(UUID id, UUID actorUserId, String requestId, String ipAddress) {
        return changeStatus(id, actorUserId, requestId, ipAddress, "risk.acknowledge", RiskStatus.ACKNOWLEDGED);
    }

    @Override
    @Transactional
    public RiskEvent resolve(UUID id, UUID actorUserId, String requestId, String ipAddress) {
        return changeStatus(id, actorUserId, requestId, ipAddress, "risk.resolve", RiskStatus.RESOLVED);
    }

    @Override
    @Transactional
    public RiskEvent dismiss(UUID id, UUID actorUserId, String requestId, String ipAddress) {
        return changeStatus(id, actorUserId, requestId, ipAddress, "risk.dismiss", RiskStatus.DISMISSED);
    }

    private RiskEvent changeStatus(
            UUID id, UUID actorUserId, String requestId, String ipAddress,
            String action, RiskStatus target) {
        var current = findById(id);
        RiskEvent updated;
        try {
            updated = switch (target) {
                case ACKNOWLEDGED -> current.acknowledge();
                case RESOLVED -> current.resolve(clock.instant(), actorUserId);
                case DISMISSED -> current.dismiss(clock.instant(), actorUserId);
                default -> throw new IllegalArgumentException("unsupported risk action");
            };
        } catch (InvalidRiskStatusTransitionException exception) {
            throw new ApiException(ErrorCode.INVALID_RISK_STATUS, HttpStatus.CONFLICT, exception.getMessage());
        }
        events.save(updated);
        audit.record(new AuditEvent(
                UUID.randomUUID(), actorUserId, action, "RISK_EVENT", id,
                current.snapshot(), updated.snapshot(), requestId, ipAddress, clock.instant()));
        if (target == RiskStatus.RESOLVED || target == RiskStatus.DISMISSED) {
            outbox.publish("RISK", updated.entityId(), "opspulse.risk.resolved", java.util.Map.of(
                    "riskEventId", updated.id().toString(),
                    "resolver", actorUserId.toString(),
                    "status", updated.status().name()));
        }
        return updated;
    }

    private static java.util.Map<String, Object> withEvaluationMetadata(
            java.util.Map<String, Object> metrics, Instant evaluatedAt) {
        var result = new java.util.LinkedHashMap<>(metrics);
        result.putIfAbsent("evaluatedAt", evaluatedAt.toString());
        return result;
    }
}
