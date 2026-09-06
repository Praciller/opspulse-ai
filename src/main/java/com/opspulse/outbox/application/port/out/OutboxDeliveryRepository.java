package com.opspulse.outbox.application.port.out;

import com.opspulse.outbox.domain.OutboxDelivery;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OutboxDeliveryRepository {

    Optional<OutboxDelivery> lockNextDue(Instant now);

    boolean markProcessedEventIfNew(UUID eventId, Instant processedAt);

    void deleteProcessedEvent(UUID eventId);

    void markProcessed(UUID outboxId, Instant processedAt);

    void markFailure(UUID outboxId, int retryCount, String status, Instant nextAttemptAt, String errorMessage);

    void replay(UUID outboxId, Instant nextAttemptAt);

    void discard(UUID outboxId, String errorMessage);

    Optional<OutboxDelivery> findById(UUID id);
}
