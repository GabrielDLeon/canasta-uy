package uy.eleven.canasta.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import uy.eleven.canasta.dto.*;
import uy.eleven.canasta.model.Client;
import uy.eleven.canasta.model.RefreshToken;
import uy.eleven.canasta.service.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final ClientService clientService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        Client client = clientService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                RegisterResponse.fromEntity(client),
                                "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        Client client = clientService.login(request.email(), request.password());

        String accessToken =
                jwtService.generateAccessToken(client.getClientId(), client.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(client);

        return ResponseEntity.ok(
                ApiResponse.success(
                        new LoginResponse(accessToken, refreshToken.getTokenValue(), 900L),
                        "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {
        RefreshTokenService.TokenPair pair =
                refreshTokenService.rotateRefreshToken(request.refreshToken());

        String newAccessToken =
                jwtService.generateAccessToken(
                        pair.client().getClientId(), pair.client().getUsername());

        return ResponseEntity.ok(
                ApiResponse.success(new RefreshResponse(newAccessToken, pair.refreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(Authentication authentication) {
        Client client = (Client) authentication.getPrincipal();
        refreshTokenService.revokeAllUserRefreshTokens(client.getClientId());
        return ResponseEntity.ok(
                ApiResponse.success(new MessageResponse("Logged out successfully")));
    }
}
