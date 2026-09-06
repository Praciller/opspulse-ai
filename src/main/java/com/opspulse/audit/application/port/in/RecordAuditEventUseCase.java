package com.opspulse.audit.application.port.in;

import com.opspulse.audit.domain.AuditEvent;

public interface RecordAuditEventUseCase {

    void record(AuditEvent event);

    void recordInNewTransaction(AuditEvent event);
}
