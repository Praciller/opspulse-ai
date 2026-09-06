package com.opspulse.inventory.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_movements")
class InventoryMovementJpaEntity {

    @Id UUID id;
    @Column(name = "product_id", nullable = false) UUID productId;
    @Column(name = "movement_type", nullable = false, length = 16) String movementType;
    @Column(nullable = false, precision = 18, scale = 3) BigDecimal quantity;
    @Column(name = "balance_before", nullable = false, precision = 18, scale = 3) BigDecimal balanceBefore;
    @Column(name = "balance_after", nullable = false, precision = 18, scale = 3) BigDecimal balanceAfter;
    @Column(length = 200) String reason;
    @Column(name = "reference_type", length = 40) String referenceType;
    @Column(name = "reference_id") UUID referenceId;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "created_by", nullable = false) UUID createdBy;
    @Column(name = "request_id", length = 64) String requestId;

    protected InventoryMovementJpaEntity() {}
}
