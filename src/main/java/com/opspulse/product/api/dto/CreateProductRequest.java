package com.opspulse.product.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 80) String category,
        @NotBlank @Size(max = 20) String unit,
        @NotNull @Digits(integer = 15, fraction = 3) BigDecimal currentStock,
        @NotNull @DecimalMin("0") @Digits(integer = 15, fraction = 3) BigDecimal safetyStock,
        @NotNull @DecimalMin("0") @Digits(integer = 15, fraction = 3) BigDecimal reorderPoint,
        @NotNull @DecimalMin("0") @Digits(integer = 14, fraction = 4) BigDecimal cost,
        @NotNull @DecimalMin("0") @Digits(integer = 14, fraction = 4) BigDecimal sellingPrice) {}
