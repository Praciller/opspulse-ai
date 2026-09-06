package com.opspulse.product.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record Product(
        UUID id,
        String sku,
        String name,
        String category,
        String unit,
        BigDecimal currentStock,
        BigDecimal safetyStock,
        BigDecimal reorderPoint,
        BigDecimal cost,
        BigDecimal sellingPrice,
        boolean active,
        UUID organizationId,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy,
        long version) {

    private static final Pattern SKU_PATTERN = Pattern.compile("[A-Z0-9][A-Z0-9._-]{0,63}");

    public Product {
        Objects.requireNonNull(id, "id must not be null");
        sku = normalizeSku(sku);
        name = required(name, 200, "name");
        category = optional(category, 80, "category");
        unit = required(unit, 20, "unit").toUpperCase();
        currentStock = quantity(currentStock, "currentStock");
        safetyStock = nonnegative(quantity(safetyStock, "safetyStock"), "safetyStock");
        reorderPoint = nonnegative(quantity(reorderPoint, "reorderPoint"), "reorderPoint");
        cost = nonnegative(money(cost, "cost"), "cost");
        sellingPrice = nonnegative(money(sellingPrice, "sellingPrice"), "sellingPrice");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Product create(
            UUID id,
            String sku,
            String name,
            String category,
            String unit,
            BigDecimal currentStock,
            BigDecimal safetyStock,
            BigDecimal reorderPoint,
            BigDecimal cost,
            BigDecimal sellingPrice,
            UUID actorUserId,
            Instant now) {
        if (currentStock == null || currentStock.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("currentStock must be zero on product creation");
        }
        return new Product(
                id,
                sku,
                name,
                category,
                unit,
                currentStock,
                safetyStock,
                reorderPoint,
                cost,
                sellingPrice,
                true,
                null,
                now,
                actorUserId,
                now,
                actorUserId,
                0);
    }

    public Map<String, Object> snapshot() {
        var snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("sku", sku);
        snapshot.put("name", name);
        snapshot.put("category", category);
        snapshot.put("unit", unit);
        snapshot.put("currentStock", currentStock);
        snapshot.put("safetyStock", safetyStock);
        snapshot.put("reorderPoint", reorderPoint);
        snapshot.put("cost", cost);
        snapshot.put("sellingPrice", sellingPrice);
        snapshot.put("active", active);
        return snapshot;
    }

    public Product updateDetails(
            String name,
            String category,
            String unit,
            BigDecimal safetyStock,
            BigDecimal reorderPoint,
            BigDecimal cost,
            BigDecimal sellingPrice,
            UUID actorUserId,
            Instant now) {
        return new Product(
                id,
                sku,
                name,
                category,
                unit,
                currentStock,
                safetyStock,
                reorderPoint,
                cost,
                sellingPrice,
                active,
                organizationId,
                createdAt,
                createdBy,
                now,
                actorUserId,
                version);
    }

    public Product deactivate(UUID actorUserId, Instant now) {
        return new Product(
                id,
                sku,
                name,
                category,
                unit,
                currentStock,
                safetyStock,
                reorderPoint,
                cost,
                sellingPrice,
                false,
                organizationId,
                createdAt,
                createdBy,
                now,
                actorUserId,
                version);
    }

    public Product changeStock(BigDecimal newStock, UUID actorUserId, Instant now) {
        return new Product(
                id, sku, name, category, unit, newStock, safetyStock, reorderPoint, cost,
                sellingPrice, active, organizationId, createdAt, createdBy, now, actorUserId, version);
    }

    private static String normalizeSku(String raw) {
        String normalized = required(raw, 64, "sku").toUpperCase();
        if (!SKU_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("sku contains unsupported characters");
        }
        return normalized;
    }

    private static String required(String raw, int maxLength, String field) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty() || value.length() > maxLength) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }

    private static String optional(String raw, int maxLength, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return required(raw, maxLength, field);
    }

    private static BigDecimal quantity(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.scale() > 3 || value.precision() - value.scale() > 15) {
            throw new IllegalArgumentException(field + " exceeds NUMERIC(18,3)");
        }
        return value.stripTrailingZeros();
    }

    private static BigDecimal money(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.scale() > 4 || value.precision() - value.scale() > 14) {
            throw new IllegalArgumentException(field + " exceeds NUMERIC(18,4)");
        }
        return value.stripTrailingZeros();
    }

    private static BigDecimal nonnegative(BigDecimal value, String field) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }
}
