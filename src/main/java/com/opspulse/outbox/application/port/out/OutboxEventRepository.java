package com.opspulse.outbox.application.port.out;

import com.opspulse.outbox.domain.OutboxEvent;

public interface OutboxEventRepository {

    void save(OutboxEvent event);
}
