package com.opspulse.product.application;

import com.opspulse.audit.application.port.in.RecordAuditEventUseCase;
import com.opspulse.audit.domain.AuditEvent;
import com.opspulse.outbox.application.port.in.OutboxPublisher;
import com.opspulse.product.application.port.in.ProductUseCase;
import com.opspulse.product.application.port.out.ProductRepository;
import com.opspulse.product.domain.Product;
import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import com.opspulse.shared.web.PagedResponse;
import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService implements ProductUseCase {

    private static final Set<String> SORT_FIELDS =
            Set.of("createdAt", "name", "sku", "category", "currentStock", "sellingPrice");

    private final ProductRepository products;
    private final RecordAuditEventUseCase auditEvents;
    private final OutboxPublisher outbox;
    private final Clock clock;

    public ProductService(
            ProductRepository products,
            RecordAuditEventUseCase auditEvents,
            OutboxPublisher outbox,
            Clock clock) {
        this.products = products;
        this.auditEvents = auditEvents;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Product create(CreateProductCommand command) {
        if (products.existsBySku(command.sku().trim().toUpperCase())) {
            throw duplicateSku(command.sku());
        }
        var now = clock.instant();
        Product product;
        try {
            product = products.save(Product.create(
                    UUID.randomUUID(),
                    command.sku(),
                    command.name(),
                    command.category(),
                    command.unit(),
                    command.currentStock(),
                    command.safetyStock(),
                    command.reorderPoint(),
                    command.cost(),
                    command.sellingPrice(),
                    command.actorUserId(),
                    now));
        } catch (DuplicateSkuException exception) {
            throw duplicateSku(command.sku());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage());
        }
        auditEvents.record(new AuditEvent(
                UUID.randomUUID(),
                command.actorUserId(),
                "product.create",
                "PRODUCT",
                product.id(),
                null,
                product.snapshot(),
                command.requestId(),
                command.ipAddress(),
                now));
        outbox.publish(
                "PRODUCT",
                product.id(),
                "opspulse.product.created",
                Map.of(
                        "productId", product.id().toString(),
                        "sku", product.sku(),
                        "name", product.name(),
                        "currentStock", product.currentStock(),
                        "active", product.active()));
        return product;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(UUID id) {
        return products.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<Product> findAll(
            int page, int size, String search, String sortField, boolean ascending) {
        if (!SORT_FIELDS.contains(sortField)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    "Unsupported product sort field");
        }
        return products.findAll(page, size, search, sortField, ascending);
    }

    @Override
    @Transactional
    public Product update(UUID id, UpdateProductCommand command) {
        Product existing = requiredProduct(id);
        requireVersion(existing, command.version());
        var before = existing.snapshot();
        Product updated;
        try {
            updated = products.save(existing.updateDetails(
                    command.name(),
                    command.category(),
                    command.unit(),
                    command.safetyStock(),
                    command.reorderPoint(),
                    command.cost(),
                    command.sellingPrice(),
                    command.actorUserId(),
                    clock.instant()));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage());
        } catch (ProductVersionConflictException exception) {
            throw versionConflict();
        }
        audit(command.actorUserId(), "product.update", updated, before, command.requestId(), command.ipAddress());
        outbox.publish(
                "PRODUCT",
                updated.id(),
                "opspulse.product.updated",
                Map.of("productId", updated.id().toString(), "before", before, "after", updated.snapshot()));
        return updated;
    }

    @Override
    @Transactional
    public void deactivate(
            UUID id,
            long version,
            UUID actorUserId,
            String requestId,
            String ipAddress) {
        Product existing = requiredProduct(id);
        requireVersion(existing, version);
        if (!existing.active()) {
            return;
        }
        if (products.hasInventoryMovements(id)) {
            throw new ApiException(
                    ErrorCode.PRODUCT_HAS_MOVEMENTS,
                    HttpStatus.CONFLICT,
                    "Product with inventory movements cannot be deactivated");
        }
        Product deactivated;
        try {
            deactivated = products.save(existing.deactivate(actorUserId, clock.instant()));
        } catch (ProductVersionConflictException exception) {
            throw versionConflict();
        }
        audit(actorUserId, "product.deactivate", deactivated, existing.snapshot(), requestId, ipAddress);
        outbox.publish(
                "PRODUCT",
                deactivated.id(),
                "opspulse.product.updated",
                Map.of("productId", deactivated.id().toString(), "before", existing.snapshot(), "after", deactivated.snapshot()));
    }

    private Product requiredProduct(UUID id) {
        return products.findById(id).orElseThrow(() -> new ApiException(
                ErrorCode.PRODUCT_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Product not found"));
    }

    private static void requireVersion(Product product, long version) {
        if (product.version() != version) {
            throw versionConflict();
        }
    }

    private void audit(
            UUID actorUserId,
            String action,
            Product product,
            Map<String, Object> before,
            String requestId,
            String ipAddress) {
        auditEvents.record(new AuditEvent(
                UUID.randomUUID(),
                actorUserId,
                action,
                "PRODUCT",
                product.id(),
                before,
                product.snapshot(),
                requestId,
                ipAddress,
                clock.instant()));
    }

    private static ApiException duplicateSku(String sku) {
        return new ApiException(
                ErrorCode.PRODUCT_SKU_DUPLICATE,
                HttpStatus.CONFLICT,
                "SKU '" + sku.trim().toUpperCase() + "' already exists");
    }

    private static ApiException versionConflict() {
        return new ApiException(
                ErrorCode.CONCURRENCY_CONFLICT,
                HttpStatus.CONFLICT,
                "Product was modified by another request");
    }
}
