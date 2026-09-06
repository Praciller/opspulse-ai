package com.opspulse.inventory.domain;

import java.math.BigDecimal;

public enum MovementType {
    INBOUND,
    OUTBOUND,
    ADJUSTMENT,
    RETURN;

    public BigDecimal signedQuantity(BigDecimal requestedQuantity) {
        if (requestedQuantity == null || requestedQuantity.signum() == 0) {
            throw new IllegalArgumentException("quantity must not be zero");
        }
        return switch (this) {
            case INBOUND, RETURN -> requirePositive(requestedQuantity);
            case OUTBOUND -> requirePositive(requestedQuantity).negate();
            case ADJUSTMENT -> requestedQuantity;
        };
    }

    private static BigDecimal requirePositive(BigDecimal quantity) {
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("quantity must be a positive magnitude");
        }
        return quantity;
    }
}
