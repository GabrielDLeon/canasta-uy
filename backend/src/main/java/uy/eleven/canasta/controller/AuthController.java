package uy.eleven.canasta.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import uy.eleven.canasta.config.JwtProperties;
import uy.eleven.canasta.dto.*;
import uy.eleven.canasta.exception.InvalidTokenException;
import uy.eleven.canasta.model.Client;
import uy.eleven.canasta.model.RefreshToken;
import uy.eleven.canasta.service.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Operaciones de autenticación y gestión de sesiones")
public class AuthController {

    private final ClientService clientService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;

    private static final String ACCESS_COOKIE_NAME = "canasta_access_token";
    private static final String REFRESH_COOKIE_NAME = "canasta_refresh_token";

    @Operation(
            summary = "Registrar nuevo usuario",
            description = "Crea una nueva cuenta de usuario con email y contraseña")
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Usuario registrado exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Datos de registro inválidos",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Email o username ya existe",
                content = @Content)
    })
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

    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica un usuario y devuelve tokens de acceso y refresh")
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Login exitoso",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Credenciales inválidas",
                content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        Client client = clientService.login(request.email(), request.password());

        String accessToken =
                jwtService.generateAccessToken(client.getClientId(), client.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(client);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        buildCookie(
                                ACCESS_COOKIE_NAME,
                                accessToken,
                                jwtProperties.getAccessTokenTtl() / 1000,
                                httpRequest.isSecure()))
                .header(
                        HttpHeaders.SET_COOKIE,
                        buildCookie(
                                REFRESH_COOKIE_NAME,
                                refreshToken.getTokenValue(),
                                jwtProperties.getRefreshTokenTtl() / 1000,
                                httpRequest.isSecure()))
                .body(
                        ApiResponse.success(
                        new LoginResponse(accessToken, refreshToken.getTokenValue(), 900L),
                        "Login successful"));
    }

    @Operation(
            summary = "Refrescar token",
            description = "Obtiene un nuevo access token usando un refresh token válido")
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Token refrescado exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Refresh token inválido o expirado",
                content = @Content)
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @RequestBody(required = false) RefreshRequest request,
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshCookie,
            HttpServletRequest httpRequest) {
        String refreshToken =
                request != null && request.refreshToken() != null && !request.refreshToken().isBlank()
                        ? request.refreshToken()
                        : refreshCookie;
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token required");
        }

        RefreshTokenService.TokenPair pair =
                refreshTokenService.rotateRefreshToken(refreshToken);

        String newAccessToken =
                jwtService.generateAccessToken(
                        pair.client().getClientId(), pair.client().getUsername());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        buildCookie(
                                ACCESS_COOKIE_NAME,
                                newAccessToken,
                                jwtProperties.getAccessTokenTtl() / 1000,
                                httpRequest.isSecure()))
                .header(
                        HttpHeaders.SET_COOKIE,
                        buildCookie(
                                REFRESH_COOKIE_NAME,
                                pair.refreshToken(),
                                jwtProperties.getRefreshTokenTtl() / 1000,
                                httpRequest.isSecure()))
                .body(ApiResponse.success(new RefreshResponse(newAccessToken, pair.refreshToken())));
    }

    @Operation(
            summary = "Cerrar sesión",
            description = "Revoca todos los tokens de refresh del usuario actual")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Sesión cerrada exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(
            Authentication authentication, HttpServletRequest httpRequest) {
        String email = (String) authentication.getPrincipal();
        Client client =
                clientService.findByUsername(email);
        refreshTokenService.revokeAllUserRefreshTokens(client.getClientId());
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearCookie(ACCESS_COOKIE_NAME, httpRequest.isSecure()))
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearCookie(REFRESH_COOKIE_NAME, httpRequest.isSecure()))
                .body(ApiResponse.success(new MessageResponse("Logged out successfully")));
    }

    private static String buildCookie(String name, String value, long maxAgeSeconds, boolean secure) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build()
                .toString();
    }

    private static String clearCookie(String name, boolean secure) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build()
                .toString();
    }
}
