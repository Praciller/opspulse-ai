package com.opspulse.integration.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@ActiveProfiles("prod")
@SpringBootTest
class ProdProfileFlywayIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("opspulse_prod_test")
                    .withUsername("opspulse")
                    .withPassword("opspulse");

    static {
        POSTGRES.start();
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @DynamicPropertySource
    static void prodProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add(
                "opspulse.security.jwt.secret",
                () -> "production-like-secret-with-at-least-32-characters");
    }

    @Test
    void prodProfileAppliesMigrationsWithoutDemoSeed() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("11");

        Integer demoUsers = jdbcTemplate.queryForObject(
                "select count(*) from users where email like '%@opspulse.demo'",
                Integer.class);
        Integer auditTables = jdbcTemplate.queryForObject(
                """
                select count(*)
                  from information_schema.tables
                 where table_schema = 'public'
                   and table_name = 'audit_logs'
                """,
                Integer.class);

        assertThat(demoUsers).isZero();
        assertThat(auditTables).isEqualTo(1);
    }
}
