package com.opspulse.identity.api;

import com.opspulse.identity.application.DuplicateEmailException;
import com.opspulse.identity.application.IdentityAuthenticationException;
import com.opspulse.identity.application.IdentityAuthorizationException;
import com.opspulse.shared.error.ApiErrorResponse;
import com.opspulse.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class IdentityExceptionHandler {

    @ExceptionHandler(IdentityAuthenticationException.class)
    ResponseEntity<ApiErrorResponse> handleAuthenticationFailure() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(ErrorCode.UNAUTHORIZED, "Authentication failed"));
    }

    @ExceptionHandler(IdentityAuthorizationException.class)
    ResponseEntity<ApiErrorResponse> handleAuthorizationFailure() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(ErrorCode.FORBIDDEN, "Access is denied"));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ApiErrorResponse> handleDuplicateEmail() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        ErrorCode.IDENTITY_CONFLICT, "A user with this email already exists"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidIdentityInput(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(ErrorCode.VALIDATION_FAILED, exception.getMessage()));
    }
}
