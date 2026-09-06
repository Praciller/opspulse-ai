package com.opspulse.purchase.application.port.out;

import com.opspulse.purchase.domain.PurchaseOrder;
import com.opspulse.purchase.domain.PurchaseOrderStatus;
import com.opspulse.shared.web.PagedResponse;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderRepository {

    boolean existsByPoNumber(String poNumber);
    PurchaseOrder save(PurchaseOrder purchaseOrder);
    Optional<PurchaseOrder> findById(UUID id);
    Optional<PurchaseOrder> findByIdForUpdate(UUID id);
    PagedResponse<PurchaseOrder> findAll(int page, int size, UUID supplierId, PurchaseOrderStatus status, String sortField, boolean ascending);
}
