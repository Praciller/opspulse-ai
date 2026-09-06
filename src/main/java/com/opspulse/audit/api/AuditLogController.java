package com.opspulse.audit.api;

import com.opspulse.audit.api.dto.AuditLogResponse;
import com.opspulse.audit.application.port.in.QueryAuditEventsUseCase;
import com.opspulse.shared.config.OpenApiConfig;
import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import com.opspulse.shared.web.PagedResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT_SCHEME)
public class AuditLogController {

    private final QueryAuditEventsUseCase queryAuditEvents;

    public AuditLogController(QueryAuditEventsUseCase queryAuditEvents) {
        this.queryAuditEvents = queryAuditEvents;
    }

    @GetMapping
    public PagedResponse<AuditLogResponse> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId) {
        var result = queryAuditEvents.findAll(page, size, entityType, entityId);
        return new PagedResponse<>(
                result.items().stream().map(AuditLogResponse::from).toList(),
                result.page(),
                result.size(),
                result.total(),
                result.totalPages());
    }

    @GetMapping("/{id}")
    public AuditLogResponse findById(@PathVariable UUID id) {
        return queryAuditEvents
                .findById(id)
                .map(AuditLogResponse::from)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND,
                        "Audit log not found"));
    }
}
