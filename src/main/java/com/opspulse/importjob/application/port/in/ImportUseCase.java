package com.opspulse.importjob.application.port.in;

import com.opspulse.importjob.domain.ImportJob;
import com.opspulse.importjob.domain.ImportType;
import com.opspulse.shared.web.PagedResponse;
import java.util.UUID;

public interface ImportUseCase {

    ImportJob start(StartImportCommand command);

    PagedResponse<ImportJob> findAll(int page, int size);

    ImportJob findById(UUID id);

    PagedResponse<ImportRowError> findErrors(UUID id, int page, int size);

    record StartImportCommand(ImportType type, String idempotencyKey, byte[] content,
                              UUID actorUserId, String requestId, String ipAddress) {}

    record ImportRowError(UUID id, UUID jobId, int rowNumber, String rawRow,
                          String errorCode, String errorMessage, java.time.Instant createdAt) {}
}
