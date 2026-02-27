package uy.eleven.canasta.dto;

import java.time.LocalDateTime;

public record ApiKeyListItem(
        String name, String keyPrefix, boolean isActive, LocalDateTime createdAt) {}
