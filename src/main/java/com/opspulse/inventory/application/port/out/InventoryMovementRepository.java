package com.opspulse.inventory.application.port.out;

import com.opspulse.inventory.domain.InventoryMovement;
import com.opspulse.inventory.domain.MovementType;
import com.opspulse.shared.web.PagedResponse;
import java.util.UUID;

public interface InventoryMovementRepository {

    InventoryMovement save(InventoryMovement movement);

    PagedResponse<InventoryMovement> findAll(int page, int size, UUID productId, MovementType type, String sortField, boolean ascending);
}
