package uy.eleven.canasta.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
        String error,
        String message,
        String path,
        LocalDateTime timestamp) {

    public ErrorResponse(String error, String message) {
        this(error, message, null, LocalDateTime.now());
    }

    public ErrorResponse(String error, String message, String path) {
        this(error, message, path, LocalDateTime.now());
    }
}
