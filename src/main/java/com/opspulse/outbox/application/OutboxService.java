package com.opspulse.outbox.application;

import com.opspulse.outbox.application.port.in.OutboxPublisher;
import com.opspulse.outbox.application.port.out.CloudEventsEnvelopeFactory;
import com.opspulse.outbox.application.port.out.OutboxEventRepository;
import com.opspulse.outbox.domain.OutboxEvent;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService implements OutboxPublisher {

    private final OutboxEventRepository repository;
    private final CloudEventsEnvelopeFactory envelopeFactory;
    private final Clock clock;

    public OutboxService(
            OutboxEventRepository repository,
            CloudEventsEnvelopeFactory envelopeFactory,
            Clock clock) {
        this.repository = repository;
        this.envelopeFactory = envelopeFactory;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID publish(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            Map<String, Object> data) {
        UUID eventId = UUID.randomUUID();
        repository.save(new OutboxEvent(
                eventId,
                aggregateType,
                aggregateId,
                eventType,
                envelopeFactory.build(eventId, aggregateType, aggregateId, eventType, data),
                clock.instant()));
        return eventId;
    }
}
