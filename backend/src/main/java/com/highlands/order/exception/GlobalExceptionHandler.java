package com.highlands.order.exception;

import com.highlands.order.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiDomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(
            ApiDomainException ex,
            HttpServletRequest request
    ) {
        return error(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, ApiErrorCode.ROUTE_NOT_FOUND, "Không tìm thấy đường dẫn yêu cầu", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.UNAUTHORIZED, ApiErrorCode.AUTHENTICATION_FAILED, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, errorMsg, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, ApiErrorCode.MALFORMED_JSON, "Nội dung JSON không hợp lệ", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String message = "Tham số '" + ex.getName() + "' không đúng định dạng";
        return error(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_PARAMETER, message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRuleViolation(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Dữ liệu đã tồn tại hoặc đang được sử dụng", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error while handling {} {}", request.getMethod(), request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INTERNAL_ERROR, "Đã xảy ra lỗi hệ thống", request);
    }

    private ResponseEntity<ApiResponse<Void>> error(
            HttpStatus status,
            ApiErrorCode errorCode,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(errorCode.name(), message, request.getRequestURI()));
    }
}
