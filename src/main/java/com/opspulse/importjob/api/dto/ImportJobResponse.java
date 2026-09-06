package com.opspulse.importjob.api.dto;

import com.opspulse.importjob.domain.ImportJob;
import java.time.Instant;
import java.util.UUID;

public record ImportJobResponse(UUID id, String idempotencyKey, String fileHash, String importType,
                                String status, Integer totalRows, Integer successRows, Integer errorRows,
                                Instant startedAt, Instant finishedAt, Instant createdAt) {
    public static ImportJobResponse from(ImportJob job) {
        return new ImportJobResponse(job.id(), job.idempotencyKey(), job.fileHash(), job.importType().wireValue(),
                job.status().name(), job.totalRows(), job.successRows(), job.errorRows(), job.startedAt(),
                job.finishedAt(), job.createdAt());
    }
}
