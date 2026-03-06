package uy.eleven.canasta.dto;

import java.time.LocalDateTime;

public record ApiKeyListItem(
        Long id, String name, String keyPrefix, boolean isActive, LocalDateTime createdAt) {}
