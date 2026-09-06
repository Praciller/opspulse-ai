package com.opspulse.identity.application.port.out;

public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);

    String dummyHash();
}
