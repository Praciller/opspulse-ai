package com.opspulse.outbox.api;

import com.opspulse.outbox.application.OutboxAdminService;
import com.opspulse.outbox.domain.OutboxDelivery;
import com.opspulse.shared.config.OpenApiConfig;
import com.opspulse.shared.observability.RequestIdContext;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/outbox-events")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT_SCHEME)
public class OutboxAdminController {

    private final OutboxAdminService service;

    public OutboxAdminController(OutboxAdminService service) {
        this.service = service;
    }

    @PostMapping("/{id}/replay")
    @PreAuthorize("hasRole('ADMIN')")
    public OutboxAdminResponse replay(@PathVariable UUID id, Authentication auth, HttpServletRequest request) {
        return OutboxAdminResponse.from(service.replay(id, actor(auth), RequestIdContext.currentRequestId(), request.getRemoteAddr()));
    }

    @PostMapping("/{id}/discard")
    @PreAuthorize("hasRole('ADMIN')")
    public OutboxAdminResponse discard(@PathVariable UUID id, Authentication auth, HttpServletRequest request) {
        return OutboxAdminResponse.from(service.discard(id, actor(auth), RequestIdContext.currentRequestId(), request.getRemoteAddr()));
    }

    private static UUID actor(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    public record OutboxAdminResponse(UUID id, String status, int retryCount, Instant nextAttemptAt, Instant createdAt) {
        static OutboxAdminResponse from(OutboxDelivery row) {
            return new OutboxAdminResponse(row.id(), row.status(), row.retryCount(), row.nextAttemptAt(), row.createdAt());
        }
    }
}
