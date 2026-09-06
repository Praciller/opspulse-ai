package com.opspulse.outbox.application;

import com.opspulse.audit.application.port.in.RecordAuditEventUseCase;
import com.opspulse.audit.domain.AuditEvent;
import com.opspulse.outbox.application.port.out.OutboxDeliveryRepository;
import com.opspulse.outbox.domain.OutboxDelivery;
import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxAdminService {

    private final OutboxDeliveryRepository repository;
    private final RecordAuditEventUseCase audit;
    private final Clock clock;

    public OutboxAdminService(OutboxDeliveryRepository repository, RecordAuditEventUseCase audit, Clock clock) {
        this.repository = repository;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public OutboxDelivery replay(UUID id, UUID actor, String requestId, String ipAddress) {
        var row = findDead(id);
        repository.replay(id, clock.instant());
        audit.record(new AuditEvent(UUID.randomUUID(), actor, "outbox.replay", "OUTBOX_EVENT", id,
                java.util.Map.of("status", row.status(), "retryCount", row.retryCount()),
                java.util.Map.of("status", "RETRY", "retryCount", 0), requestId, ipAddress, clock.instant()));
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public OutboxDelivery discard(UUID id, UUID actor, String requestId, String ipAddress) {
        var row = findDead(id);
        repository.discard(id, "discarded by administrator");
        audit.record(new AuditEvent(UUID.randomUUID(), actor, "outbox.discard", "OUTBOX_EVENT", id,
                java.util.Map.of("status", row.status(), "retryCount", row.retryCount()),
                java.util.Map.of("status", "DEAD", "discarded", true), requestId, ipAddress, clock.instant()));
        return repository.findById(id).orElseThrow();
    }

    private OutboxDelivery findDead(UUID id) {
        var row = repository.findById(id).orElseThrow(() -> new ApiException(
                ErrorCode.OUTBOX_EVENT_NOT_FOUND, HttpStatus.NOT_FOUND, "Outbox event not found"));
        if (!"DEAD".equals(row.status())) {
            throw new ApiException(ErrorCode.OUTBOX_NOT_REPLAYABLE, HttpStatus.CONFLICT,
                    "Only DEAD outbox events can be changed");
        }
        return row;
    }
}
