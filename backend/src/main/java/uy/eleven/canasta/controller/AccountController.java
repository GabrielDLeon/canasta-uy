package uy.eleven.canasta.controller;

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
public class AccountController {

    private final ClientService clientService;
    private final ApiKeyService apiKeyService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(Authentication authentication) {
        Client client = (Client) authentication.getPrincipal();
        int totalKeys = apiKeyService.countActiveApiKeys(client.getClientId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        new ProfileResponse(
                                client.getClientId(),
                                client.getEmail(),
                                totalKeys,
                                client.getCreatedAt())));
    }

    @GetMapping("/api-keys")
    public ResponseEntity<ApiResponse<ApiKeyListResponse>> listApiKeys(
            Authentication authentication) {
        Client client = (Client) authentication.getPrincipal();
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

    @PostMapping("/api-keys")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> createApiKey(
            @Valid @RequestBody CreateApiKeyRequest request, Authentication authentication) {

        Client client = (Client) authentication.getPrincipal();
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

    @DeleteMapping("/api-keys/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> revokeApiKey(
            @PathVariable Long id, Authentication authentication) {

        Client client = (Client) authentication.getPrincipal();
        apiKeyService.revokeApiKey(client.getClientId(), id);

        return ResponseEntity.ok(
                ApiResponse.success(new MessageResponse("API key revoked successfully")));
    }
}
