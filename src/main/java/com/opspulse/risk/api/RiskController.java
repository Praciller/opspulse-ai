package com.opspulse.risk.api;

import com.opspulse.risk.api.dto.RiskEventResponse;
import com.opspulse.risk.api.dto.RiskScanResponse;
import com.opspulse.risk.application.port.in.RiskUseCase;
import com.opspulse.risk.domain.RiskEntityType;
import com.opspulse.risk.domain.RiskSeverity;
import com.opspulse.risk.domain.RiskStatus;
import com.opspulse.risk.domain.RiskType;
import com.opspulse.shared.config.OpenApiConfig;
import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import com.opspulse.shared.observability.RequestIdContext;
import com.opspulse.shared.web.PagedResponse;
import com.opspulse.shared.web.SortQuery;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/risks")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT_SCHEME)
public class RiskController {

    private final RiskUseCase risks;

    public RiskController(RiskUseCase risks) {
        this.risks = risks;
    }

    @GetMapping
    public PagedResponse<RiskEventResponse> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) RiskType riskType,
            @RequestParam(required = false) RiskSeverity severity,
            @RequestParam(required = false) RiskStatus status,
            @RequestParam(required = false) RiskEntityType entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var sorting = SortQuery.parse(sort);
        if (!java.util.Set.of("createdAt", "severity", "riskType", "status", "resolvedAt")
                .contains(sorting.field())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                    "Unsupported risk sort field");
        }
        var result = risks.findAll(page, size, riskType, severity, status, entityType, entityId, from, to,
                sorting.field(), sorting.ascending());
        return new PagedResponse<>(result.items().stream().map(RiskEventResponse::from).toList(),
                result.page(), result.size(), result.total(), result.totalPages());
    }

    @GetMapping("/{id}")
    public RiskEventResponse findById(@PathVariable UUID id) {
        return RiskEventResponse.from(risks.findById(id));
    }

    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public RiskScanResponse scan() {
        return RiskScanResponse.from(risks.scan());
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public RiskEventResponse acknowledge(
            @PathVariable UUID id, Authentication authentication, HttpServletRequest request) {
        return RiskEventResponse.from(risks.acknowledge(id, actor(authentication),
                RequestIdContext.currentRequestId(), request.getRemoteAddr()));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public RiskEventResponse resolve(
            @PathVariable UUID id, Authentication authentication, HttpServletRequest request) {
        return RiskEventResponse.from(risks.resolve(id, actor(authentication),
                RequestIdContext.currentRequestId(), request.getRemoteAddr()));
    }

    @PostMapping("/{id}/dismiss")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public RiskEventResponse dismiss(
            @PathVariable UUID id, Authentication authentication, HttpServletRequest request) {
        return RiskEventResponse.from(risks.dismiss(id, actor(authentication),
                RequestIdContext.currentRequestId(), request.getRemoteAddr()));
    }

    private static UUID actor(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
