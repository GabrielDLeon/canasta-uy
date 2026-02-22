package uy.eleven.canasta.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        T data,
        boolean success,
        String message,
        LocalDateTime timestamp) {

    public ApiResponse(T data) {
        this(data, true, null, LocalDateTime.now());
    }

    public ApiResponse(T data, String message) {
        this(data, true, message, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, true, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(data, true, message, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(null, false, message, LocalDateTime.now());
    }
}
