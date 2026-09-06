package com.opspulse.integration.phase6;

import static org.assertj.core.api.Assertions.assertThat;

import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class Phase6MigrationIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsImportTablesAndAppliesV11AfterV10() {
        assertThat(jdbc.queryForList("""
                select table_name from information_schema.tables where table_schema = 'public'
                  and table_name in ('import_jobs', 'import_row_errors') order by table_name
                """, String.class)).containsExactly("import_jobs", "import_row_errors");
        assertThat(jdbc.queryForList("select version from flyway_schema_history where success order by installed_rank", String.class))
                .containsSubsequence("10", "11");
        assertThat(jdbc.queryForList("select indexname from pg_indexes where tablename = 'import_jobs'", String.class))
                .contains("idx_import_jobs_content", "idx_import_jobs_created", "idx_import_jobs_status_created");
    }

    @Test
    void storesRowErrorsWithCascadeDelete() {
        var jobId = java.util.UUID.randomUUID();
        jdbc.update("""
                insert into import_jobs(id, idempotency_key, file_hash, import_type, status, created_by)
                values (?, ?, ?, 'products', 'COMPLETED', ?)
                """, jobId, "migration-" + jobId, "a".repeat(64),
                java.util.UUID.fromString("10000000-0000-0000-0000-000000000001"));
        jdbc.update("""
                insert into import_row_errors(id, import_job_id, row_number, raw_row, error_code, error_message)
                values (?, ?, 2, 'bad,row', 'IMPORT_ROW_INVALID', 'invalid value')
                """, java.util.UUID.randomUUID(), jobId);
        assertThat(jdbc.queryForObject("select count(*) from import_row_errors where import_job_id = ?", Integer.class, jobId))
                .isEqualTo(1);
        jdbc.update("delete from import_jobs where id = ?", jobId);
        assertThat(jdbc.queryForObject("select count(*) from import_row_errors where import_job_id = ?", Integer.class, jobId))
                .isZero();
    }
}
