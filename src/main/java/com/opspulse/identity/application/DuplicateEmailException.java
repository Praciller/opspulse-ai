package com.opspulse.identity.application;

public final class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("A user with this email already exists");
    }
}
