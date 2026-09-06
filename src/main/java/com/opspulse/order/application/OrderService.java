package com.opspulse.order.application;

import com.opspulse.audit.application.port.in.RecordAuditEventUseCase;
import com.opspulse.audit.domain.AuditEvent;
import com.opspulse.order.application.port.in.OrderUseCase;
import com.opspulse.order.application.port.out.OrderRepository;
import com.opspulse.order.domain.InvalidOrderStatusTransitionException;
import com.opspulse.order.domain.Order;
import com.opspulse.order.domain.OrderItem;
import com.opspulse.order.domain.OrderStatus;
import com.opspulse.outbox.application.port.in.OutboxPublisher;
import com.opspulse.product.application.port.in.ProductUseCase;
import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import com.opspulse.shared.web.PagedResponse;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService implements OrderUseCase {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "orderNumber", "customerName", "status", "expectedShipDate", "totalAmount");

    private final OrderRepository orders;
    private final ProductUseCase products;
    private final RecordAuditEventUseCase auditEvents;
    private final OutboxPublisher outbox;
    private final Clock clock;

    public OrderService(OrderRepository orders, ProductUseCase products, RecordAuditEventUseCase auditEvents, OutboxPublisher outbox, Clock clock) {
        this.orders = orders;
        this.products = products;
        this.auditEvents = auditEvents;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Order create(CreateOrderCommand command) {
        String number = normalizeNumber(command.orderNumber());
        if (orders.existsByOrderNumber(number)) {
            throw duplicateNumber(number);
        }
        var now = clock.instant();
        Order order = validated(() -> orders.save(Order.create(
                UUID.randomUUID(), number, command.customerName(), command.expectedShipDate(),
                toItems(command.items(), List.of()), command.actorUserId(), now)));
        audit("order.create", order, null, command.actorUserId(), command.requestId(), command.ipAddress());
        outbox.publish("ORDER", order.id(), "opspulse.order.created", Map.of("order", order.snapshot()));
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(UUID id) {
        return orders.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<Order> findAll(int page, int size, String search, OrderStatus status, String sortField, boolean ascending) {
        if (!SORT_FIELDS.contains(sortField)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Unsupported order sort field");
        }
        return orders.findAll(page, size, search, status, sortField, ascending);
    }

    @Override
    @Transactional
    public Order update(UUID id, UpdateOrderCommand command) {
        Order existing = requiredOrder(id);
        requireVersion(existing, command.version());
        Order updated = validated(() -> orders.save(existing.updateDetails(
                command.customerName(), command.expectedShipDate(), toItems(command.items(), existing.items()),
                command.actorUserId(), clock.instant())));
        audit("order.update", updated, existing.snapshot(), command.actorUserId(), command.requestId(), command.ipAddress());
        return updated;
    }

    @Override
    @Transactional
    public Order updateStatus(UUID id, UpdateOrderStatusCommand command) {
        Order existing = requiredOrder(id);
        requireVersion(existing, command.version());
        Order updated = validated(() -> orders.save(existing.transitionTo(
                command.status(), command.actualShipDate(), command.actorUserId(), clock.instant())));
        audit("order.status.update", updated, existing.snapshot(), command.actorUserId(), command.requestId(), command.ipAddress());
        String eventType = updated.status() == OrderStatus.DELAYED
                ? "opspulse.order.delayed"
                : "opspulse.order.status.changed";
        outbox.publish("ORDER", updated.id(), eventType, Map.of(
                "orderId", updated.id().toString(),
                "oldStatus", existing.status().name(),
                "newStatus", updated.status().name()));
        return updated;
    }

    private List<OrderItem> toItems(List<ItemCommand> commands, List<OrderItem> existingItems) {
        if (commands == null) {
            throw validation("items must not be null");
        }
        return commands.stream().map(item -> {
            var product = products.findById(item.productId()).orElseThrow(() -> new ApiException(
                    ErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND, "Product not found"));
            if (!product.active()) {
                throw validation("Order items require active products");
            }
            UUID itemId = existingItems.stream()
                    .filter(existing -> existing.productId().equals(item.productId()))
                    .map(OrderItem::id)
                    .findFirst()
                    .orElseGet(UUID::randomUUID);
            return validated(() -> new OrderItem(itemId, item.productId(), item.quantity(), item.unitPrice()));
        }).toList();
    }

    private Order requiredOrder(UUID id) {
        return orders.findById(id).orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND, "Order not found"));
    }

    private static void requireVersion(Order order, long version) {
        if (order.version() != version) {
            throw concurrencyConflict();
        }
    }

    private void audit(String action, Order order, Map<String, Object> before, UUID actor, String requestId, String ipAddress) {
        auditEvents.record(new AuditEvent(UUID.randomUUID(), actor, action, "ORDER", order.id(), before, order.snapshot(), requestId, ipAddress, clock.instant()));
    }

    private static String normalizeNumber(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private static <T> T validated(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (InvalidOrderStatusTransitionException exception) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.CONFLICT, exception.getMessage());
        } catch (OrderPersistenceException exception) {
            if (exception.reason() == OrderPersistenceException.Reason.DUPLICATE_NUMBER) {
                throw duplicateNumber("provided");
            }
            throw concurrencyConflict();
        } catch (IllegalArgumentException exception) {
            throw validation(exception.getMessage());
        }
    }

    private static ApiException duplicateNumber(String number) {
        return new ApiException(ErrorCode.ORDER_NUMBER_DUPLICATE, HttpStatus.CONFLICT, "Order number '" + number + "' already exists");
    }

    private static ApiException concurrencyConflict() {
        return new ApiException(ErrorCode.CONCURRENCY_CONFLICT, HttpStatus.CONFLICT, "Order was modified by another request");
    }

    private static ApiException validation(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }
}
