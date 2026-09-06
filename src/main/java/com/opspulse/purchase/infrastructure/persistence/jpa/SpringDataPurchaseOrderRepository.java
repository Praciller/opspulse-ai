package com.opspulse.purchase.infrastructure.persistence.jpa;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataPurchaseOrderRepository extends JpaRepository<PurchaseOrderJpaEntity, UUID> {
    boolean existsByPoNumberIgnoreCase(String number);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PurchaseOrderJpaEntity p where p.id = :id")
    Optional<PurchaseOrderJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select p from PurchaseOrderJpaEntity p
            where (:supplierId is null or p.supplierId = :supplierId)
              and (:status is null or p.status = :status)
            """)
    Page<PurchaseOrderJpaEntity> search(@Param("supplierId") UUID supplierId, @Param("status") String status, Pageable pageable);
}
