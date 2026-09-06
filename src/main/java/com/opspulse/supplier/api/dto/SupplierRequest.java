package com.opspulse.supplier.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public record SupplierRequest(
        @NotBlank @Size(max = 200) String name,
        @Valid ContactInfo contactInfo,
        @DecimalMin("0") @Digits(integer = 4, fraction = 2) BigDecimal averageLeadTimeDays,
        @NotNull @PositiveOrZero Integer expectedSlaDays,
        @PositiveOrZero Long version) {

    public Map<String, String> contactMap() {
        if (contactInfo == null) {
            return Map.of();
        }
        var result = new LinkedHashMap<String, String>();
        putIfPresent(result, "email", contactInfo.email());
        putIfPresent(result, "phone", contactInfo.phone());
        putIfPresent(result, "address", contactInfo.address());
        return Map.copyOf(result);
    }

    private static void putIfPresent(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value.trim());
        }
    }

    public record ContactInfo(
            @Email @Size(max = 254) String email,
            @Size(max = 40) String phone,
            @Size(max = 300) String address) {}
}
