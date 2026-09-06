package com.opspulse.audit.application;

import com.opspulse.audit.application.port.in.QueryAuditEventsUseCase;
import com.opspulse.audit.application.port.out.AuditEventRepository;
import com.opspulse.audit.domain.AuditEvent;
import com.opspulse.shared.web.PagedResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QueryAuditEventsService implements QueryAuditEventsUseCase {

    private final AuditEventRepository repository;

    public QueryAuditEventsService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public PagedResponse<AuditEvent> findAll(
            int page, int size, String entityType, UUID entityId) {
        return repository.findAll(page, size, entityType, entityId);
    }

    @Override
    public Optional<AuditEvent> findById(UUID id) {
        return repository.findById(id);
    }
}
