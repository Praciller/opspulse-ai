package com.opspulse.risk.infrastructure.persistence.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspulse.risk.application.port.out.RiskEventRepository;
import com.opspulse.risk.domain.RiskEntityType;
import com.opspulse.risk.domain.RiskEvent;
import com.opspulse.risk.domain.RiskSeverity;
import com.opspulse.risk.domain.RiskStatus;
import com.opspulse.risk.domain.RiskType;
import com.opspulse.shared.web.PagedResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class JpaRiskEventRepositoryAdapter implements RiskEventRepository {

    private static final List<String> ACTIVE = List.of("OPEN", "ACKNOWLEDGED");
    private static final java.util.Set<String> SORT_FIELDS = java.util.Set.of(
            "createdAt", "severity", "riskType", "status", "resolvedAt");

    private final SpringDataRiskEventRepository repository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    JpaRiskEventRepositoryAdapter(
            SpringDataRiskEventRepository repository,
            EntityManager entityManager,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<RiskEvent> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<RiskEvent> findActiveByDedupKey(String dedupKey) {
        return repository.findFirstByDedupKeyAndStatusInOrderByCreatedAtDesc(dedupKey, ACTIVE)
                .map(this::toDomain);
    }

    @Override
    public Optional<RiskEvent> findLatestByDedupKey(String dedupKey) {
        return repository.findFirstByDedupKeyOrderByCreatedAtDesc(dedupKey).map(this::toDomain);
    }

    @Override
    public List<RiskEvent> findActive() {
        return repository.findByStatusIn(ACTIVE).stream().map(this::toDomain).toList();
    }

    @Override
    public List<RiskEvent> findTopOpenForBrief(int limit) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(RiskEventJpaEntity.class);
        var root = query.from(RiskEventJpaEntity.class);
        var severityOrder = cb.selectCase(root.get("severity"))
                .when("CRITICAL", 0)
                .when("HIGH", 1)
                .when("MEDIUM", 2)
                .when("LOW", 3)
                .otherwise(4);
        query.where(root.get("status").in(ACTIVE));
        query.orderBy(cb.asc(severityOrder), cb.desc(root.get("createdAt")));
        return entityManager.createQuery(query)
                .setMaxResults(Math.max(0, limit))
                .getResultList()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public RiskEvent save(RiskEvent event) {
        try {
            return toDomain(repository.saveAndFlush(toEntity(event)));
        } catch (DataIntegrityViolationException exception) {
            throw exception;
        }
    }

    @Override
    public PagedResponse<RiskEvent> findAll(
            int page,
            int size,
            RiskType riskType,
            RiskSeverity severity,
            RiskStatus status,
            RiskEntityType entityType,
            UUID entityId,
            Instant from,
            Instant to,
            String sortField,
            boolean ascending) {
        String field = SORT_FIELDS.contains(sortField) ? sortField : "createdAt";
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(RiskEventJpaEntity.class);
        var root = query.from(RiskEventJpaEntity.class);
        var predicates = predicates(cb, root, riskType, severity, status, entityType, entityId, from, to);
        query.where(predicates.toArray(Predicate[]::new));
        var sortPath = root.get(field);
        query.orderBy(ascending ? cb.asc(sortPath) : cb.desc(sortPath));
        var typed = entityManager.createQuery(query);
        typed.setFirstResult(page * size);
        typed.setMaxResults(size);
        var items = typed.getResultList().stream().map(this::toDomain).toList();

        var countQuery = cb.createQuery(Long.class);
        var countRoot = countQuery.from(RiskEventJpaEntity.class);
        var countPredicates = predicates(cb, countRoot, riskType, severity, status, entityType, entityId, from, to);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(Predicate[]::new));
        long total = entityManager.createQuery(countQuery).getSingleResult();
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PagedResponse<>(items, page, size, total, totalPages);
    }

    private List<Predicate> predicates(
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Root<RiskEventJpaEntity> root,
            RiskType riskType,
            RiskSeverity severity,
            RiskStatus status,
            RiskEntityType entityType,
            UUID entityId,
            Instant from,
            Instant to) {
        var result = new ArrayList<Predicate>();
        if (riskType != null) result.add(cb.equal(root.get("riskType"), riskType.name()));
        if (severity != null) result.add(cb.equal(root.get("severity"), severity.name()));
        if (status != null) result.add(cb.equal(root.get("status"), status.name()));
        if (entityType != null) result.add(cb.equal(root.get("entityType"), entityType.name()));
        if (entityId != null) result.add(cb.equal(root.get("entityId"), entityId));
        if (from != null) result.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        if (to != null) result.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
        return result;
    }

    private RiskEventJpaEntity toEntity(RiskEvent event) {
        var entity = new RiskEventJpaEntity();
        entity.id = event.id();
        entity.riskType = event.riskType().name();
        entity.severity = event.severity().name();
        entity.entityType = event.entityType().name();
        entity.entityId = event.entityId();
        entity.sourceMetrics = event.sourceMetrics();
        entity.explanation = event.explanation();
        entity.recommendedAction = event.recommendedAction();
        entity.status = event.status().name();
        entity.createdAt = event.createdAt();
        entity.resolvedAt = event.resolvedAt();
        entity.resolvedBy = event.resolvedBy();
        entity.organizationId = event.organizationId();
        return entity;
    }

    private RiskEvent toDomain(RiskEventJpaEntity entity) {
        return new RiskEvent(
                entity.id,
                RiskType.valueOf(entity.riskType),
                RiskSeverity.valueOf(entity.severity),
                RiskEntityType.valueOf(entity.entityType),
                entity.entityId,
                entity.sourceMetrics,
                entity.explanation,
                entity.recommendedAction,
                RiskStatus.valueOf(entity.status),
                entity.createdAt,
                entity.resolvedAt,
                entity.resolvedBy,
                entity.organizationId);
    }
}
