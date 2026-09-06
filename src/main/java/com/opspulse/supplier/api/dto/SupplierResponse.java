package com.opspulse.supplier.api.dto;

import com.opspulse.supplier.domain.Supplier;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String name,
        Map<String, String> contactInfo,
        BigDecimal averageLeadTimeDays,
        int expectedSlaDays,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(
                supplier.id(), supplier.name(), supplier.contactInfo(),
                supplier.averageLeadTimeDays(), supplier.expectedSlaDays(), supplier.active(),
                supplier.createdAt(), supplier.updatedAt(), supplier.version());
    }
}
