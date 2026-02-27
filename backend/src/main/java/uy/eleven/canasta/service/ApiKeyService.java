package uy.eleven.canasta.service;

import lombok.AllArgsConstructor;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uy.eleven.canasta.dto.ApiKeyCreateResponse;
import uy.eleven.canasta.dto.ApiKeyListItem;
import uy.eleven.canasta.exception.ClientNotFoundException;
import uy.eleven.canasta.exception.InvalidApiKeyException;
import uy.eleven.canasta.model.ApiKey;
import uy.eleven.canasta.model.Client;
import uy.eleven.canasta.repository.ApiKeyRepository;
import uy.eleven.canasta.repository.ClientRepository;
import uy.eleven.canasta.security.util.ApiKeyGenerator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ClientRepository clientRepository;
    private final ApiKeyGenerator apiKeyGenerator;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String API_KEY_CACHE_PREFIX = "api_key:";
    private static final String LAST_USED_CACHE_PREFIX = "api_key:last_used:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    @Transactional
    public ApiKey createApiKey(Long clientId, String name) {
        Client client =
                clientRepository
                        .findById(clientId)
                        .orElseThrow(() -> new ClientNotFoundException(clientId));

        String keyValue = apiKeyGenerator.generate();

        ApiKey apiKey =
                ApiKey.builder()
                        .client(client)
                        .keyValue(keyValue)
                        .name(name)
                        .isActive(true)
                        .build();

        return apiKeyRepository.save(apiKey);
    }

    @Transactional(readOnly = true)
    public List<ApiKey> getClientApiKeys(Long clientId) {
        Client client =
                clientRepository
                        .findById(clientId)
                        .orElseThrow(() -> new ClientNotFoundException(clientId));

        return apiKeyRepository.findByClient(client);
    }

    @Transactional(readOnly = true)
    public int countActiveApiKeys(Long clientId) {
        return (int) getClientApiKeys(clientId).stream().filter(ApiKey::isActive).count();
    }

    @Transactional(readOnly = true)
    public Client validateApiKey(String keyValue) {
        String cacheKey = API_KEY_CACHE_PREFIX + keyValue;

        String cachedClientIdStr = (String) redisTemplate.opsForValue().get(cacheKey);
        if (cachedClientIdStr != null) {
            updateLastUsedInCache(keyValue);
            Long cachedClientId = Long.parseLong(cachedClientIdStr);
            return clientRepository
                    .findById(cachedClientId)
                    .orElseThrow(
                            () ->
                                    new InvalidApiKeyException(
                                            "Client not found for cached API key"));
        }

        ApiKey apiKey =
                apiKeyRepository
                        .findByKeyValue(keyValue)
                        .orElseThrow(() -> new InvalidApiKeyException("Invalid API key"));

        if (!apiKey.isActive()) {
            throw new InvalidApiKeyException("API key has been revoked");
        }

        Client client = apiKey.getClient();
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(client.getClientId()), CACHE_TTL);
        updateLastUsedInCache(keyValue);

        return client;
    }

    @Transactional
    public void revokeApiKey(Long clientId, Long keyId) {
        Client client =
                clientRepository
                        .findById(clientId)
                        .orElseThrow(() -> new ClientNotFoundException(clientId));

        ApiKey apiKey =
                apiKeyRepository
                        .findById(keyId)
                        .orElseThrow(() -> new InvalidApiKeyException("API key not found"));

        if (!apiKey.getClient().getClientId().equals(clientId)) {
            throw new InvalidApiKeyException("API key does not belong to this client");
        }

        apiKey.setActive(false);
        apiKey.setRevokedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);

        String cacheKey = API_KEY_CACHE_PREFIX + apiKey.getKeyValue();
        redisTemplate.delete(cacheKey);
    }

    private void updateLastUsedInCache(String keyValue) {
        String lastUsedKey = LAST_USED_CACHE_PREFIX + keyValue;
        redisTemplate.opsForValue().set(lastUsedKey, LocalDateTime.now().toString(), CACHE_TTL);
    }

    public String maskApiKey(String keyValue) {
        if (keyValue == null || keyValue.length() < 12) {
            return keyValue;
        }
        return keyValue.substring(0, 10) + "..." + keyValue.substring(keyValue.length() - 4);
    }

    public List<ApiKeyListItem> getClientApiKeyListItems(Long clientId) {
        Client client =
                clientRepository
                        .findById(clientId)
                        .orElseThrow(() -> new ClientNotFoundException(clientId));

        return apiKeyRepository.findByClient(client).stream()
                .map(
                        key ->
                                new ApiKeyListItem(
                                        key.getName(),
                                        maskApiKey(key.getKeyValue()),
                                        key.isActive(),
                                        key.getCreatedAt()))
                .toList();
    }

    public ApiKeyCreateResponse createApiKeyResponse(Long clientId, String name) {
        ApiKey apiKey = createApiKey(clientId, name);
        return new ApiKeyCreateResponse(
                apiKey.getName(),
                apiKey.getKeyValue(),
                null,
                apiKey.isActive(),
                apiKey.getCreatedAt());
    }
}
