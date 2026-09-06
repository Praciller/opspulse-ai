package com.opspulse.integration.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class IdentityPersistenceIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void databaseEnforcesCaseInsensitiveUniqueEmail() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        insert into users (
                            id, email, password_hash, full_name, active,
                            created_at, updated_at, version
                        ) values (?, ?, ?, ?, true, now(), now(), 0)
                        """,
                        UUID.randomUUID(),
                        "ADMIN@OPSPULSE.DEMO",
                        "$2a$12$not-a-real-hash",
                        "Duplicate Admin"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void seededPasswordsAreStoredOnlyAsBcryptHashes() {
        String stored = jdbcTemplate.queryForObject(
                "select password_hash from users where email = ?",
                String.class,
                "admin@opspulse.demo");

        assertThat(stored).startsWith("$2");
        assertThat(stored).doesNotContain("OpsPulseDemo!2026");
    }
}
