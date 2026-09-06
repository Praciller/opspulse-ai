package com.opspulse.purchase.infrastructure.persistence.jpa;

import com.opspulse.purchase.application.PurchaseOrderPersistenceException;
import com.opspulse.purchase.application.port.out.PurchaseOrderRepository;
import com.opspulse.purchase.domain.PurchaseOrder;
import com.opspulse.purchase.domain.PurchaseOrderItem;
import com.opspulse.purchase.domain.PurchaseOrderStatus;
import com.opspulse.shared.web.PagedResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class JpaPurchaseOrderRepositoryAdapter implements PurchaseOrderRepository {
    private final SpringDataPurchaseOrderRepository repository;
    JpaPurchaseOrderRepositoryAdapter(SpringDataPurchaseOrderRepository repository) { this.repository = repository; }

    @Override public boolean existsByPoNumber(String number) { return repository.existsByPoNumberIgnoreCase(number); }
    @Override public PurchaseOrder save(PurchaseOrder order) {
        try { return toDomain(repository.saveAndFlush(toEntity(order))); }
        catch (DataIntegrityViolationException exception) { throw new PurchaseOrderPersistenceException(PurchaseOrderPersistenceException.Reason.DUPLICATE_NUMBER); }
        catch (OptimisticLockingFailureException exception) { throw new PurchaseOrderPersistenceException(PurchaseOrderPersistenceException.Reason.STALE_VERSION); }
    }
    @Override public Optional<PurchaseOrder> findById(UUID id) { return repository.findById(id).map(JpaPurchaseOrderRepositoryAdapter::toDomain); }
    @Override public Optional<PurchaseOrder> findByIdForUpdate(UUID id) { return repository.findByIdForUpdate(id).map(JpaPurchaseOrderRepositoryAdapter::toDomain); }
    @Override public PagedResponse<PurchaseOrder> findAll(int page, int size, UUID supplierId, PurchaseOrderStatus status, String sortField, boolean ascending) {
        var result = repository.search(supplierId, status == null ? null : status.name(), PageRequest.of(page, size, Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, sortField)));
        return new PagedResponse<>(result.getContent().stream().map(JpaPurchaseOrderRepositoryAdapter::toDomain).toList(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private static PurchaseOrderJpaEntity toEntity(PurchaseOrder order) {
        var entity = new PurchaseOrderJpaEntity();
        entity.id = order.id(); entity.poNumber = order.poNumber(); entity.supplierId = order.supplierId(); entity.status = order.status().name();
        entity.expectedDeliveryDate = order.expectedDeliveryDate(); entity.actualDeliveryDate = order.actualDeliveryDate(); entity.organizationId = order.organizationId();
        entity.createdAt = order.createdAt(); entity.createdBy = order.createdBy(); entity.updatedAt = order.updatedAt(); entity.updatedBy = order.updatedBy(); entity.version = order.version();
        entity.items = order.items().stream().map(item -> { var child = new PurchaseOrderItemJpaEntity(); child.id = item.id(); child.purchaseOrder = entity;
            child.productId = item.productId(); child.quantity = item.quantity(); child.unitCost = item.unitCost(); child.receivedQuantity = item.receivedQuantity(); return child; }).toList();
        return entity;
    }
    private static PurchaseOrder toDomain(PurchaseOrderJpaEntity entity) {
        return new PurchaseOrder(entity.id, entity.poNumber, entity.supplierId, PurchaseOrderStatus.valueOf(entity.status), entity.expectedDeliveryDate, entity.actualDeliveryDate,
                entity.items.stream().map(item -> new PurchaseOrderItem(item.id, item.productId, item.quantity, item.unitCost, item.receivedQuantity)).toList(),
                entity.organizationId, entity.createdAt, entity.createdBy, entity.updatedAt, entity.updatedBy, entity.version);
    }
}
