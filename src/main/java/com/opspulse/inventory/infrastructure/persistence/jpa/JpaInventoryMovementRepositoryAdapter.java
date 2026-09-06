package com.opspulse.inventory.infrastructure.persistence.jpa;

import com.opspulse.inventory.application.port.out.InventoryMovementRepository;
import com.opspulse.inventory.domain.InventoryMovement;
import com.opspulse.inventory.domain.MovementType;
import com.opspulse.shared.web.PagedResponse;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class JpaInventoryMovementRepositoryAdapter implements InventoryMovementRepository {

    private final SpringDataInventoryMovementRepository repository;

    JpaInventoryMovementRepositoryAdapter(SpringDataInventoryMovementRepository repository) {
        this.repository = repository;
    }

    @Override
    public InventoryMovement save(InventoryMovement movement) {
        return toDomain(repository.saveAndFlush(toEntity(movement)));
    }

    @Override
    public PagedResponse<InventoryMovement> findAll(int page, int size, UUID productId, MovementType type, String sortField, boolean ascending) {
        var direction = ascending ? Sort.Direction.ASC : Sort.Direction.DESC;
        var result = repository.search(productId, type == null ? null : type.name(), PageRequest.of(page, size, Sort.by(direction, sortField)));
        return new PagedResponse<>(result.getContent().stream().map(JpaInventoryMovementRepositoryAdapter::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private static InventoryMovementJpaEntity toEntity(InventoryMovement movement) {
        var entity = new InventoryMovementJpaEntity();
        entity.id = movement.id();
        entity.productId = movement.productId();
        entity.movementType = movement.movementType().name();
        entity.quantity = movement.quantity();
        entity.balanceBefore = movement.balanceBefore();
        entity.balanceAfter = movement.balanceAfter();
        entity.reason = movement.reason();
        entity.referenceType = movement.referenceType();
        entity.referenceId = movement.referenceId();
        entity.createdAt = movement.createdAt();
        entity.createdBy = movement.createdBy();
        entity.requestId = movement.requestId();
        return entity;
    }

    private static InventoryMovement toDomain(InventoryMovementJpaEntity entity) {
        return new InventoryMovement(entity.id, entity.productId, MovementType.valueOf(entity.movementType), entity.quantity,
                entity.balanceBefore, entity.balanceAfter, entity.reason, entity.referenceType, entity.referenceId,
                entity.createdAt, entity.createdBy, entity.requestId);
    }
}
