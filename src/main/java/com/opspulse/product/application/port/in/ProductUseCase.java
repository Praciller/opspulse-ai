package com.opspulse.product.application.port.in;

import com.opspulse.product.domain.Product;
import com.opspulse.shared.web.PagedResponse;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ProductUseCase {

    Product create(CreateProductCommand command);

    Optional<Product> findById(UUID id);

    PagedResponse<Product> findAll(
            int page, int size, String search, String sortField, boolean ascending);

    Product update(UUID id, UpdateProductCommand command);

    void deactivate(UUID id, long version, UUID actorUserId, String requestId, String ipAddress);

    record CreateProductCommand(
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
            String requestId,
            String ipAddress) {}

    record UpdateProductCommand(
            String name,
            String category,
            String unit,
            BigDecimal safetyStock,
            BigDecimal reorderPoint,
            BigDecimal cost,
            BigDecimal sellingPrice,
            long version,
            UUID actorUserId,
            String requestId,
            String ipAddress) {}
}
