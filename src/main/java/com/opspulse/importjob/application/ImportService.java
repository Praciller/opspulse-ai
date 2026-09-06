package com.opspulse.importjob.application;

import com.opspulse.audit.application.port.in.RecordAuditEventUseCase;
import com.opspulse.audit.domain.AuditEvent;
import com.opspulse.importjob.application.port.in.ImportUseCase;
import com.opspulse.importjob.application.port.out.ImportJobRepository;
import com.opspulse.importjob.domain.ImportJob;
import com.opspulse.importjob.domain.ImportJobStatus;
import com.opspulse.importjob.domain.ImportType;
import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import com.opspulse.shared.web.PagedResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class ImportService implements ImportUseCase {

    public static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
    private final ImportJobRepository jobs;
    private final ImportProcessor processor;
    private final RecordAuditEventUseCase audit;
    private final Executor executor;
    private final Clock clock;
    private final MeterRegistry metrics;

    public ImportService(ImportJobRepository jobs, ImportProcessor processor,
                         RecordAuditEventUseCase audit, @Qualifier("importTaskExecutor") Executor importTaskExecutor,
                         Clock clock, MeterRegistry metrics) {
        this.jobs = jobs;
        this.processor = processor;
        this.audit = audit;
        this.executor = importTaskExecutor;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Override
    public ImportJob start(StartImportCommand command) {
        if (command == null || command.actorUserId() == null) {
            throw invalid("actorUserId is required");
        }
        String key = command.idempotencyKey() == null ? "" : command.idempotencyKey().trim();
        if (key.isBlank() || key.length() > 128) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must contain 1 to 128 characters");
        }
        if (command.type() == null) {
            throw new ApiException(ErrorCode.IMPORT_TYPE_INVALID, HttpStatus.BAD_REQUEST, "Import type is required");
        }
        byte[] content = command.content();
        if (content == null || content.length == 0) {
            throw new ApiException(ErrorCode.IMPORT_FILE_INVALID, HttpStatus.BAD_REQUEST, "CSV file must not be empty");
        }
        if (content.length > MAX_FILE_BYTES) {
            throw new ApiException(ErrorCode.IMPORT_FILE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE,
                    "CSV file exceeds the 10 MiB limit");
        }
        ImportJob replay = jobs.findByIdempotencyKey(key).orElse(null);
        if (replay != null) {
            return replay;
        }
        String hash = sha256(content);
        if (jobs.findByContent(command.type(), hash).isPresent()) {
            throw new ApiException(ErrorCode.IMPORT_DUPLICATE_CONTENT, HttpStatus.CONFLICT,
                    "An import with the same content and type already exists");
        }
        Instant now = clock.instant();
        ImportJob job = new ImportJob(UUID.randomUUID(), key, hash, command.type(), ImportJobStatus.PENDING,
                null, null, null, null, null, now, command.actorUserId());
        jobs.save(job);
        metrics.counter("opspulse.import.started", "type", command.type().wireValue()).increment();
        audit.record(new AuditEvent(UUID.randomUUID(), command.actorUserId(), "import.start", "IMPORT_JOB", job.id(),
                null, snapshot(job), command.requestId(), command.ipAddress(), now));
        executor.execute(() -> processor.process(job, content, command.requestId(), command.ipAddress()));
        return job;
    }

    @Override
    public PagedResponse<ImportJob> findAll(int page, int size) {
        return jobs.findAll(page, size);
    }

    @Override
    public ImportJob findById(UUID id) {
        return jobs.findById(id).orElseThrow(() -> new ApiException(ErrorCode.IMPORT_JOB_NOT_FOUND,
                HttpStatus.NOT_FOUND, "Import job not found"));
    }

    @Override
    public PagedResponse<ImportRowError> findErrors(UUID id, int page, int size) {
        findById(id);
        return jobs.findErrors(id, page, size);
    }

    private static ApiException invalid(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash import file", exception);
        }
    }

    static java.util.Map<String, Object> snapshot(ImportJob job) {
        return java.util.Map.of(
                "id", job.id().toString(),
                "importType", job.importType().wireValue(),
                "status", job.status().name(),
                "totalRows", java.util.Objects.requireNonNullElse(job.totalRows(), 0),
                "successRows", java.util.Objects.requireNonNullElse(job.successRows(), 0),
                "errorRows", java.util.Objects.requireNonNullElse(job.errorRows(), 0));
    }
}
