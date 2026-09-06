package com.opspulse.supplier.infrastructure.persistence.jpa;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSupplierRepository extends JpaRepository<SupplierJpaEntity, UUID> {
    Page<SupplierJpaEntity> findAllByNameContainingIgnoreCase(String search, Pageable pageable);
}
