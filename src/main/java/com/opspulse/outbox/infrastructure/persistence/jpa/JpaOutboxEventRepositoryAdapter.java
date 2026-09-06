package com.opspulse.outbox.infrastructure.persistence.jpa;

import com.opspulse.outbox.application.port.out.OutboxEventRepository;
import com.opspulse.outbox.domain.OutboxEvent;
import org.springframework.stereotype.Repository;

@Repository
class JpaOutboxEventRepositoryAdapter implements OutboxEventRepository {

    private final SpringDataOutboxEventRepository repository;

    JpaOutboxEventRepositoryAdapter(SpringDataOutboxEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(OutboxEvent event) {
        var entity = new OutboxEventJpaEntity();
        entity.id = event.id();
        entity.aggregateType = event.aggregateType();
        entity.aggregateId = event.aggregateId();
        entity.eventType = event.eventType();
        entity.payload = event.payload();
        entity.status = "NEW";
        entity.retryCount = 0;
        entity.nextAttemptAt = event.createdAt();
        entity.createdAt = event.createdAt();
        repository.save(entity);
    }
}
