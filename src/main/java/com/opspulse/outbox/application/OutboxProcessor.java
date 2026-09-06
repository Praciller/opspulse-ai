package com.opspulse.outbox.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxProcessor {

    private static final int BATCH_SIZE = 50;

    private final OutboxDeliveryService delivery;

    public OutboxProcessor(OutboxDeliveryService delivery) {
        this.delivery = delivery;
    }

    @Scheduled(fixedDelayString = "${opspulse.outbox.poll-interval-ms:${OUTBOX_POLL_INTERVAL_MS:5000}}")
    public void poll() {
        for (int i = 0; i < BATCH_SIZE; i++) {
            if (!delivery.processOne()) {
                return;
            }
        }
    }
}
