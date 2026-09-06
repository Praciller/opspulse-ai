package com.opspulse.purchase.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record PurchaseOrder(
        UUID id,
        String poNumber,
        UUID supplierId,
        PurchaseOrderStatus status,
        LocalDate expectedDeliveryDate,
        LocalDate actualDeliveryDate,
        List<PurchaseOrderItem> items,
        UUID organizationId,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy,
        long version) {

    private static final Map<PurchaseOrderStatus, Set<PurchaseOrderStatus>> TRANSITIONS = Map.of(
            PurchaseOrderStatus.DRAFT, Set.of(PurchaseOrderStatus.SENT, PurchaseOrderStatus.CANCELLED),
            PurchaseOrderStatus.SENT, Set.of(PurchaseOrderStatus.DELAYED, PurchaseOrderStatus.CANCELLED),
            PurchaseOrderStatus.DELAYED, Set.of(PurchaseOrderStatus.SENT, PurchaseOrderStatus.CANCELLED),
            PurchaseOrderStatus.PARTIALLY_RECEIVED, Set.of(),
            PurchaseOrderStatus.RECEIVED, Set.of(),
            PurchaseOrderStatus.CANCELLED, Set.of());

    public PurchaseOrder {
        Objects.requireNonNull(id, "id must not be null");
        poNumber = required(poNumber, 64, "poNumber").toUpperCase();
        Objects.requireNonNull(supplierId, "supplierId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(expectedDeliveryDate, "expectedDeliveryDate must not be null");
        items = items == null ? List.of() : List.copyOf(items);
        if (items.isEmpty() || items.stream().map(PurchaseOrderItem::productId).distinct().count() != items.size()) {
            throw new IllegalArgumentException("items must be nonempty and contain unique products");
        }
        if ((status == PurchaseOrderStatus.RECEIVED) != (actualDeliveryDate != null)) {
            throw new IllegalArgumentException("actualDeliveryDate is required only for received purchase orders");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static PurchaseOrder create(UUID id, String poNumber, UUID supplierId, LocalDate expectedDate, List<PurchaseOrderItem> items, UUID actor, Instant now) {
        return new PurchaseOrder(id, poNumber, supplierId, PurchaseOrderStatus.DRAFT, expectedDate, null,
                items, null, now, actor, now, actor, 0);
    }

    public PurchaseOrder updateDraft(UUID supplierId, LocalDate expectedDate, List<PurchaseOrderItem> items, UUID actor, Instant now) {
        if (status != PurchaseOrderStatus.DRAFT) {
            throw new InvalidPurchaseOrderStatusTransitionException("Only DRAFT purchase orders can be edited");
        }
        return new PurchaseOrder(id, poNumber, supplierId, status, expectedDate, null, items,
                organizationId, createdAt, createdBy, now, actor, version);
    }

    public PurchaseOrder transitionTo(PurchaseOrderStatus target, UUID actor, Instant now) {
        if (!TRANSITIONS.get(status).contains(target)) {
            throw new InvalidPurchaseOrderStatusTransitionException("Purchase order cannot transition from " + status + " to " + target);
        }
        return new PurchaseOrder(id, poNumber, supplierId, target, expectedDeliveryDate, null, items,
                organizationId, createdAt, createdBy, now, actor, version);
    }

    public PurchaseOrder receive(Map<UUID, BigDecimal> receipts, LocalDate actualDate, UUID actor, Instant now) {
        if (status != PurchaseOrderStatus.SENT && status != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new InvalidPurchaseOrderStatusTransitionException("Purchase order cannot receive inventory in status " + status);
        }
        if (receipts == null || receipts.isEmpty() || receipts.size() != receipts.keySet().stream().distinct().count()) {
            throw new IllegalArgumentException("receipts must not be empty");
        }
        if (!items.stream().map(PurchaseOrderItem::productId).collect(java.util.stream.Collectors.toSet()).containsAll(receipts.keySet())) {
            throw new IllegalArgumentException("receipt contains a product not present on the purchase order");
        }
        var receivedItems = items.stream().map(item -> receipts.containsKey(item.productId())
                ? item.receive(receipts.get(item.productId())) : item).toList();
        boolean complete = receivedItems.stream().allMatch(PurchaseOrderItem::fullyReceived);
        if (complete && actualDate == null) {
            throw new IllegalArgumentException("actualDeliveryDate is required when completing receipt");
        }
        if (!complete && actualDate != null) {
            throw new IllegalArgumentException("actualDeliveryDate is only valid for a complete receipt");
        }
        return new PurchaseOrder(id, poNumber, supplierId,
                complete ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED,
                expectedDeliveryDate, complete ? actualDate : null, receivedItems,
                organizationId, createdAt, createdBy, now, actor, version);
    }

    public BigDecimal totalAmount() {
        return items.stream().map(PurchaseOrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, Object> snapshot() {
        var value = new LinkedHashMap<String, Object>();
        value.put("poNumber", poNumber);
        value.put("supplierId", supplierId.toString());
        value.put("status", status.name());
        value.put("expectedDeliveryDate", expectedDeliveryDate.toString());
        value.put("actualDeliveryDate", actualDeliveryDate == null ? null : actualDeliveryDate.toString());
        value.put("totalAmount", totalAmount());
        value.put("items", items.stream().map(item -> Map.of(
                "productId", item.productId().toString(), "quantity", item.quantity(),
                "unitCost", item.unitCost(), "receivedQuantity", item.receivedQuantity())).toList());
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
