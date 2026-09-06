package com.opspulse.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record InventoryMovement(
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
        UUID createdBy,
        String requestId) {

    public InventoryMovement {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(movementType, "movementType must not be null");
        quantity = quantity(quantity, "quantity");
        balanceBefore = quantity(balanceBefore, "balanceBefore");
        balanceAfter = quantity(balanceAfter, "balanceAfter");
        if (quantity.signum() == 0 || balanceBefore.add(quantity).compareTo(balanceAfter) != 0) {
            throw new IllegalArgumentException("inventory balances are inconsistent");
        }
        if ((referenceType == null) != (referenceId == null)) {
            throw new IllegalArgumentException("referenceType and referenceId must be supplied together");
        }
        reason = optional(reason, 200, "reason");
        referenceType = optional(referenceType, 40, "referenceType");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null");
    }

    public Map<String, Object> snapshot() {
        var value = new LinkedHashMap<String, Object>();
        value.put("movementId", id.toString());
        value.put("movementType", movementType.name());
        value.put("quantity", quantity);
        value.put("balanceBefore", balanceBefore);
        value.put("balanceAfter", balanceAfter);
        value.put("reason", reason);
        value.put("referenceType", referenceType);
        value.put("referenceId", referenceId == null ? null : referenceId.toString());
        return value;
    }

    private static BigDecimal quantity(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.scale() > 3 || value.precision() - value.scale() > 15) {
            throw new IllegalArgumentException(field + " exceeds NUMERIC(18,3)");
        }
        return value.stripTrailingZeros();
    }

    private static String optional(String raw, int maxLength, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " is too long");
        }
        return value;
    }
}
