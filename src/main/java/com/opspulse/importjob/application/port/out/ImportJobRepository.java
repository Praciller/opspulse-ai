package com.opspulse.importjob.application.port.out;

import com.opspulse.importjob.application.port.in.ImportUseCase.ImportRowError;
import com.opspulse.importjob.domain.ImportJob;
import com.opspulse.importjob.domain.ImportJobStatus;
import com.opspulse.importjob.domain.ImportType;
import com.opspulse.shared.web.PagedResponse;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ImportJobRepository {
    ImportJob save(ImportJob job);
    Optional<ImportJob> findByIdempotencyKey(String key);
    Optional<ImportJob> findByContent(ImportType type, String fileHash);
    Optional<ImportJob> findById(UUID id);
    PagedResponse<ImportJob> findAll(int page, int size);
    PagedResponse<ImportRowError> findErrors(UUID jobId, int page, int size);
    void updateStatus(UUID id, ImportJobStatus status, Integer totalRows, Integer successRows,
                      Integer errorRows, Instant startedAt, Instant finishedAt);
    void addError(ImportRowError error);
}
