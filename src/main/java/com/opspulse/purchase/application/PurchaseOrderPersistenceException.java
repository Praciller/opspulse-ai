package com.opspulse.purchase.application;

public class PurchaseOrderPersistenceException extends RuntimeException {

    public enum Reason { DUPLICATE_NUMBER, STALE_VERSION }

    private final Reason reason;

    public PurchaseOrderPersistenceException(Reason reason) {
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
