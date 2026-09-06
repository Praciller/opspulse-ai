package com.opspulse.inventory.api;

import com.opspulse.inventory.api.dto.CreateInventoryMovementRequest;
import com.opspulse.inventory.api.dto.InventoryMovementResponse;
import com.opspulse.inventory.api.dto.ProductInventoryResponse;
import com.opspulse.inventory.application.port.in.InventoryUseCase;
import com.opspulse.inventory.domain.MovementType;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/inventory-movements")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT_SCHEME)
public class InventoryMovementController {

    private final InventoryUseCase inventory;

    public InventoryMovementController(InventoryUseCase inventory) {
        this.inventory = inventory;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<InventoryMovementResponse> create(@Valid @RequestBody CreateInventoryMovementRequest request, Authentication authentication, HttpServletRequest servletRequest) {
        var movement = inventory.createMovement(new InventoryUseCase.CreateMovementCommand(request.productId(), request.movementType(), request.quantity(),
                request.reason(), request.referenceType(), request.referenceId(), UUID.fromString(authentication.getName()),
                RequestIdContext.currentRequestId(), servletRequest.getRemoteAddr()));
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryMovementResponse.from(movement));
    }

    @GetMapping
    public PagedResponse<InventoryMovementResponse> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) MovementType movementType,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var sorting = SortQuery.parse(sort);
        var result = inventory.findAll(page, size, productId, movementType, sorting.field(), sorting.ascending());
        return new PagedResponse<>(result.items().stream().map(InventoryMovementResponse::from).toList(), result.page(), result.size(), result.total(), result.totalPages());
    }

    @GetMapping("/product/{productId}")
    public ProductInventoryResponse findByProduct(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ProductInventoryResponse.from(inventory.findByProduct(productId, page, size));
    }

}
