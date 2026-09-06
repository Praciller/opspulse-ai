package com.opspulse.inventory.api.dto;

import com.opspulse.inventory.domain.InventoryMovement;
import com.opspulse.inventory.domain.MovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryMovementResponse(
        UUID id,
        UUID productId,
        MovementType movementType,
        BigDecimal quantity,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String reason,
        String referenceType,
        UUID referenceId,
        Instant createdAt,
        UUID createdBy) {

    public static InventoryMovementResponse from(InventoryMovement movement) {
        return new InventoryMovementResponse(movement.id(), movement.productId(), movement.movementType(), movement.quantity(),
                movement.balanceBefore(), movement.balanceAfter(), movement.reason(), movement.referenceType(), movement.referenceId(),
                movement.createdAt(), movement.createdBy());
    }
}
