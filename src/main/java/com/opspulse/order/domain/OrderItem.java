package com.opspulse.order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

public record OrderItem(
        UUID id,
        UUID productId,
        BigDecimal quantity,
        BigDecimal unitPrice) {

    public OrderItem {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (quantity.signum() <= 0 || quantity.scale() > 3) {
            throw new IllegalArgumentException("quantity must be positive with at most 3 decimals");
        }
        if (unitPrice.signum() < 0 || unitPrice.scale() > 4) {
            throw new IllegalArgumentException("unitPrice must be nonnegative with at most 4 decimals");
        }
        quantity = quantity.stripTrailingZeros();
        unitPrice = unitPrice.stripTrailingZeros();
    }

    public BigDecimal lineTotal() {
        return quantity.multiply(unitPrice).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
    }
}
