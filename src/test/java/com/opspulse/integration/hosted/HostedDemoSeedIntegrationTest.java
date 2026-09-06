package com.opspulse.integration.hosted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.opspulse.demo.HostedDemoSeedRunner;
import com.opspulse.identity.application.port.out.PasswordHasher;
import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Proves the hosted public demo security model: exactly one least-privilege VIEWER
 * account whose credential comes from environment injection (never the repository's
 * documented local demo password), synthetic credential-free business data, and
 * idempotent re-seeding.
 */
@ActiveProfiles({"test", "hosted-demo"})
@SpringBootTest
class HostedDemoSeedIntegrationTest extends PostgresIntegrationTestSupport {

    // Distinct from the test-profile seed users: the runner must refuse to
    // hijack an account it does not own, so this address is hosted-demo-only.
    private static final String VIEWER_EMAIL = "hosted-viewer@opspulse.demo";
    // Generated per test run: no credential value is stored in source.
    private static final String VIEWER_PASSWORD = "viewer-" + UUID.randomUUID() + "-demo";

    @Autowired
    private HostedDemoSeedRunner runner;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void hostedDemoProperties(DynamicPropertyRegistry registry) {
        registry.add("HOSTED_DEMO_VIEWER_EMAIL", () -> VIEWER_EMAIL);
        registry.add("HOSTED_DEMO_VIEWER_PASSWORD", () -> VIEWER_PASSWORD);
    }

    @Test
    void seedsOneLeastPrivilegeViewerAndSyntheticBusinessData() {
        assertViewerRow();
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM products WHERE sku LIKE 'OPS-%'",
                        Integer.class))
                .isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM suppliers", Integer.class))
                .isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM orders", Integer.class))
                .isEqualTo(12);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM purchase_orders", Integer.class))
                .isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM inventory_movements", Integer.class))
                .isEqualTo(20);
    }

    @Test
    void movementChainBalancesMatchCurrentStock() {
        var mismatches = jdbcTemplate.query(
                """
                SELECT p.sku
                FROM products p
                LEFT JOIN LATERAL (
                    SELECT balance_after FROM inventory_movements m
                    WHERE m.product_id = p.id
                    ORDER BY created_at DESC, id DESC LIMIT 1
                ) last ON TRUE
                WHERE p.sku LIKE 'OPS-%'
                  AND p.current_stock <> COALESCE(last.balance_after, 0)
                """,
                (row, index) -> row.getString("sku"));
        assertThat(mismatches).isEmpty();
    }

    @Test
    void reseedingIsIdempotent() {
        runner.run(null);

        assertViewerRow();
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM users WHERE email = ?",
                        Integer.class, VIEWER_EMAIL))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM products WHERE sku LIKE 'OPS-%'",
                        Integer.class))
                .isEqualTo(10);
    }

    @Test
    void missingEnvironmentFailsFast() {
        var withoutConfig = runnerWith(new MockEnvironment());
        assertThatThrownBy(() -> withoutConfig.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HOSTED_DEMO_VIEWER_EMAIL");
    }

    @Test
    void rejectsShortHostedPassword() {
        var shortPasswordEnv = new MockEnvironment();
        shortPasswordEnv.setProperty("HOSTED_DEMO_VIEWER_EMAIL", VIEWER_EMAIL);
        shortPasswordEnv.setProperty("HOSTED_DEMO_VIEWER_PASSWORD", "short");
        assertThatThrownBy(() -> runnerWith(shortPasswordEnv).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 12");
    }

    private HostedDemoSeedRunner runnerWith(MockEnvironment environment) {
        return new HostedDemoSeedRunner(passwordHasher, jdbcTemplate, environment);
    }

    private void assertViewerRow() {
        var row = jdbcTemplate.queryForMap(
                """
                SELECT u.active, u.password_hash,
                       STRING_AGG(r.name, ',' ORDER BY r.name) AS roles
                FROM users u
                JOIN user_roles ur ON ur.user_id = u.id
                JOIN roles r ON r.id = ur.role_id
                WHERE u.email = ?
                GROUP BY u.active, u.password_hash
                """,
                VIEWER_EMAIL);
        assertThat((String) row.get("roles")).isEqualTo("VIEWER");
        assertThat(row.get("active")).isEqualTo(true);
        assertThat(passwordHasher.matches(VIEWER_PASSWORD, (String) row.get("password_hash")))
                .isTrue();
    }
}
