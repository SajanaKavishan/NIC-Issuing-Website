package com.project.nic.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        List<ApiFieldError> errors,
        LocalDateTime timestamp,
        String path
) {
    public static <T> ApiResponse<T> success(T data, String path) {
        return new ApiResponse<>(true, "Success", data, List.of(), LocalDateTime.now(), path);
    }

    public static <T> ApiResponse<T> success(T data, String message, String path) {
        return new ApiResponse<>(true, message, data, List.of(), LocalDateTime.now(), path);
    }

    public static ApiResponse<Object> error(String message, List<ApiFieldError> errors, String path) {
        return new ApiResponse<>(false, message, null, errors == null ? List.of() : errors, LocalDateTime.now(), path);
    }

    public record ApiFieldError(String field, String message) {
    }
}
