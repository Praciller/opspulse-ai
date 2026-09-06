package com.opspulse.inventory.infrastructure.persistence.jpa;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataInventoryMovementRepository extends JpaRepository<InventoryMovementJpaEntity, UUID> {

    @Query("""
            select m from InventoryMovementJpaEntity m
            where (:productId is null or m.productId = :productId)
              and (:type is null or m.movementType = :type)
            """)
    Page<InventoryMovementJpaEntity> search(
            @Param("productId") UUID productId,
            @Param("type") String type,
            Pageable pageable);
}
