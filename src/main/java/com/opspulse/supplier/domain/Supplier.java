package com.opspulse.supplier.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record Supplier(
        UUID id,
        String name,
        Map<String, String> contactInfo,
        BigDecimal averageLeadTimeDays,
        int expectedSlaDays,
        boolean active,
        UUID organizationId,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy,
        long version) {

    public Supplier {
        Objects.requireNonNull(id, "id must not be null");
        name = required(name, 200, "name");
        contactInfo = contactInfo == null ? Map.of() : Map.copyOf(contactInfo);
        contactInfo.forEach((key, value) -> {
            if (!java.util.Set.of("email", "phone", "address").contains(key)
                    || value == null
                    || value.isBlank()
                    || value.length() > 300) {
                throw new IllegalArgumentException("contactInfo is invalid");
            }
        });
        if (averageLeadTimeDays != null
                && (averageLeadTimeDays.signum() < 0 || averageLeadTimeDays.scale() > 2)) {
            throw new IllegalArgumentException("averageLeadTimeDays is invalid");
        }
        if (expectedSlaDays < 0) {
            throw new IllegalArgumentException("expectedSlaDays must not be negative");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Supplier create(
            UUID id,
            String name,
            Map<String, String> contactInfo,
            BigDecimal averageLeadTimeDays,
            int expectedSlaDays,
            UUID actorUserId,
            Instant now) {
        return new Supplier(
                id, name, contactInfo, averageLeadTimeDays, expectedSlaDays, true, null,
                now, actorUserId, now, actorUserId, 0);
    }

    public Supplier update(
            String name,
            Map<String, String> contactInfo,
            BigDecimal averageLeadTimeDays,
            int expectedSlaDays,
            UUID actorUserId,
            Instant now) {
        return new Supplier(
                id, name, contactInfo, averageLeadTimeDays, expectedSlaDays, active,
                organizationId, createdAt, createdBy, now, actorUserId, version);
    }

    public Supplier deactivate(UUID actorUserId, Instant now) {
        return new Supplier(
                id, name, contactInfo, averageLeadTimeDays, expectedSlaDays, false,
                organizationId, createdAt, createdBy, now, actorUserId, version);
    }

    public Map<String, Object> snapshot() {
        var result = new LinkedHashMap<String, Object>();
        result.put("name", name);
        result.put("contactInfo", contactInfo);
        result.put("averageLeadTimeDays", averageLeadTimeDays);
        result.put("expectedSlaDays", expectedSlaDays);
        result.put("active", active);
        return result;
    }

    private static String required(String raw, int maxLength, String field) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty() || value.length() > maxLength) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }
}
