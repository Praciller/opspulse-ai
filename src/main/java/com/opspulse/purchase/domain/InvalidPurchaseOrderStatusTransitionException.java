package com.opspulse.purchase.domain;

public class InvalidPurchaseOrderStatusTransitionException extends RuntimeException {

    public InvalidPurchaseOrderStatusTransitionException(String message) {
        super(message);
    }
}
