package com.opspulse.purchase.api.dto;

import com.opspulse.purchase.domain.PurchaseOrder;
import com.opspulse.purchase.domain.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

public record PurchaseOrderResponse(UUID id, String poNumber, UUID supplierId, PurchaseOrderStatus status,
        LocalDate expectedDeliveryDate, LocalDate actualDeliveryDate, List<Item> items,
        BigDecimal totalAmount, Instant createdAt, Instant updatedAt, long version) {
    @Schema(name = "PurchaseOrderItemResponse")
    public record Item(UUID id, UUID productId, BigDecimal quantity, BigDecimal unitCost, BigDecimal lineTotal, BigDecimal receivedQuantity) {}
    public static PurchaseOrderResponse from(PurchaseOrder order) {
        return new PurchaseOrderResponse(order.id(), order.poNumber(), order.supplierId(), order.status(), order.expectedDeliveryDate(), order.actualDeliveryDate(),
                order.items().stream().map(item -> new Item(item.id(), item.productId(), item.quantity(), item.unitCost(), item.lineTotal(), item.receivedQuantity())).toList(),
                order.totalAmount(), order.createdAt(), order.updatedAt(), order.version());
    }
}
