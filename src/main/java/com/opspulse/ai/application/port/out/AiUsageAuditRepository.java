package com.opspulse.ai.application.port.out;

import com.opspulse.ai.domain.AiUsageAudit;

public interface AiUsageAuditRepository {
    void save(AiUsageAudit audit);
}
