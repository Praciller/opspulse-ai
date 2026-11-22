package com.opspulse.importjob.infrastructure;

import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Single-instance safety net for the current in-memory import executor.
 * Upload bytes are not durable, so interrupted jobs cannot be resumed after a restart.
 * Mark them failed rather than leaving misleading PENDING/PROCESSING states forever.
 * This does not roll back rows already committed by a partially completed import.
 */
@Component
public class ImportStartupReconciler implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportStartupReconciler.class);

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final MeterRegistry metrics;

    public ImportStartupReconciler(JdbcTemplate jdbc, Clock clock, MeterRegistry metrics) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Override
    public void run(ApplicationArguments args) {
        int abandoned = jdbc.update("""
                UPDATE import_jobs
                   SET status = 'FAILED', finished_at = ?
                 WHERE status IN ('PENDING', 'PROCESSING')
                """, Timestamp.from(clock.instant()));
        if (abandoned > 0) {
            metrics.counter("opspulse.import.abandoned_on_startup").increment(abandoned);
            log.warn("Marked {} interrupted import jobs FAILED after application restart", abandoned);
        }
    }
}
