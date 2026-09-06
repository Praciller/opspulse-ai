package com.opspulse.inventory.application;

import com.opspulse.audit.application.port.in.RecordAuditEventUseCase;
import com.opspulse.audit.domain.AuditEvent;
import com.opspulse.inventory.application.port.in.InventoryUseCase;
import com.opspulse.inventory.application.port.out.InventoryMovementRepository;
import com.opspulse.inventory.domain.InventoryMovement;
import com.opspulse.outbox.application.port.in.OutboxPublisher;
import com.opspulse.product.application.port.in.ProductStockUseCase;
import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryTransactionExecutor {

    private final InventoryMovementRepository movements;
    private final ProductStockUseCase productStock;
    private final RecordAuditEventUseCase auditEvents;
    private final OutboxPublisher outbox;
    private final Clock clock;
    private final boolean allowNegativeStock;

    public InventoryTransactionExecutor(
            InventoryMovementRepository movements,
            ProductStockUseCase productStock,
            RecordAuditEventUseCase auditEvents,
            OutboxPublisher outbox,
            Clock clock,
            @Value("${opspulse.inventory.allow-negative-stock:false}") boolean allowNegativeStock) {
        this.movements = movements;
        this.productStock = productStock;
        this.auditEvents = auditEvents;
        this.outbox = outbox;
        this.clock = clock;
        this.allowNegativeStock = allowNegativeStock;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public InventoryMovement execute(InventoryUseCase.CreateMovementCommand command) {
        var product = productStock.lockForUpdate(command.productId());
        var signedQuantity = validated(() -> command.movementType().signedQuantity(command.quantity()));
        var nextBalance = product.currentStock().add(signedQuantity);
        if (!allowNegativeStock && nextBalance.signum() < 0) {
            throw new ApiException(ErrorCode.NEGATIVE_STOCK_BLOCKED, HttpStatus.UNPROCESSABLE_ENTITY,
                    "Stock would be " + nextBalance.toPlainString() + " for product " + product.sku() + "; negative stock not allowed");
        }
        var now = clock.instant();
        var movement = validated(() -> movements.save(new InventoryMovement(
                UUID.randomUUID(), product.id(), command.movementType(), signedQuantity,
                product.currentStock(), nextBalance, command.reason(), command.referenceType(), command.referenceId(),
                now, command.actorUserId(), command.requestId())));
        var updatedProduct = productStock.saveStock(product, nextBalance, command.actorUserId(), now);
        auditEvents.record(new AuditEvent(UUID.randomUUID(), command.actorUserId(), "inventory.movement", "PRODUCT", product.id(),
                Map.of("currentStock", product.currentStock()), movement.snapshot(), command.requestId(), command.ipAddress(), now));
        outbox.publish("PRODUCT", product.id(), "opspulse.product.stock.changed", Map.of(
                "productId", product.id().toString(),
                "delta", movement.quantity(),
                "newStock", updatedProduct.currentStock(),
                "reason", movement.reason() == null ? "" : movement.reason()));
        return movement;
    }

    private static <T> T validated(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }
}
