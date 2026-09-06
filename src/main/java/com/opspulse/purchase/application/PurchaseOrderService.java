package com.opspulse.purchase.application;

import com.opspulse.audit.application.port.in.RecordAuditEventUseCase;
import com.opspulse.audit.domain.AuditEvent;
import com.opspulse.inventory.application.port.in.InventoryUseCase;
import com.opspulse.inventory.domain.MovementType;
import com.opspulse.outbox.application.port.in.OutboxPublisher;
import com.opspulse.product.application.port.in.ProductUseCase;
import com.opspulse.purchase.application.port.in.PurchaseOrderUseCase;
import com.opspulse.purchase.application.port.out.PurchaseOrderRepository;
import com.opspulse.purchase.domain.InvalidPurchaseOrderStatusTransitionException;
import com.opspulse.purchase.domain.PurchaseOrder;
import com.opspulse.purchase.domain.PurchaseOrderItem;
import com.opspulse.purchase.domain.PurchaseOrderStatus;
import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import com.opspulse.shared.web.PagedResponse;
import com.opspulse.supplier.application.port.in.SupplierUseCase;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseOrderService implements PurchaseOrderUseCase {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "poNumber", "status", "expectedDeliveryDate");
    private final PurchaseOrderRepository purchaseOrders;
    private final SupplierUseCase suppliers;
    private final ProductUseCase products;
    private final InventoryUseCase inventory;
    private final RecordAuditEventUseCase auditEvents;
    private final OutboxPublisher outbox;
    private final Clock clock;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrders, SupplierUseCase suppliers, ProductUseCase products,
            InventoryUseCase inventory, RecordAuditEventUseCase auditEvents, OutboxPublisher outbox, Clock clock) {
        this.purchaseOrders = purchaseOrders;
        this.suppliers = suppliers;
        this.products = products;
        this.inventory = inventory;
        this.auditEvents = auditEvents;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PurchaseOrder create(CreateCommand command) {
        String poNumber = normalizeNumber(command.poNumber());
        if (purchaseOrders.existsByPoNumber(poNumber)) {
            throw duplicateNumber(poNumber);
        }
        requireActiveSupplier(command.supplierId());
        var order = validated(() -> purchaseOrders.save(PurchaseOrder.create(UUID.randomUUID(), poNumber, command.supplierId(),
                command.expectedDeliveryDate(), toItems(command.items(), List.of()), command.actorUserId(), clock.instant())));
        audit("purchase-order.create", order, null, command.actorUserId(), command.requestId(), command.ipAddress());
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PurchaseOrder> findById(UUID id) { return purchaseOrders.findById(id); }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PurchaseOrder> findAll(int page, int size, UUID supplierId, PurchaseOrderStatus status, String sortField, boolean ascending) {
        if (!SORT_FIELDS.contains(sortField)) throw validation("Unsupported purchase order sort field");
        return purchaseOrders.findAll(page, size, supplierId, status, sortField, ascending);
    }

    @Override
    @Transactional
    public PurchaseOrder update(UUID id, UpdateCommand command) {
        var existing = required(id);
        requireVersion(existing, command.version());
        requireActiveSupplier(command.supplierId());
        var updated = validated(() -> purchaseOrders.save(existing.updateDraft(command.supplierId(), command.expectedDeliveryDate(),
                toItems(command.items(), existing.items()), command.actorUserId(), clock.instant())));
        audit("purchase-order.update", updated, existing.snapshot(), command.actorUserId(), command.requestId(), command.ipAddress());
        return updated;
    }

    @Override
    @Transactional
    public PurchaseOrder updateStatus(UUID id, StatusCommand command) {
        var existing = required(id);
        requireVersion(existing, command.version());
        var updated = validated(() -> purchaseOrders.save(existing.transitionTo(command.status(), command.actorUserId(), clock.instant())));
        audit("purchase-order.status.update", updated, existing.snapshot(), command.actorUserId(), command.requestId(), command.ipAddress());
        if (updated.status() == PurchaseOrderStatus.SENT) {
            outbox.publish("PURCHASE_ORDER", updated.id(), "opspulse.po.sent", Map.of(
                    "poId", updated.id().toString(),
                    "supplierId", updated.supplierId().toString()));
        }
        return updated;
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public PurchaseOrder receive(UUID id, ReceiveCommand command) {
        var existing = purchaseOrders.findByIdForUpdate(id).orElseThrow(() -> notFound());
        requireVersion(existing, command.version());
        var receipts = new LinkedHashMap<UUID, BigDecimal>();
        if (command.items() == null) throw validation("items must not be null");
        command.items().forEach(item -> {
            if (receipts.put(item.productId(), item.quantity()) != null) throw validation("receipt items must contain unique products");
        });
        var received = validated(() -> existing.receive(receipts, command.actualDeliveryDate(), command.actorUserId(), clock.instant()));
        receipts.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(receipt -> inventory.createMovement(
                new InventoryUseCase.CreateMovementCommand(receipt.getKey(), MovementType.INBOUND, receipt.getValue(),
                        "Purchase order " + existing.poNumber() + " receipt", "PURCHASE_ORDER", existing.id(),
                        command.actorUserId(), command.requestId(), command.ipAddress())));
        var updated = validated(() -> purchaseOrders.save(received));
        audit("purchase-order.receive", updated, existing.snapshot(), command.actorUserId(), command.requestId(), command.ipAddress());
        outbox.publish("PURCHASE_ORDER", updated.id(), "opspulse.po.received", Map.of(
                "poId", updated.id().toString(),
                "items", receipts.entrySet().stream().map(entry -> Map.of(
                        "productId", entry.getKey().toString(),
                        "quantity", entry.getValue())).toList()));
        return updated;
    }

    private List<PurchaseOrderItem> toItems(List<ItemCommand> commands, List<PurchaseOrderItem> existing) {
        if (commands == null) throw validation("items must not be null");
        return commands.stream().map(item -> {
            var product = products.findById(item.productId()).orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND, "Product not found"));
            if (!product.active()) throw validation("Purchase orders require active products");
            UUID id = existing.stream().filter(value -> value.productId().equals(item.productId())).map(PurchaseOrderItem::id).findFirst().orElseGet(UUID::randomUUID);
            return validated(() -> new PurchaseOrderItem(id, item.productId(), item.quantity(), item.unitCost(), BigDecimal.ZERO));
        }).toList();
    }

    private void requireActiveSupplier(UUID id) {
        var supplier = suppliers.findById(id).orElseThrow(() -> new ApiException(ErrorCode.SUPPLIER_NOT_FOUND, HttpStatus.NOT_FOUND, "Supplier not found"));
        if (!supplier.active()) throw validation("Purchase orders require an active supplier");
    }

    private PurchaseOrder required(UUID id) { return purchaseOrders.findById(id).orElseThrow(PurchaseOrderService::notFound); }
    private static void requireVersion(PurchaseOrder order, long version) { if (order.version() != version) throw concurrency(); }

    private void audit(String action, PurchaseOrder order, Map<String, Object> before, UUID actor, String requestId, String ip) {
        auditEvents.record(new AuditEvent(UUID.randomUUID(), actor, action, "PURCHASE_ORDER", order.id(), before, order.snapshot(), requestId, ip, clock.instant()));
    }

    private static String normalizeNumber(String value) { return value == null ? "" : value.trim().toUpperCase(); }
    private static <T> T validated(java.util.function.Supplier<T> action) {
        try { return action.get(); }
        catch (InvalidPurchaseOrderStatusTransitionException exception) { throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.CONFLICT, exception.getMessage()); }
        catch (PurchaseOrderPersistenceException exception) {
            if (exception.reason() == PurchaseOrderPersistenceException.Reason.DUPLICATE_NUMBER) throw duplicateNumber("provided");
            throw concurrency();
        }
        catch (IllegalArgumentException exception) { throw validation(exception.getMessage()); }
    }
    private static ApiException notFound() { return new ApiException(ErrorCode.PURCHASE_ORDER_NOT_FOUND, HttpStatus.NOT_FOUND, "Purchase order not found"); }
    private static ApiException duplicateNumber(String value) { return new ApiException(ErrorCode.PURCHASE_ORDER_NUMBER_DUPLICATE, HttpStatus.CONFLICT, "Purchase order number '" + value + "' already exists"); }
    private static ApiException concurrency() { return new ApiException(ErrorCode.CONCURRENCY_CONFLICT, HttpStatus.CONFLICT, "Purchase order was modified by another request"); }
    private static ApiException validation(String message) { return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message); }
}
