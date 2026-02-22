package uy.eleven.canasta.dto;

public record LoginResponse(String accessToken, String refreshToken, Long expiresIn) {}
