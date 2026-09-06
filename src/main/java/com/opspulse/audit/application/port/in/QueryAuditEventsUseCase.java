package com.opspulse.audit.application.port.in;

import com.opspulse.audit.domain.AuditEvent;
import com.opspulse.shared.web.PagedResponse;
import java.util.Optional;
import java.util.UUID;

public interface QueryAuditEventsUseCase {

    PagedResponse<AuditEvent> findAll(
            int page, int size, String entityType, UUID entityId);

    Optional<AuditEvent> findById(UUID id);
}
