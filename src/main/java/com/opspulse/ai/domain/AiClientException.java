package com.opspulse.ai.domain;

public class AiClientException extends RuntimeException {

    private final boolean retryable;
    private final String errorClass;

    public AiClientException(String message, String errorClass, boolean retryable) {
        super(message);
        this.errorClass = errorClass == null ? "AI_CLIENT_FAILURE" : errorClass;
        this.retryable = retryable;
    }

    public AiClientException(String message, String errorClass, boolean retryable, Throwable cause) {
        super(message, cause);
        this.errorClass = errorClass == null ? "AI_CLIENT_FAILURE" : errorClass;
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }

    public String errorClass() {
        return errorClass;
    }
}
