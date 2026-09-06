package com.opspulse.inventory.api.dto;

import com.opspulse.inventory.application.port.in.InventoryUseCase;
import com.opspulse.shared.web.PagedResponse;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductInventoryResponse(
        UUID productId,
        String sku,
        BigDecimal currentStock,
        BigDecimal safetyStock,
        BigDecimal reorderPoint,
        PagedResponse<InventoryMovementResponse> movements) {

    public static ProductInventoryResponse from(InventoryUseCase.ProductInventory inventory) {
        var product = inventory.product();
        var movements = inventory.movements();
        return new ProductInventoryResponse(product.id(), product.sku(), product.currentStock(), product.safetyStock(), product.reorderPoint(),
                new PagedResponse<>(movements.items().stream().map(InventoryMovementResponse::from).toList(),
                        movements.page(), movements.size(), movements.total(), movements.totalPages()));
    }
}
