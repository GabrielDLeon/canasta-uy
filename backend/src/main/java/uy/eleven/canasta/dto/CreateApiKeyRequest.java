package uy.eleven.canasta.dto;

import jakarta.validation.constraints.Size;

public record CreateApiKeyRequest(
        @Size(max = 100, message = "Name must be less than 100 characters") String name) {}
