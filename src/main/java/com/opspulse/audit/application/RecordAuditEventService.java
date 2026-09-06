package com.opspulse.audit.application;

import com.opspulse.audit.application.port.in.RecordAuditEventUseCase;
import com.opspulse.audit.application.port.out.AuditEventRepository;
import com.opspulse.audit.domain.AuditEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordAuditEventService implements RecordAuditEventUseCase {

    private final AuditEventRepository repository;

    public RecordAuditEventService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void record(AuditEvent event) {
        repository.save(event);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordInNewTransaction(AuditEvent event) {
        repository.save(event);
    }
}
