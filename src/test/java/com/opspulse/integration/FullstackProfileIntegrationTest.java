package com.opspulse.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("fullstack")
@SpringBootTest(properties =
        "opspulse.security.jwt.secret=test-only-jwt-secret-with-at-least-32-characters")
class FullstackProfileIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private Environment environment;

    @Test
    void fullstackProfileIncludesLocalFoundation() {
        assertThat(environment.acceptsProfiles(Profiles.of("local"))).isTrue();
    }
}
