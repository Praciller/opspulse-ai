package com.opspulse.purchase.api.dto;

import com.opspulse.purchase.domain.PurchaseOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

public final class PurchaseOrderRequests {
    private PurchaseOrderRequests() {}

    @Schema(name = "PurchaseOrderItemRequest")
    public record Item(@NotNull UUID productId,
                       @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 3) BigDecimal quantity,
                       @NotNull @DecimalMin("0") @Digits(integer = 14, fraction = 4) BigDecimal unitCost) {}
    @Schema(name = "PurchaseOrderReceiptItemRequest")
    public record Receipt(@NotNull UUID productId,
                          @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 3) BigDecimal quantity) {}
    @Schema(name = "CreatePurchaseOrderRequest")
    public record Create(@NotBlank @Size(max = 64) String poNumber, @NotNull UUID supplierId,
                         @NotNull LocalDate expectedDeliveryDate, @NotEmpty @Size(max = 100) List<@Valid Item> items) {}
    @Schema(name = "UpdatePurchaseOrderRequest")
    public record Update(@NotNull UUID supplierId, @NotNull LocalDate expectedDeliveryDate,
                         @NotEmpty @Size(max = 100) List<@Valid Item> items, @PositiveOrZero long version) {}
    @Schema(name = "UpdatePurchaseOrderStatusRequest")
    public record StatusUpdate(@NotNull PurchaseOrderStatus status, @PositiveOrZero long version) {}
    @Schema(name = "ReceivePurchaseOrderRequest")
    public record Receive(@NotEmpty @Size(max = 100) List<@Valid Receipt> items, LocalDate actualDeliveryDate, @PositiveOrZero long version) {}
}
