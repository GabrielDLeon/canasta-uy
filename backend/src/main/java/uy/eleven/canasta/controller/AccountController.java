package uy.eleven.canasta.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import uy.eleven.canasta.dto.*;
import uy.eleven.canasta.model.ApiKey;
import uy.eleven.canasta.model.Client;
import uy.eleven.canasta.service.ApiKeyService;
import uy.eleven.canasta.service.ClientService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Operaciones para gestionar la cuenta del usuario y API keys")
public class AccountController {

    private final ClientService clientService;
    private final ApiKeyService apiKeyService;

    @Operation(
            summary = "Obtener perfil del usuario",
            description = "Obtiene la información del perfil del usuario autenticado")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Perfil obtenido exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        Client client = clientService.findByUsername(username);
        int totalKeys = apiKeyService.countActiveApiKeys(client.getClientId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        new ProfileResponse(
                                client.getClientId(),
                                client.getEmail(),
                                totalKeys,
                                client.getCreatedAt())));
    }

    @Operation(
            summary = "Listar API keys",
            description = "Obtiene todas las API keys del usuario autenticado")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lista de API keys obtenida exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/api-keys")
    public ResponseEntity<ApiResponse<ApiKeyListResponse>> listApiKeys(
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        Client client = clientService.findByUsername(email);
        List<ApiKey> keys = apiKeyService.getClientApiKeys(client.getClientId());

        List<ApiKeyResponse> responses =
                keys.stream()
                        .map(
                                key -> {
                                    String keyPrefix = apiKeyService.maskApiKey(key.getKeyValue());
                                    return new ApiKeyResponse(
                                            key.getId(),
                                            key.getName(),
                                            null,
                                            keyPrefix,
                                            key.isActive(),
                                            key.getCreatedAt());
                                })
                        .toList();

        return ResponseEntity.ok(ApiResponse.success(new ApiKeyListResponse(responses)));
    }

    @Operation(
            summary = "Crear nueva API key",
            description = "Crea una nueva API key para el usuario autenticado. La clave solo se muestra una vez.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "API key creada exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/api-keys")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> createApiKey(
            @Valid @RequestBody CreateApiKeyRequest request, Authentication authentication) {

        String email = (String) authentication.getPrincipal();
        Client client = clientService.findByUsername(email);
        ApiKey key = apiKeyService.createApiKey(client.getClientId(), request.name());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                new ApiKeyResponse(
                                        key.getId(),
                                        key.getName(),
                                        key.getKeyValue(),
                                        null,
                                        key.isActive(),
                                        key.getCreatedAt()),
                                "API key created successfully. Save this key - it won't be shown"
                                        + " again."));
    }

    @Operation(
            summary = "Revocar API key",
            description = "Revoca (elimina) una API key específica del usuario autenticado")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "API key revocada exitosamente",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "API key no encontrada",
                content = @Content)
    })
    @DeleteMapping("/api-keys/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> revokeApiKey(
            @Parameter(description = "ID de la API key", example = "1", required = true)
            @PathVariable Long id, Authentication authentication) {

        String email = (String) authentication.getPrincipal();
        Client client = clientService.findByUsername(email);
        apiKeyService.revokeApiKey(client.getClientId(), id);

        return ResponseEntity.ok(
                ApiResponse.success(new MessageResponse("API key revoked successfully")));
    }
}
