package com.opspulse.product.api;

import com.opspulse.product.api.dto.CreateProductRequest;
import com.opspulse.product.api.dto.ProductResponse;
import com.opspulse.product.api.dto.UpdateProductRequest;
import com.opspulse.product.application.port.in.ProductUseCase;
import com.opspulse.inventory.api.dto.InventoryMovementResponse;
import com.opspulse.inventory.application.port.in.InventoryUseCase;
import com.opspulse.shared.config.OpenApiConfig;
import com.opspulse.shared.observability.RequestIdContext;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import com.opspulse.shared.web.PagedResponse;
import com.opspulse.shared.web.SortQuery;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@Validated
@RequestMapping("/api/products")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT_SCHEME)
public class ProductController {

    private final ProductUseCase products;
    private final InventoryUseCase inventory;

    public ProductController(ProductUseCase products, InventoryUseCase inventory) {
        this.products = products;
        this.inventory = inventory;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody CreateProductRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        var product = products.create(new ProductUseCase.CreateProductCommand(
                request.sku(),
                request.name(),
                request.category(),
                request.unit(),
                request.currentStock(),
                request.safetyStock(),
                request.reorderPoint(),
                request.cost(),
                request.sellingPrice(),
                UUID.fromString(authentication.getName()),
                RequestIdContext.currentRequestId(),
                servletRequest.getRemoteAddr()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable UUID id) {
        return products.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new com.opspulse.shared.error.ApiException(
                        com.opspulse.shared.error.ErrorCode.PRODUCT_NOT_FOUND,
                        HttpStatus.NOT_FOUND,
                        "Product not found"));
    }

    @GetMapping
    public PagedResponse<ProductResponse> findAll(
            @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int page,
            @RequestParam(defaultValue = "20") @jakarta.validation.constraints.Min(1)
                    @jakarta.validation.constraints.Max(100) int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var sorting = SortQuery.parse(sort);
        var result = products.findAll(page, size, search, sorting.field(), sorting.ascending());
        return new PagedResponse<>(
                result.items().stream().map(ProductResponse::from).toList(),
                result.page(),
                result.size(),
                result.total(),
                result.totalPages());
    }

    @GetMapping("/{id}/movements")
    public PagedResponse<InventoryMovementResponse> findMovements(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int page,
            @RequestParam(defaultValue = "20") @jakarta.validation.constraints.Min(1)
                    @jakarta.validation.constraints.Max(100) int size) {
        products.findById(id).orElseThrow(() -> new com.opspulse.shared.error.ApiException(
                com.opspulse.shared.error.ErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND, "Product not found"));
        var result = inventory.findAll(page, size, id, null, "createdAt", false);
        return new PagedResponse<>(result.items().stream().map(InventoryMovementResponse::from).toList(),
                result.page(), result.size(), result.total(), result.totalPages());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ProductResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        return ProductResponse.from(products.update(id, new ProductUseCase.UpdateProductCommand(
                request.name(),
                request.category(),
                request.unit(),
                request.safetyStock(),
                request.reorderPoint(),
                request.cost(),
                request.sellingPrice(),
                request.version(),
                UUID.fromString(authentication.getName()),
                RequestIdContext.currentRequestId(),
                servletRequest.getRemoteAddr())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID id,
            @RequestParam long version,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        products.deactivate(
                id,
                version,
                UUID.fromString(authentication.getName()),
                RequestIdContext.currentRequestId(),
                servletRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
