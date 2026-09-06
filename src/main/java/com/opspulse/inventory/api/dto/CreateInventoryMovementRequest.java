package com.opspulse.inventory.api.dto;

import com.opspulse.inventory.domain.MovementType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateInventoryMovementRequest(
        @NotNull UUID productId,
        @NotNull MovementType movementType,
        @NotNull @Digits(integer = 15, fraction = 3) BigDecimal quantity,
        @Size(max = 200) String reason,
        @Size(max = 40) String referenceType,
        UUID referenceId) {}
