package com.opspulse.order.api;

import com.opspulse.order.api.dto.OrderRequests;
import com.opspulse.order.api.dto.OrderResponse;
import com.opspulse.order.application.port.in.OrderUseCase;
import com.opspulse.order.domain.OrderStatus;
import com.opspulse.shared.config.OpenApiConfig;
import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import com.opspulse.shared.observability.RequestIdContext;
import com.opspulse.shared.web.PagedResponse;
import com.opspulse.shared.web.SortQuery;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/orders")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT_SCHEME)
public class OrderController {

    private final OrderUseCase orders;

    public OrderController(OrderUseCase orders) {
        this.orders = orders;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequests.Create request, Authentication authentication, HttpServletRequest servletRequest) {
        var order = orders.create(new OrderUseCase.CreateOrderCommand(request.orderNumber(), request.customerName(), request.expectedShipDate(),
                request.items().stream().map(OrderController::toCommand).toList(), actor(authentication), RequestIdContext.currentRequestId(), servletRequest.getRemoteAddr()));
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable UUID id) {
        return orders.findById(id).map(OrderResponse::from).orElseThrow(() ->
                new ApiException(ErrorCode.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND, "Order not found"));
    }

    @GetMapping
    public PagedResponse<OrderResponse> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var sorting = SortQuery.parse(sort);
        var result = orders.findAll(page, size, search, status, sorting.field(), sorting.ascending());
        return new PagedResponse<>(result.items().stream().map(OrderResponse::from).toList(), result.page(), result.size(), result.total(), result.totalPages());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public OrderResponse update(@PathVariable UUID id, @Valid @RequestBody OrderRequests.Update request, Authentication authentication, HttpServletRequest servletRequest) {
        return OrderResponse.from(orders.update(id, new OrderUseCase.UpdateOrderCommand(request.customerName(), request.expectedShipDate(),
                request.items().stream().map(OrderController::toCommand).toList(), request.version(), actor(authentication), RequestIdContext.currentRequestId(), servletRequest.getRemoteAddr())));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public OrderResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody OrderRequests.StatusUpdate request, Authentication authentication, HttpServletRequest servletRequest) {
        return OrderResponse.from(orders.updateStatus(id, new OrderUseCase.UpdateOrderStatusCommand(request.status(), request.actualShipDate(), request.version(),
                actor(authentication), RequestIdContext.currentRequestId(), servletRequest.getRemoteAddr())));
    }

    private static OrderUseCase.ItemCommand toCommand(OrderRequests.Item item) {
        return new OrderUseCase.ItemCommand(item.productId(), item.quantity(), item.unitPrice());
    }

    private static UUID actor(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

}
