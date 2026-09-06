package com.opspulse.risk.domain;

public class InvalidRiskStatusTransitionException extends RuntimeException {

    public InvalidRiskStatusTransitionException(String message) {
        super(message);
    }
}
