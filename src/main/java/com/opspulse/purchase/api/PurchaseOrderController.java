package com.opspulse.purchase.api;

import com.opspulse.purchase.api.dto.PurchaseOrderRequests;
import com.opspulse.purchase.api.dto.PurchaseOrderResponse;
import com.opspulse.purchase.application.port.in.PurchaseOrderUseCase;
import com.opspulse.purchase.domain.PurchaseOrderStatus;
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
@RequestMapping("/api/purchase-orders")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT_SCHEME)
public class PurchaseOrderController {
    private final PurchaseOrderUseCase purchaseOrders;
    public PurchaseOrderController(PurchaseOrderUseCase purchaseOrders) { this.purchaseOrders = purchaseOrders; }

    @PostMapping @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody PurchaseOrderRequests.Create request, Authentication auth, HttpServletRequest servlet) {
        var order = purchaseOrders.create(new PurchaseOrderUseCase.CreateCommand(request.poNumber(), request.supplierId(), request.expectedDeliveryDate(),
                request.items().stream().map(PurchaseOrderController::toItem).toList(), actor(auth), RequestIdContext.currentRequestId(), servlet.getRemoteAddr()));
        return ResponseEntity.status(HttpStatus.CREATED).body(PurchaseOrderResponse.from(order));
    }
    @GetMapping("/{id}") public PurchaseOrderResponse findById(@PathVariable UUID id) {
        return purchaseOrders.findById(id).map(PurchaseOrderResponse::from).orElseThrow(() -> new ApiException(ErrorCode.PURCHASE_ORDER_NOT_FOUND, HttpStatus.NOT_FOUND, "Purchase order not found"));
    }
    @GetMapping public PagedResponse<PurchaseOrderResponse> findAll(@RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size, @RequestParam(required=false) UUID supplierId,
            @RequestParam(required=false) PurchaseOrderStatus status, @RequestParam(defaultValue="createdAt,desc") String sort) {
        var sorting = SortQuery.parse(sort); var result = purchaseOrders.findAll(page, size, supplierId, status, sorting.field(), sorting.ascending());
        return new PagedResponse<>(result.items().stream().map(PurchaseOrderResponse::from).toList(), result.page(), result.size(), result.total(), result.totalPages());
    }
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public PurchaseOrderResponse update(@PathVariable UUID id, @Valid @RequestBody PurchaseOrderRequests.Update request, Authentication auth, HttpServletRequest servlet) {
        return PurchaseOrderResponse.from(purchaseOrders.update(id, new PurchaseOrderUseCase.UpdateCommand(request.supplierId(), request.expectedDeliveryDate(),
                request.items().stream().map(PurchaseOrderController::toItem).toList(), request.version(), actor(auth), RequestIdContext.currentRequestId(), servlet.getRemoteAddr())));
    }
    @PatchMapping("/{id}/status") @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public PurchaseOrderResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody PurchaseOrderRequests.StatusUpdate request, Authentication auth, HttpServletRequest servlet) {
        return PurchaseOrderResponse.from(purchaseOrders.updateStatus(id, new PurchaseOrderUseCase.StatusCommand(request.status(), request.version(), actor(auth), RequestIdContext.currentRequestId(), servlet.getRemoteAddr())));
    }
    @PostMapping("/{id}/receive") @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public PurchaseOrderResponse receive(@PathVariable UUID id, @Valid @RequestBody PurchaseOrderRequests.Receive request, Authentication auth, HttpServletRequest servlet) {
        return PurchaseOrderResponse.from(purchaseOrders.receive(id, new PurchaseOrderUseCase.ReceiveCommand(
                request.items().stream().map(item -> new PurchaseOrderUseCase.ReceiptCommand(item.productId(), item.quantity())).toList(),
                request.actualDeliveryDate(), request.version(), actor(auth), RequestIdContext.currentRequestId(), servlet.getRemoteAddr())));
    }
    private static PurchaseOrderUseCase.ItemCommand toItem(PurchaseOrderRequests.Item item) { return new PurchaseOrderUseCase.ItemCommand(item.productId(), item.quantity(), item.unitCost()); }
    private static UUID actor(Authentication auth) { return UUID.fromString(auth.getName()); }
}
