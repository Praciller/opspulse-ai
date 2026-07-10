package com.opspulse.shared.error;

import com.opspulse.shared.observability.RequestIdContext;
import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        ErrorCode code,
        String message,
        List<FieldErrorResponse> fields,
        String requestId,
        Instant timestamp) {

    public ApiErrorResponse {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    public static ApiErrorResponse of(
            ErrorCode code,
            String message,
            List<FieldErrorResponse> fields) {
        return new ApiErrorResponse(
                code,
                message,
                fields,
                RequestIdContext.currentRequestId(),
                Instant.now());
    }

    public static ApiErrorResponse of(ErrorCode code, String message) {
        return of(code, message, List.of());
    }
}
