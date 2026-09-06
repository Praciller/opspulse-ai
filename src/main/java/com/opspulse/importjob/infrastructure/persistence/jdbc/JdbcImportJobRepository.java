package com.opspulse.importjob.infrastructure.persistence.jdbc;

import com.opspulse.importjob.application.port.in.ImportUseCase.ImportRowError;
import com.opspulse.importjob.application.port.out.ImportJobRepository;
import com.opspulse.importjob.domain.ImportJob;
import com.opspulse.importjob.domain.ImportJobStatus;
import com.opspulse.importjob.domain.ImportType;
import com.opspulse.shared.web.PagedResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcImportJobRepository implements ImportJobRepository {

    private final JdbcTemplate jdbc;

    public JdbcImportJobRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ImportJob save(ImportJob job) {
        jdbc.update("""
                insert into import_jobs (id, idempotency_key, file_hash, import_type, status, created_at, created_by)
                values (?, ?, ?, ?, ?, ?, ?)
                """, job.id(), job.idempotencyKey(), job.fileHash(), job.importType().wireValue(),
                job.status().name(), Timestamp.from(job.createdAt()), job.createdBy());
        return job;
    }

    @Override
    public Optional<ImportJob> findByIdempotencyKey(String key) {
        return queryOne("select * from import_jobs where idempotency_key = ?", key);
    }

    @Override
    public Optional<ImportJob> findByContent(ImportType type, String fileHash) {
        return queryOne("select * from import_jobs where import_type = ? and file_hash = ? order by created_at desc limit 1",
                type.wireValue(), fileHash);
    }

    @Override
    public Optional<ImportJob> findById(UUID id) {
        return queryOne("select * from import_jobs where id = ?", id);
    }

    @Override
    public PagedResponse<ImportJob> findAll(int page, int size) {
        long total = jdbc.queryForObject("select count(*) from import_jobs", Long.class);
        var items = jdbc.query("""
                select * from import_jobs order by created_at desc limit ? offset ?
                """, this::mapJob, size, page * size);
        return new PagedResponse<>(items, page, size, total, totalPages(total, size));
    }

    @Override
    public PagedResponse<ImportRowError> findErrors(UUID jobId, int page, int size) {
        long total = jdbc.queryForObject("select count(*) from import_row_errors where import_job_id = ?", Long.class, jobId);
        var items = jdbc.query("""
                select * from import_row_errors where import_job_id = ? order by row_number asc limit ? offset ?
                """, this::mapError, jobId, size, page * size);
        return new PagedResponse<>(items, page, size, total, totalPages(total, size));
    }

    @Override
    public void updateStatus(UUID id, ImportJobStatus status, Integer totalRows, Integer successRows,
                             Integer errorRows, Instant startedAt, Instant finishedAt) {
        jdbc.update("""
                update import_jobs set status = ?, total_rows = ?, success_rows = ?, error_rows = ?,
                    started_at = ?, finished_at = ? where id = ?
                """, status.name(), totalRows, successRows, errorRows,
                timestamp(startedAt), timestamp(finishedAt), id);
    }

    @Override
    public void addError(ImportRowError error) {
        jdbc.update("""
                insert into import_row_errors (id, import_job_id, row_number, raw_row, error_code, error_message, created_at)
                values (?, ?, ?, ?, ?, ?, ?)
                """, error.id(), error.jobId(), error.rowNumber(), bounded(error.rawRow(), 8000),
                bounded(error.errorCode(), 64), bounded(error.errorMessage(), 512), Timestamp.from(error.createdAt()));
    }

    private Optional<ImportJob> queryOne(String sql, Object... args) {
        var rows = jdbc.query(sql, this::mapJob, args);
        return rows.stream().findFirst();
    }

    private ImportJob mapJob(ResultSet rs, int row) throws SQLException {
        return new ImportJob(
                rs.getObject("id", UUID.class),
                rs.getString("idempotency_key"),
                rs.getString("file_hash"),
                ImportType.parse(rs.getString("import_type")),
                ImportJobStatus.valueOf(rs.getString("status")),
                nullableInt(rs, "total_rows"),
                nullableInt(rs, "success_rows"),
                nullableInt(rs, "error_rows"),
                instant(rs.getTimestamp("started_at")),
                instant(rs.getTimestamp("finished_at")),
                instant(rs.getTimestamp("created_at")),
                rs.getObject("created_by", UUID.class));
    }

    private ImportRowError mapError(ResultSet rs, int row) throws SQLException {
        return new ImportRowError(rs.getObject("id", UUID.class), rs.getObject("import_job_id", UUID.class),
                rs.getInt("row_number"), bounded(rs.getString("raw_row"), 8000), rs.getString("error_code"),
                bounded(rs.getString("error_message"), 512), instant(rs.getTimestamp("created_at")));
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static Integer nullableInt(ResultSet rs, String name) throws SQLException {
        int value = rs.getInt(name);
        return rs.wasNull() ? null : value;
    }

    private static int totalPages(long total, int size) {
        return size == 0 ? 0 : (int) Math.ceil((double) total / size);
    }

    private static String bounded(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
