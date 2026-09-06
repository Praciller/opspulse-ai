package com.opspulse.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record Order(
        UUID id,
        String orderNumber,
        String customerName,
        OrderStatus status,
        LocalDate expectedShipDate,
        LocalDate actualShipDate,
        List<OrderItem> items,
        UUID organizationId,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy,
        long version) {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            OrderStatus.NEW, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PICKING, OrderStatus.DELAYED, OrderStatus.CANCELLED),
            OrderStatus.PICKING, Set.of(OrderStatus.SHIPPED, OrderStatus.DELAYED, OrderStatus.CANCELLED),
            OrderStatus.DELAYED, Set.of(OrderStatus.PICKING, OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(),
            OrderStatus.CANCELLED, Set.of());

    public Order {
        Objects.requireNonNull(id, "id must not be null");
        orderNumber = required(orderNumber, 64, "orderNumber").toUpperCase();
        customerName = required(customerName, 200, "customerName");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(expectedShipDate, "expectedShipDate must not be null");
        items = items == null ? List.of() : List.copyOf(items);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        if (items.stream().map(OrderItem::productId).distinct().count() != items.size()) {
            throw new IllegalArgumentException("items must contain unique products");
        }
        if ((status == OrderStatus.SHIPPED) != (actualShipDate != null)) {
            throw new IllegalArgumentException("actualShipDate is required only for shipped orders");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Order create(
            UUID id,
            String orderNumber,
            String customerName,
            LocalDate expectedShipDate,
            List<OrderItem> items,
            UUID actorUserId,
            Instant now) {
        return new Order(id, orderNumber, customerName, OrderStatus.NEW, expectedShipDate, null,
                items, null, now, actorUserId, now, actorUserId, 0);
    }

    public BigDecimal totalAmount() {
        return items.stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Order updateDetails(
            String customerName,
            LocalDate expectedShipDate,
            List<OrderItem> items,
            UUID actorUserId,
            Instant now) {
        if (status != OrderStatus.NEW) {
            throw new InvalidOrderStatusTransitionException("Only NEW orders can be edited");
        }
        return new Order(id, orderNumber, customerName, status, expectedShipDate, null, items,
                organizationId, createdAt, createdBy, now, actorUserId, version);
    }

    public Order transitionTo(
            OrderStatus target,
            LocalDate actualShipDate,
            UUID actorUserId,
            Instant now) {
        if (!TRANSITIONS.get(status).contains(target)) {
            throw new InvalidOrderStatusTransitionException(
                    "Order cannot transition from " + status + " to " + target);
        }
        if (target == OrderStatus.SHIPPED && actualShipDate == null) {
            throw new IllegalArgumentException("actualShipDate is required when shipping an order");
        }
        if (target != OrderStatus.SHIPPED && actualShipDate != null) {
            throw new IllegalArgumentException("actualShipDate is only valid for SHIPPED status");
        }
        return new Order(id, orderNumber, customerName, target, expectedShipDate, actualShipDate, items,
                organizationId, createdAt, createdBy, now, actorUserId, version);
    }

    public Map<String, Object> snapshot() {
        var value = new LinkedHashMap<String, Object>();
        value.put("orderNumber", orderNumber);
        value.put("customerName", customerName);
        value.put("status", status.name());
        value.put("expectedShipDate", expectedShipDate.toString());
        value.put("actualShipDate", actualShipDate == null ? null : actualShipDate.toString());
        value.put("totalAmount", totalAmount());
        value.put("items", items.stream().map(item -> Map.of(
                "productId", item.productId().toString(),
                "quantity", item.quantity(),
                "unitPrice", item.unitPrice(),
                "lineTotal", item.lineTotal())).toList());
        return value;
    }

    private static String required(String raw, int maxLength, String field) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty() || value.length() > maxLength) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }
}
