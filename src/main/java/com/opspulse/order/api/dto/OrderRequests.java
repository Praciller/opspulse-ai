package com.opspulse.order.api.dto;

import com.opspulse.order.domain.OrderStatus;
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

public final class OrderRequests {

    private OrderRequests() {}

    @Schema(name = "OrderItemRequest")
    public record Item(
            @NotNull UUID productId,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 3) BigDecimal quantity,
            @NotNull @DecimalMin("0") @Digits(integer = 14, fraction = 4) BigDecimal unitPrice) {}

    @Schema(name = "CreateOrderRequest")
    public record Create(
            @NotBlank @Size(max = 64) String orderNumber,
            @NotBlank @Size(max = 200) String customerName,
            @NotNull LocalDate expectedShipDate,
            @NotEmpty @Size(max = 100) List<@Valid Item> items) {}

    @Schema(name = "UpdateOrderRequest")
    public record Update(
            @NotBlank @Size(max = 200) String customerName,
            @NotNull LocalDate expectedShipDate,
            @NotEmpty @Size(max = 100) List<@Valid Item> items,
            @PositiveOrZero long version) {}

    @Schema(name = "UpdateOrderStatusRequest")
    public record StatusUpdate(
            @NotNull OrderStatus status,
            LocalDate actualShipDate,
            @PositiveOrZero long version) {}
}
