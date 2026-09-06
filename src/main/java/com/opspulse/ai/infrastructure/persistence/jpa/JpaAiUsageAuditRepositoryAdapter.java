package com.opspulse.ai.infrastructure.persistence.jpa;

import com.opspulse.ai.application.port.out.AiUsageAuditRepository;
import com.opspulse.ai.domain.AiUsageAudit;
import org.springframework.stereotype.Repository;

@Repository
class JpaAiUsageAuditRepositoryAdapter implements AiUsageAuditRepository {
    private final SpringDataAiUsageAuditRepository repository;

    JpaAiUsageAuditRepositoryAdapter(SpringDataAiUsageAuditRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(AiUsageAudit audit) {
        var entity = new AiUsageAuditJpaEntity();
        entity.id = audit.id();
        entity.recommendationId = audit.recommendationId();
        entity.provider = audit.provider();
        entity.model = audit.model();
        entity.promptVersion = audit.promptVersion();
        entity.inputTokenCount = audit.inputTokenCount();
        entity.outputTokenCount = audit.outputTokenCount();
        entity.latencyMs = audit.latencyMs();
        entity.success = audit.success();
        entity.errorClass = audit.errorClass();
        entity.requestId = audit.requestId();
        entity.createdAt = audit.createdAt();
        repository.save(entity);
    }
}
