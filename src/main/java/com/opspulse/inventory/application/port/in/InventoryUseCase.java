package com.opspulse.inventory.application.port.in;

import com.opspulse.inventory.domain.InventoryMovement;
import com.opspulse.inventory.domain.MovementType;
import com.opspulse.product.domain.Product;
import com.opspulse.shared.web.PagedResponse;
import java.math.BigDecimal;
import java.util.UUID;

public interface InventoryUseCase {

    InventoryMovement createMovement(CreateMovementCommand command);

    PagedResponse<InventoryMovement> findAll(int page, int size, UUID productId, MovementType type, String sortField, boolean ascending);

    ProductInventory findByProduct(UUID productId, int page, int size);

    record CreateMovementCommand(
            UUID productId,
            MovementType movementType,
            BigDecimal quantity,
            String reason,
            String referenceType,
            UUID referenceId,
            UUID actorUserId,
            String requestId,
            String ipAddress) {}

    record ProductInventory(Product product, PagedResponse<InventoryMovement> movements) {}
}
