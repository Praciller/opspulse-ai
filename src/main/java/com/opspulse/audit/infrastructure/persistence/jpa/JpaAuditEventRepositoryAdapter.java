package com.opspulse.audit.infrastructure.persistence.jpa;

import com.opspulse.audit.application.port.out.AuditEventRepository;
import com.opspulse.audit.domain.AuditEvent;
import com.opspulse.shared.web.PagedResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class JpaAuditEventRepositoryAdapter implements AuditEventRepository {

    private final SpringDataAuditLogRepository repository;

    JpaAuditEventRepositoryAdapter(SpringDataAuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(AuditEvent event) {
        var entity = new AuditLogJpaEntity();
        entity.id = event.id();
        entity.actorUserId = event.actorUserId();
        entity.action = event.action();
        entity.entityType = event.entityType();
        entity.entityId = event.entityId();
        entity.beforeSnapshot = event.beforeSnapshot();
        entity.afterSnapshot = event.afterSnapshot();
        entity.requestId = event.requestId();
        entity.ipAddress = event.ipAddress();
        entity.createdAt = event.createdAt();
        repository.save(entity);
    }

    @Override
    public PagedResponse<AuditEvent> findAll(
            int page, int size, String entityType, UUID entityId) {
        var pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLogJpaEntity> result;
        if (entityType != null && entityId != null) {
            result = repository.findAllByEntityTypeAndEntityId(entityType, entityId, pageable);
        } else if (entityType != null) {
            result = repository.findAllByEntityType(entityType, pageable);
        } else if (entityId != null) {
            result = repository.findAllByEntityId(entityId, pageable);
        } else {
            result = repository.findAll(pageable);
        }
        return new PagedResponse<>(
                result.getContent().stream().map(this::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Override
    public Optional<AuditEvent> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    private AuditEvent toDomain(AuditLogJpaEntity entity) {
        return new AuditEvent(
                entity.id,
                entity.actorUserId,
                entity.action,
                entity.entityType,
                entity.entityId,
                entity.beforeSnapshot,
                entity.afterSnapshot,
                entity.requestId,
                entity.ipAddress,
                entity.createdAt);
    }
}
