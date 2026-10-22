package com.opspulse.integration.phase6;

import static org.assertj.core.api.Assertions.assertThat;

import com.opspulse.importjob.infrastructure.ImportStartupReconciler;
import com.opspulse.integration.support.PostgresIntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class ImportStartupReconcilerIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ImportStartupReconciler reconciler;

    @Test
    void interruptedImportsAreFailedButCompletedImportsRemainUntouched() {
        UUID pending = UUID.randomUUID();
        UUID completed = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        insertJob(pending, actor, "PROCESSING");
        insertJob(completed, actor, "COMPLETED");
        try {
            ApplicationArguments args = new DefaultApplicationArguments(new String[0]);
            reconciler.run(args);
            assertThat(status(pending)).isEqualTo("FAILED");
            assertThat(jdbc.queryForObject(
                    "select finished_at is not null from import_jobs where id = ?",
                    Boolean.class, pending)).isTrue();
            assertThat(status(completed)).isEqualTo("COMPLETED");
            reconciler.run(args);
            assertThat(status(pending)).isEqualTo("FAILED");
        } finally {
            jdbc.update("delete from import_jobs where id in (?, ?)", pending, completed);
        }
    }

    private void insertJob(UUID id, UUID actor, String status) {
        jdbc.update("""
                insert into import_jobs (id, idempotency_key, file_hash, import_type, status, created_by)
                values (?, ?, ?, 'products', ?, ?)
                """, id, "reconcile-" + id, id.toString().replace("-", ""), status, actor);
    }

    private String status(UUID id) {
        return jdbc.queryForObject("select status from import_jobs where id = ?", String.class, id);
    }
}
