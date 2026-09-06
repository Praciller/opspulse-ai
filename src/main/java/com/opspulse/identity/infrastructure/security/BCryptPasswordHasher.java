package com.opspulse.identity.infrastructure.security;

import com.opspulse.identity.application.port.out.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private static final String DUMMY_PASSWORD = "constant-style-dummy-password";

    private final BCryptPasswordEncoder encoder;
    private final String dummyHash;

    public BCryptPasswordHasher(IdentitySecurityProperties properties) {
        this.encoder = new BCryptPasswordEncoder(properties.bcryptStrength());
        this.dummyHash = encoder.encode(DUMMY_PASSWORD);
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }

    @Override
    public String dummyHash() {
        return dummyHash;
    }
}
