package com.opspulse.identity.application;

public final class IdentityAuthorizationException extends RuntimeException {

    public IdentityAuthorizationException() {
        super("Access is denied");
    }
}
