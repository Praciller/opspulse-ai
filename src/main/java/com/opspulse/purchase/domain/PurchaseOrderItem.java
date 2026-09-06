package com.opspulse.purchase.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.math.RoundingMode;

public record PurchaseOrderItem(
        UUID id,
        UUID productId,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal receivedQuantity) {

    public PurchaseOrderItem {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        quantity = quantity(quantity, "quantity");
        unitCost = money(unitCost);
        receivedQuantity = quantity(receivedQuantity, "receivedQuantity");
        if (quantity.signum() <= 0 || unitCost.signum() < 0 || receivedQuantity.signum() < 0 || receivedQuantity.compareTo(quantity) > 0) {
            throw new IllegalArgumentException("purchase order item quantities are invalid");
        }
    }

    public PurchaseOrderItem receive(BigDecimal amount) {
        BigDecimal updated = receivedQuantity.add(quantity(amount, "received quantity"));
        if (amount.signum() <= 0 || updated.compareTo(quantity) > 0) {
            throw new IllegalArgumentException("received quantity exceeds outstanding quantity");
        }
        return new PurchaseOrderItem(id, productId, quantity, unitCost, updated);
    }

    public boolean fullyReceived() {
        return receivedQuantity.compareTo(quantity) == 0;
    }

    public BigDecimal lineTotal() {
        return quantity.multiply(unitCost).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static BigDecimal quantity(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.scale() > 3 || value.precision() - value.scale() > 15) {
            throw new IllegalArgumentException(field + " exceeds NUMERIC(18,3)");
        }
        return value.stripTrailingZeros();
    }

    private static BigDecimal money(BigDecimal value) {
        Objects.requireNonNull(value, "unitCost must not be null");
        if (value.scale() > 4 || value.precision() - value.scale() > 14) {
            throw new IllegalArgumentException("unitCost exceeds NUMERIC(18,4)");
        }
        return value.stripTrailingZeros();
    }
}
