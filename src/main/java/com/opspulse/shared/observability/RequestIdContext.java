package com.opspulse.shared.observability;

import org.slf4j.MDC;

public final class RequestIdContext {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    public static final String USER_ID_MDC_KEY = "userId";

    private RequestIdContext() {
    }

    public static String currentRequestId() {
        var requestId = MDC.get(MDC_KEY);
        return requestId == null || requestId.isBlank() ? "unknown" : requestId;
    }

    public static String currentUserId() {
        return MDC.get(USER_ID_MDC_KEY);
    }
}
