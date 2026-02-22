package uy.eleven.canasta.dto;

import java.time.LocalDateTime;

public record ApiKeyResponse(
        Long id,
        String name,
        String keyValue,
        String keyPrefix,
        boolean isActive,
        LocalDateTime createdAt) {
    public ApiKeyResponse {
        if (keyValue != null && keyValue.length() > 20) {
            keyPrefix =
                    keyValue.substring(0, 10) + "..." + keyValue.substring(keyValue.length() - 6);
        }
    }
}
