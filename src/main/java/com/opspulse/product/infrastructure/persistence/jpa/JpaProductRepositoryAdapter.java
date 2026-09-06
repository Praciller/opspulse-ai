package com.opspulse.product.infrastructure.persistence.jpa;

import com.opspulse.product.application.DuplicateSkuException;
import com.opspulse.product.application.port.out.ProductRepository;
import com.opspulse.product.application.ProductVersionConflictException;
import com.opspulse.product.domain.Product;
import com.opspulse.shared.web.PagedResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class JpaProductRepositoryAdapter implements ProductRepository {

    private final SpringDataProductRepository repository;

    JpaProductRepositoryAdapter(SpringDataProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsBySku(String sku) {
        return repository.existsBySkuIgnoreCase(sku);
    }

    @Override
    public Product save(Product product) {
        try {
            return toDomain(repository.saveAndFlush(toEntity(product)));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateSkuException();
        } catch (OptimisticLockingFailureException exception) {
            throw new ProductVersionConflictException();
        }
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return repository.findById(id).map(JpaProductRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Product> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(JpaProductRepositoryAdapter::toDomain);
    }

    @Override
    public boolean hasInventoryMovements(UUID id) {
        return repository.hasInventoryMovements(id);
    }

    @Override
    public PagedResponse<Product> findAll(
            int page, int size, String search, String sortField, boolean ascending) {
        var direction = ascending ? Sort.Direction.ASC : Sort.Direction.DESC;
        var pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<ProductJpaEntity> result = search == null || search.isBlank()
                ? repository.findAll(pageable)
                : repository.findAllBySkuContainingIgnoreCaseOrNameContainingIgnoreCase(
                        search.trim(), search.trim(), pageable);
        return new PagedResponse<>(
                result.getContent().stream().map(JpaProductRepositoryAdapter::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private static ProductJpaEntity toEntity(Product product) {
        var entity = new ProductJpaEntity();
        entity.id = product.id();
        entity.sku = product.sku();
        entity.name = product.name();
        entity.category = product.category();
        entity.unit = product.unit();
        entity.currentStock = product.currentStock();
        entity.safetyStock = product.safetyStock();
        entity.reorderPoint = product.reorderPoint();
        entity.cost = product.cost();
        entity.sellingPrice = product.sellingPrice();
        entity.active = product.active();
        entity.organizationId = product.organizationId();
        entity.createdAt = product.createdAt();
        entity.createdBy = product.createdBy();
        entity.updatedAt = product.updatedAt();
        entity.updatedBy = product.updatedBy();
        entity.version = product.version();
        return entity;
    }

    private static Product toDomain(ProductJpaEntity entity) {
        return new Product(
                entity.id,
                entity.sku,
                entity.name,
                entity.category,
                entity.unit,
                entity.currentStock,
                entity.safetyStock,
                entity.reorderPoint,
                entity.cost,
                entity.sellingPrice,
                entity.active,
                entity.organizationId,
                entity.createdAt,
                entity.createdBy,
                entity.updatedAt,
                entity.updatedBy,
                entity.version);
    }
}
