package com.opspulse.supplier.infrastructure.persistence.jpa;

import com.opspulse.shared.web.PagedResponse;
import com.opspulse.supplier.application.port.out.SupplierRepository;
import com.opspulse.supplier.domain.Supplier;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class JpaSupplierRepositoryAdapter implements SupplierRepository {

    private final SpringDataSupplierRepository repository;

    JpaSupplierRepositoryAdapter(SpringDataSupplierRepository repository) {
        this.repository = repository;
    }

    @Override public Supplier save(Supplier supplier) {
        return toDomain(repository.saveAndFlush(toEntity(supplier)));
    }

    @Override public Optional<Supplier> findById(UUID id) {
        return repository.findById(id).map(JpaSupplierRepositoryAdapter::toDomain);
    }

    @Override
    public PagedResponse<Supplier> findAll(
            int page, int size, String search, String sortField, boolean ascending) {
        var pageable = PageRequest.of(
                page, size, Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, sortField));
        Page<SupplierJpaEntity> result = search == null || search.isBlank()
                ? repository.findAll(pageable)
                : repository.findAllByNameContainingIgnoreCase(search.trim(), pageable);
        return new PagedResponse<>(
                result.getContent().stream().map(JpaSupplierRepositoryAdapter::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private static SupplierJpaEntity toEntity(Supplier supplier) {
        var entity = new SupplierJpaEntity();
        entity.id = supplier.id(); entity.name = supplier.name(); entity.contactInfo = supplier.contactInfo();
        entity.averageLeadTimeDays = supplier.averageLeadTimeDays(); entity.expectedSlaDays = supplier.expectedSlaDays();
        entity.active = supplier.active(); entity.organizationId = supplier.organizationId();
        entity.createdAt = supplier.createdAt(); entity.createdBy = supplier.createdBy();
        entity.updatedAt = supplier.updatedAt(); entity.updatedBy = supplier.updatedBy(); entity.version = supplier.version();
        return entity;
    }

    private static Supplier toDomain(SupplierJpaEntity entity) {
        return new Supplier(
                entity.id, entity.name, entity.contactInfo, entity.averageLeadTimeDays,
                entity.expectedSlaDays, entity.active, entity.organizationId,
                entity.createdAt, entity.createdBy, entity.updatedAt, entity.updatedBy, entity.version);
    }
}
