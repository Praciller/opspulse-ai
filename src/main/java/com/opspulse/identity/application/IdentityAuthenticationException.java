package com.opspulse.identity.application;

public final class IdentityAuthenticationException extends RuntimeException {

    public IdentityAuthenticationException() {
        super("Invalid email or password");
    }
}
