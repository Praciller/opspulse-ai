package com.opspulse.importjob.api.dto;

import com.opspulse.importjob.application.port.in.ImportUseCase.ImportRowError;
import java.time.Instant;
import java.util.UUID;

public record ImportRowErrorResponse(UUID id, int rowNumber, String rawRow, String errorCode,
                                     String errorMessage, Instant createdAt) {
    public static ImportRowErrorResponse from(ImportRowError error) {
        return new ImportRowErrorResponse(error.id(), error.rowNumber(), error.rawRow(), error.errorCode(),
                error.errorMessage(), error.createdAt());
    }
}
