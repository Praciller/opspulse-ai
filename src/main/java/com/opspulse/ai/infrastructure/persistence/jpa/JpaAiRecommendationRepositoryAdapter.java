package com.opspulse.ai.infrastructure.persistence.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.opspulse.ai.application.port.out.AiRecommendationRepository;
import com.opspulse.ai.domain.AiRecommendation;
import com.opspulse.ai.domain.BriefAction;
import com.opspulse.ai.domain.MessageDraft;
import com.opspulse.ai.domain.CreatedByType;
import com.opspulse.ai.domain.GeneratedBy;
import com.opspulse.ai.domain.RecommendationStatus;
import com.opspulse.shared.web.PagedResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaAiRecommendationRepositoryAdapter implements AiRecommendationRepository {

    private static final TypeReference<java.util.Map<String, Object>> JSON_MAP = new TypeReference<>() {};

    private static final java.util.Set<String> SORT_FIELDS = java.util.Set.of("createdAt", "status", "generatedBy", "modelProviderName");

    private final SpringDataAiRecommendationRepository recommendations;
    private final SpringDataAiRecommendationItemRepository items;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    JpaAiRecommendationRepositoryAdapter(
            SpringDataAiRecommendationRepository recommendations,
            SpringDataAiRecommendationItemRepository items,
            ObjectMapper objectMapper,
            EntityManager entityManager) {
        this.recommendations = recommendations;
        this.items = items;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public AiRecommendation save(AiRecommendation recommendation) {
        var entity = toEntity(recommendation);
        recommendations.saveAndFlush(entity);
        items.deleteAllByRecommendationId(recommendation.id());
        entityManager.flush();
        var itemEntities = recommendation.inputRiskEventIds().stream().map(riskId -> {
            var item = new AiRecommendationItemJpaEntity();
            item.id = UUID.randomUUID();
            item.recommendationId = recommendation.id();
            item.riskEventId = riskId;
            item.role = "top5";
            return item;
        }).toList();
        items.saveAll(itemEntities);
        return toDomain(entity, itemEntities);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiRecommendation> findById(UUID id) {
        return recommendations.findById(id).map(entity ->
                toDomain(entity, items.findAllByRecommendationIdOrderById(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AiRecommendation> findAll(
            int page, int size, RecommendationStatus status, String sortField, boolean ascending) {
        String field = SORT_FIELDS.contains(sortField) ? sortField : "createdAt";
        var direction = ascending ? Sort.Direction.ASC : Sort.Direction.DESC;
        var pageable = PageRequest.of(page, size, Sort.by(direction, field));
        Page<AiRecommendationJpaEntity> result = status == null
                ? recommendations.findAll(pageable)
                : recommendations.findAllByStatus(status.name(), pageable);
        var values = result.getContent().stream()
                .map(entity -> toDomain(entity, items.findAllByRecommendationIdOrderById(entity.id)))
                .toList();
        return new PagedResponse<>(values, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private AiRecommendationJpaEntity toEntity(AiRecommendation value) {
        var entity = new AiRecommendationJpaEntity();
        entity.id = value.id();
        entity.promptVersion = value.promptVersion();
        entity.modelProviderName = value.modelProviderName();
        entity.generatedBy = value.generatedBy().name();
        entity.generatedSummary = value.generatedSummary();
        entity.generatedActions = value.generatedActions().stream().map(action -> objectMapper.convertValue(action, JSON_MAP)).toList();
        entity.generatedMessageDrafts = value.generatedMessageDrafts().stream().map(draft -> objectMapper.convertValue(draft, JSON_MAP)).toList();
        entity.confidence = value.confidence();
        entity.status = value.status().name();
        entity.userFeedback = value.userFeedback();
        entity.createdAt = value.createdAt();
        entity.createdBy = value.createdBy();
        entity.createdByType = value.createdByType().name();
        entity.organizationId = value.organizationId();
        return entity;
    }

    private AiRecommendation toDomain(AiRecommendationJpaEntity entity, List<AiRecommendationItemJpaEntity> itemEntities) {
        var actions = entity.generatedActions == null ? List.<BriefAction>of()
                : entity.generatedActions.stream().map(value -> objectMapper.convertValue(value, BriefAction.class)).toList();
        var drafts = entity.generatedMessageDrafts == null ? List.<MessageDraft>of()
                : entity.generatedMessageDrafts.stream().map(value -> objectMapper.convertValue(value, MessageDraft.class)).toList();
        var riskIds = itemEntities.stream().map(item -> item.riskEventId).filter(java.util.Objects::nonNull).toList();
        return new AiRecommendation(entity.id, entity.promptVersion, entity.modelProviderName,
                GeneratedBy.valueOf(entity.generatedBy), entity.generatedSummary, actions, drafts,
                entity.confidence, RecommendationStatus.valueOf(entity.status), entity.userFeedback,
                entity.createdAt, entity.createdBy, CreatedByType.valueOf(entity.createdByType),
                entity.organizationId, riskIds);
    }
}
