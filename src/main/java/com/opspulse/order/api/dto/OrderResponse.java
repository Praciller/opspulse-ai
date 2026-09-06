package com.opspulse.order.api.dto;

import com.opspulse.order.domain.Order;
import com.opspulse.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

public record OrderResponse(
        UUID id,
        String orderNumber,
        String customerName,
        OrderStatus status,
        LocalDate expectedShipDate,
        LocalDate actualShipDate,
        BigDecimal totalAmount,
        List<Item> items,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    @Schema(name = "OrderItemResponse")
    public record Item(UUID id, UUID productId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.id(), order.orderNumber(), order.customerName(), order.status(),
                order.expectedShipDate(), order.actualShipDate(), order.totalAmount(),
                order.items().stream().map(item -> new Item(item.id(), item.productId(), item.quantity(), item.unitPrice(), item.lineTotal())).toList(),
                order.createdAt(), order.updatedAt(), order.version());
    }
}
