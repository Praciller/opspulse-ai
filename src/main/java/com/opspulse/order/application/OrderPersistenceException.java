package com.opspulse.order.application;

public class OrderPersistenceException extends RuntimeException {

    public enum Reason { DUPLICATE_NUMBER, STALE_VERSION }

    private final Reason reason;

    public OrderPersistenceException(Reason reason) {
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
