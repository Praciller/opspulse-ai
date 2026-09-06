package com.opspulse.product.infrastructure.persistence.jpa;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

interface SpringDataProductRepository extends JpaRepository<ProductJpaEntity, UUID> {

    boolean existsBySkuIgnoreCase(String sku);

    Page<ProductJpaEntity> findAllBySkuContainingIgnoreCaseOrNameContainingIgnoreCase(
            String sku, String name, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductJpaEntity p where p.id = :id")
    java.util.Optional<ProductJpaEntity> findByIdForUpdate(UUID id);

    @Query(value = "select exists(select 1 from inventory_movements where product_id = :id)", nativeQuery = true)
    boolean hasInventoryMovements(UUID id);
}
