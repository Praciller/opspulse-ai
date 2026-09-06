package com.opspulse.importjob.domain;

import java.time.Instant;
import java.util.UUID;

public record ImportJob(
        UUID id,
        String idempotencyKey,
        String fileHash,
        ImportType importType,
        ImportJobStatus status,
        Integer totalRows,
        Integer successRows,
        Integer errorRows,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt,
        UUID createdBy) {

    public ImportJob {
        if (id == null || idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128
                || fileHash == null || fileHash.length() != 64 || importType == null || status == null
                || createdAt == null || createdBy == null) {
            throw new IllegalArgumentException("Invalid import job");
        }
    }
}
