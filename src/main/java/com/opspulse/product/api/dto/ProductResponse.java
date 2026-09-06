package com.opspulse.product.api.dto;

import com.opspulse.product.domain.Product;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
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
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.id(),
                product.sku(),
                product.name(),
                product.category(),
                product.unit(),
                product.currentStock(),
                product.safetyStock(),
                product.reorderPoint(),
                product.cost(),
                product.sellingPrice(),
                product.active(),
                product.createdAt(),
                product.updatedAt(),
                product.version());
    }
}
