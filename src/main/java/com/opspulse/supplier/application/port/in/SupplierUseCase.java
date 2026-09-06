package com.opspulse.supplier.application.port.in;

import com.opspulse.shared.web.PagedResponse;
import com.opspulse.supplier.domain.Supplier;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SupplierUseCase {

    Supplier create(SupplierCommand command);

    Optional<Supplier> findById(UUID id);

    PagedResponse<Supplier> findAll(int page, int size, String search, String sortField, boolean ascending);

    Supplier update(UUID id, SupplierCommand command);

    void deactivate(UUID id, long version, UUID actorUserId, String requestId, String ipAddress);

    record SupplierCommand(
            String name,
            Map<String, String> contactInfo,
            BigDecimal averageLeadTimeDays,
            int expectedSlaDays,
            long version,
            UUID actorUserId,
            String requestId,
            String ipAddress) {}
}
