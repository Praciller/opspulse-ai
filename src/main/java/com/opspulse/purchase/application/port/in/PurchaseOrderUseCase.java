package com.opspulse.purchase.application.port.in;

import com.opspulse.purchase.domain.PurchaseOrder;
import com.opspulse.purchase.domain.PurchaseOrderStatus;
import com.opspulse.shared.web.PagedResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderUseCase {

    PurchaseOrder create(CreateCommand command);
    Optional<PurchaseOrder> findById(UUID id);
    PagedResponse<PurchaseOrder> findAll(int page, int size, UUID supplierId, PurchaseOrderStatus status, String sortField, boolean ascending);
    PurchaseOrder update(UUID id, UpdateCommand command);
    PurchaseOrder updateStatus(UUID id, StatusCommand command);
    PurchaseOrder receive(UUID id, ReceiveCommand command);

    record ItemCommand(UUID productId, BigDecimal quantity, BigDecimal unitCost) {}
    record ReceiptCommand(UUID productId, BigDecimal quantity) {}

    record CreateCommand(String poNumber, UUID supplierId, LocalDate expectedDeliveryDate, List<ItemCommand> items,
                         UUID actorUserId, String requestId, String ipAddress) {}
    record UpdateCommand(UUID supplierId, LocalDate expectedDeliveryDate, List<ItemCommand> items, long version,
                         UUID actorUserId, String requestId, String ipAddress) {}
    record StatusCommand(PurchaseOrderStatus status, long version, UUID actorUserId, String requestId, String ipAddress) {}
    record ReceiveCommand(List<ReceiptCommand> items, LocalDate actualDeliveryDate, long version,
                          UUID actorUserId, String requestId, String ipAddress) {}
}
