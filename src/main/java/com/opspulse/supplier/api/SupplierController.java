package com.opspulse.supplier.api;

import com.opspulse.shared.config.OpenApiConfig;
import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import com.opspulse.shared.observability.RequestIdContext;
import com.opspulse.shared.web.PagedResponse;
import com.opspulse.shared.web.SortQuery;
import com.opspulse.supplier.api.dto.SupplierRequest;
import com.opspulse.supplier.api.dto.SupplierResponse;
import com.opspulse.supplier.application.port.in.SupplierUseCase;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/suppliers")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT_SCHEME)
public class SupplierController {

    private final SupplierUseCase suppliers;

    public SupplierController(SupplierUseCase suppliers) {
        this.suppliers = suppliers;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<SupplierResponse> create(
            @Valid @RequestBody SupplierRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        var supplier = suppliers.create(command(request, 0, authentication, servletRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(SupplierResponse.from(supplier));
    }

    @GetMapping("/{id}")
    public SupplierResponse findById(@PathVariable UUID id) {
        return suppliers.findById(id).map(SupplierResponse::from).orElseThrow(() -> new ApiException(
                ErrorCode.SUPPLIER_NOT_FOUND, HttpStatus.NOT_FOUND, "Supplier not found"));
    }

    @GetMapping
    public PagedResponse<SupplierResponse> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var sorting = SortQuery.parse(sort);
        var result = suppliers.findAll(page, size, search, sorting.field(), sorting.ascending());
        return new PagedResponse<>(
                result.items().stream().map(SupplierResponse::from).toList(),
                result.page(), result.size(), result.total(), result.totalPages());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public SupplierResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        if (request.version() == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "version is required");
        }
        return SupplierResponse.from(suppliers.update(
                id, command(request, request.version(), authentication, servletRequest)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID id,
            @RequestParam long version,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        suppliers.deactivate(
                id, version, UUID.fromString(authentication.getName()),
                RequestIdContext.currentRequestId(), servletRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    private static SupplierUseCase.SupplierCommand command(
            SupplierRequest request,
            long version,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        return new SupplierUseCase.SupplierCommand(
                request.name(), request.contactMap(), request.averageLeadTimeDays(),
                request.expectedSlaDays(), version, UUID.fromString(authentication.getName()),
                RequestIdContext.currentRequestId(), servletRequest.getRemoteAddr());
    }

}
