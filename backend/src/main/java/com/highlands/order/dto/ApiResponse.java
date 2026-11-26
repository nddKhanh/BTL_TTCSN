package com.highlands.order.dto;

import java.time.Instant;

public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    String errorCode,
    Instant timestamp,
    String path
) {
    public static <T> ApiResponse<T> success(T data) {
        return success("Success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message, String path) {
        return new ApiResponse<>(false, message, null, errorCode, Instant.now(), path);
    }
}
