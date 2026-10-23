package com.highlands.order.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for a client-visible business rule with a stable API error code.
 */
public class ApiDomainException extends RuntimeException {
    private final ApiErrorCode errorCode;
    private final HttpStatus status;

    public ApiDomainException(ApiErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
