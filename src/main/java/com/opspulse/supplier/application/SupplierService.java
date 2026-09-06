package com.opspulse.supplier.application;

import com.opspulse.audit.application.port.in.RecordAuditEventUseCase;
import com.opspulse.audit.domain.AuditEvent;
import com.opspulse.outbox.application.port.in.OutboxPublisher;
import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import com.opspulse.shared.web.PagedResponse;
import com.opspulse.supplier.application.port.in.SupplierUseCase;
import com.opspulse.supplier.application.port.out.SupplierRepository;
import com.opspulse.supplier.domain.Supplier;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService implements SupplierUseCase {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "name", "expectedSlaDays");
    private final SupplierRepository suppliers;
    private final RecordAuditEventUseCase audits;
    private final OutboxPublisher outbox;
    private final Clock clock;

    public SupplierService(
            SupplierRepository suppliers,
            RecordAuditEventUseCase audits,
            OutboxPublisher outbox,
            Clock clock) {
        this.suppliers = suppliers;
        this.audits = audits;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Supplier create(SupplierCommand command) {
        Supplier supplier = validated(() -> suppliers.save(Supplier.create(
                UUID.randomUUID(), command.name(), command.contactInfo(),
                command.averageLeadTimeDays(), command.expectedSlaDays(),
                command.actorUserId(), clock.instant())));
        audit("supplier.create", supplier, null, command);
        outbox.publish(
                "SUPPLIER",
                supplier.id(),
                "opspulse.supplier.created",
                Map.of(
                        "supplierId", supplier.id().toString(),
                        "name", supplier.name(),
                        "contactInfo", supplier.contactInfo(),
                        "expectedSlaDays", supplier.expectedSlaDays(),
                        "active", supplier.active()));
        return supplier;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Supplier> findById(UUID id) {
        return suppliers.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<Supplier> findAll(
            int page, int size, String search, String sortField, boolean ascending) {
        if (!SORT_FIELDS.contains(sortField)) {
            throw invalid("Unsupported supplier sort field");
        }
        return suppliers.findAll(page, size, search, sortField, ascending);
    }

    @Override
    @Transactional
    public Supplier update(UUID id, SupplierCommand command) {
        Supplier existing = required(id);
        requireVersion(existing, command.version());
        Supplier updated = validated(() -> suppliers.save(existing.update(
                command.name(), command.contactInfo(), command.averageLeadTimeDays(),
                command.expectedSlaDays(), command.actorUserId(), clock.instant())));
        audit("supplier.update", updated, existing.snapshot(), command);
        return updated;
    }

    @Override
    @Transactional
    public void deactivate(
            UUID id, long version, UUID actorUserId, String requestId, String ipAddress) {
        Supplier existing = required(id);
        requireVersion(existing, version);
        if (!existing.active()) {
            return;
        }
        Supplier updated = suppliers.save(existing.deactivate(actorUserId, clock.instant()));
        audit("supplier.deactivate", updated, existing.snapshot(), new SupplierCommand(
                updated.name(), updated.contactInfo(), updated.averageLeadTimeDays(),
                updated.expectedSlaDays(), version, actorUserId, requestId, ipAddress));
    }

    private Supplier required(UUID id) {
        return suppliers.findById(id).orElseThrow(() -> new ApiException(
                ErrorCode.SUPPLIER_NOT_FOUND, HttpStatus.NOT_FOUND, "Supplier not found"));
    }

    private static void requireVersion(Supplier supplier, long version) {
        if (supplier.version() != version) {
            throw new ApiException(
                    ErrorCode.CONCURRENCY_CONFLICT,
                    HttpStatus.CONFLICT,
                    "Supplier was modified by another request");
        }
    }

    private void audit(
            String action, Supplier supplier, Map<String, Object> before, SupplierCommand command) {
        audits.record(new AuditEvent(
                UUID.randomUUID(), command.actorUserId(), action, "SUPPLIER", supplier.id(),
                before, supplier.snapshot(), command.requestId(), command.ipAddress(), clock.instant()));
    }

    private static Supplier validated(java.util.function.Supplier<Supplier> operation) {
        try {
            return operation.get();
        } catch (IllegalArgumentException exception) {
            throw invalid(exception.getMessage());
        } catch (org.springframework.dao.OptimisticLockingFailureException exception) {
            throw new ApiException(
                    ErrorCode.CONCURRENCY_CONFLICT,
                    HttpStatus.CONFLICT,
                    "Supplier was modified by another request");
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }
}
