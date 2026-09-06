package com.opspulse.integration.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class IdentityFlywayMigrationIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Test
    void appliesIdentityAndAuditMigrationsToPostgresql() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("11");
        assertThat(flyway.info().all())
                .filteredOn(info -> info.getVersion() != null)
                .extracting(info -> info.getVersion().getVersion())
                .containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");

        List<String> tables = jdbcTemplate.queryForList(
                """
                select table_name
                  from information_schema.tables
                 where table_schema = 'public'
                   and table_name in ('users','roles','user_roles','refresh_tokens','audit_logs')
                """,
                String.class);
        assertThat(tables)
                .containsExactlyInAnyOrder(
                        "users", "roles", "user_roles", "refresh_tokens", "audit_logs");
    }

    @Test
    void createsCitextFamilyIdAndRequiredIdentityIndexes() {
        Integer citext = jdbcTemplate.queryForObject(
                "select count(*) from pg_extension where extname = 'citext'", Integer.class);
        Integer familyIdColumn = jdbcTemplate.queryForObject(
                """
                select count(*)
                  from information_schema.columns
                 where table_schema = 'public'
                   and table_name = 'refresh_tokens'
                   and column_name = 'family_id'
                """,
                Integer.class);
        List<String> indexes = jdbcTemplate.queryForList(
                """
                select indexname
                  from pg_indexes
                 where schemaname = 'public'
                   and indexname in (
                       'idx_users_email_lower',
                       'idx_refresh_tokens_token_hash',
                       'idx_refresh_tokens_user_expires',
                       'idx_refresh_tokens_family_id',
                       'idx_audit_logs_actor_created',
                       'idx_audit_logs_entity_created',
                       'idx_audit_logs_request_id'
                   )
                """,
                String.class);

        assertThat(citext).isEqualTo(1);
        assertThat(familyIdColumn).isEqualTo(1);
        assertThat(indexes)
                .containsExactlyInAnyOrder(
                        "idx_users_email_lower",
                        "idx_refresh_tokens_token_hash",
                        "idx_refresh_tokens_user_expires",
                        "idx_refresh_tokens_family_id",
                        "idx_audit_logs_actor_created",
                        "idx_audit_logs_entity_created",
                        "idx_audit_logs_request_id");
    }
}
