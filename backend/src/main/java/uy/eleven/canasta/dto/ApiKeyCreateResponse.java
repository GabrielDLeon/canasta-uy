package uy.eleven.canasta.dto;

import java.time.LocalDateTime;

public record ApiKeyCreateResponse(
        String name,
        String keyValue,
        String keyPrefix,
        boolean isActive,
        LocalDateTime createdAt) {}
