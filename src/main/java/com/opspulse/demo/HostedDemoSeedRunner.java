package com.opspulse.demo;

import com.opspulse.ai.application.port.in.BriefUseCase;
import com.opspulse.identity.application.port.out.PasswordHasher;
import com.opspulse.risk.application.port.in.RiskUseCase;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

/**
 * Seeds the hosted public demo, activated only by the {@code hosted-demo} profile.
 *
 * <p>Security model: the public repository documents local demo credentials, so the
 * hosted demo must never reuse them. This runner creates (or rotates) exactly one
 * least-privilege VIEWER account whose password is injected through the
 * {@code HOSTED_DEMO_VIEWER_PASSWORD} environment variable and never logged, then
 * loads synthetic business data that contains no credentials. No ADMIN, MANAGER, or
 * OPERATOR account is created, and {@code REGISTRATION_MODE} stays ADMIN_ONLY.
 *
 * <p>The upsert and the seed deliberately share one JDBC channel so the seeded
 * movement rows can reference the viewer account inside the same transaction.
 */
@Component
@Profile("hosted-demo")
public class HostedDemoSeedRunner implements ApplicationRunner {

    static final UUID VIEWER_USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    static final UUID MARKER_PRODUCT_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final String VIEWER_FULL_NAME = "Hosted Demo Viewer";
    private static final String BUSINESS_DATA_LOCATION =
            "db/demo-hosted/hosted_demo_business_data.sql";
    private static final String VIEWER_ID_PLACEHOLDER = "${VIEWER_USER_ID}";
    private static final int MINIMUM_PASSWORD_LENGTH = 12;

    private final PasswordHasher passwordHasher;
    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;
    private final RiskUseCase risks;
    private final BriefUseCase briefs;

    public HostedDemoSeedRunner(
            PasswordHasher passwordHasher, JdbcTemplate jdbcTemplate, Environment environment,
            RiskUseCase risks, BriefUseCase briefs) {
        this.passwordHasher = passwordHasher;
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
        this.risks = risks;
        this.briefs = briefs;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        upsertViewerUser(
                requiredProperty("HOSTED_DEMO_VIEWER_EMAIL"),
                requiredProperty("HOSTED_DEMO_VIEWER_PASSWORD"));
        seedBusinessData();
        bootstrapRiskAndBrief();
    }

    private void upsertViewerUser(String email, String password) {
        if (password.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "HOSTED_DEMO_VIEWER_PASSWORD must be at least "
                            + MINIMUM_PASSWORD_LENGTH + " characters");
        }
        UUID existingId = jdbcTemplate.query(
                "SELECT id FROM users WHERE email = ?",
                resultSet -> resultSet.next()
                        ? resultSet.getObject("id", UUID.class)
                        : null,
                email);
        if (existingId != null && !VIEWER_USER_ID.equals(existingId)) {
            throw new IllegalStateException(
                    "HOSTED_DEMO_VIEWER_EMAIL already belongs to an existing account; "
                            + "refusing to modify it for the hosted demo");
        }
        String hash = passwordHasher.hash(password);
        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password_hash, full_name, active,
                                   created_at, created_by, updated_at, updated_by)
                VALUES (?, ?, ?, ?, TRUE, now(), ?, now(), ?)
                ON CONFLICT (email) DO UPDATE
                SET password_hash = EXCLUDED.password_hash, updated_at = now()
                """,
                VIEWER_USER_ID, email, hash, VIEWER_FULL_NAME, VIEWER_USER_ID, VIEWER_USER_ID);
        jdbcTemplate.update(
                "DELETE FROM user_roles WHERE user_id = ? AND role_id <> "
                        + "(SELECT id FROM roles WHERE name = 'VIEWER')",
                VIEWER_USER_ID);
        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id) VALUES (?, "
                        + "(SELECT id FROM roles WHERE name = 'VIEWER')) ON CONFLICT DO NOTHING",
                VIEWER_USER_ID);
    }

    private void bootstrapRiskAndBrief() {
        // Re-evaluate deterministic rules on every demo boot. The active-risk dedup
        // index makes this idempotent while keeping a cold-started demo current.
        risks.scan();
        Integer briefCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ai_usage_audit WHERE request_id = 'hosted-demo-bootstrap'",
                Integer.class);
        if (briefCount != null && briefCount == 0) {
            briefs.generate(5, null, null, "hosted-demo-bootstrap", "system");
        }
    }

    private void seedBusinessData() {
        // Scope the guard to this seed's marker row: the instance may legally hold
        // unrelated products (e.g., created through API flows or other tests).
        Integer markerCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM products WHERE id = ?",
                Integer.class, MARKER_PRODUCT_ID);
        if (markerCount != null && markerCount > 0) {
            return;
        }
        String script = loadBusinessDataScript();
        // ConnectionCallback supplies the transaction-bound connection, so the seed
        // sees the viewer account inserted earlier in this transaction.
        jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            ScriptUtils.executeSqlScript(
                    connection,
                    new org.springframework.core.io.ByteArrayResource(
                            script.getBytes(StandardCharsets.UTF_8)));
            return null;
        });
    }

    private String loadBusinessDataScript() {
        try {
            var resource = new ClassPathResource(BUSINESS_DATA_LOCATION);
            String script = FileCopyUtils.copyToString(
                    new java.io.InputStreamReader(
                            resource.getInputStream(), StandardCharsets.UTF_8));
            return script.replace(VIEWER_ID_PLACEHOLDER, VIEWER_USER_ID.toString());
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Hosted demo business data script is missing: " + BUSINESS_DATA_LOCATION,
                    exception);
        }
    }

    private String requiredProperty(String name) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "hosted-demo profile requires the " + name + " environment variable");
        }
        return value.trim();
    }
}
