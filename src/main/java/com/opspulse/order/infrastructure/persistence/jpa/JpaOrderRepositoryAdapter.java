package com.opspulse.order.infrastructure.persistence.jpa;

import com.opspulse.order.application.OrderPersistenceException;
import com.opspulse.order.application.port.out.OrderRepository;
import com.opspulse.order.domain.Order;
import com.opspulse.order.domain.OrderItem;
import com.opspulse.order.domain.OrderStatus;
import com.opspulse.shared.web.PagedResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class JpaOrderRepositoryAdapter implements OrderRepository {

    private final SpringDataOrderRepository repository;

    JpaOrderRepositoryAdapter(SpringDataOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByOrderNumber(String orderNumber) {
        return repository.existsByOrderNumberIgnoreCase(orderNumber);
    }

    @Override
    public Order save(Order order) {
        try {
            return toDomain(repository.saveAndFlush(toEntity(order)));
        } catch (DataIntegrityViolationException exception) {
            throw new OrderPersistenceException(OrderPersistenceException.Reason.DUPLICATE_NUMBER);
        } catch (OptimisticLockingFailureException exception) {
            throw new OrderPersistenceException(OrderPersistenceException.Reason.STALE_VERSION);
        }
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return repository.findById(id).map(JpaOrderRepositoryAdapter::toDomain);
    }

    @Override
    public PagedResponse<Order> findAll(int page, int size, String search, OrderStatus status, String sortField, boolean ascending) {
        var direction = ascending ? Sort.Direction.ASC : Sort.Direction.DESC;
        var result = repository.search(
                search == null || search.isBlank() ? null : search.trim(),
                status == null ? null : status.name(),
                PageRequest.of(page, size, Sort.by(direction, sortField)));
        return new PagedResponse<>(result.getContent().stream().map(JpaOrderRepositoryAdapter::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private static OrderJpaEntity toEntity(Order order) {
        var entity = new OrderJpaEntity();
        entity.id = order.id();
        entity.orderNumber = order.orderNumber();
        entity.customerName = order.customerName();
        entity.status = order.status().name();
        entity.expectedShipDate = order.expectedShipDate();
        entity.actualShipDate = order.actualShipDate();
        entity.totalAmount = order.totalAmount();
        entity.organizationId = order.organizationId();
        entity.createdAt = order.createdAt();
        entity.createdBy = order.createdBy();
        entity.updatedAt = order.updatedAt();
        entity.updatedBy = order.updatedBy();
        entity.version = order.version();
        entity.items = order.items().stream().map(item -> {
            var child = new OrderItemJpaEntity();
            child.id = item.id();
            child.order = entity;
            child.productId = item.productId();
            child.quantity = item.quantity();
            child.unitPrice = item.unitPrice();
            return child;
        }).toList();
        return entity;
    }

    private static Order toDomain(OrderJpaEntity entity) {
        return new Order(entity.id, entity.orderNumber, entity.customerName, OrderStatus.valueOf(entity.status),
                entity.expectedShipDate, entity.actualShipDate,
                entity.items.stream().map(item -> new OrderItem(item.id, item.productId, item.quantity, item.unitPrice)).toList(),
                entity.organizationId, entity.createdAt, entity.createdBy, entity.updatedAt, entity.updatedBy, entity.version);
    }
}
