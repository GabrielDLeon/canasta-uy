package uy.eleven.canasta.dto;

import java.time.LocalDateTime;

public record ProfileResponse(
        Long clientId, String email, int totalKeys, LocalDateTime createdAt) {}
