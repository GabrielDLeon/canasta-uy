package uy.eleven.canasta.dto;

import uy.eleven.canasta.model.Client;

import java.time.LocalDateTime;

public record RegisterResponse(
        Long clientId, String username, String email, LocalDateTime createdAt) {

    public static RegisterResponse fromEntity(Client client) {
        return new RegisterResponse(
                client.getClientId(),
                client.getUsername(),
                client.getEmail(),
                client.getCreatedAt());
    }
}
