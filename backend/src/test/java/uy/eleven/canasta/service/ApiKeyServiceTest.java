package uy.eleven.canasta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import uy.eleven.canasta.exception.ClientNotFoundException;
import uy.eleven.canasta.exception.InvalidApiKeyException;
import uy.eleven.canasta.model.ApiKey;
import uy.eleven.canasta.model.Client;
import uy.eleven.canasta.repository.ApiKeyRepository;
import uy.eleven.canasta.repository.ClientRepository;
import uy.eleven.canasta.security.util.ApiKeyGenerator;
import uy.eleven.canasta.testsupport.TestDataFactory;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock private ApiKeyRepository apiKeyRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ApiKeyGenerator apiKeyGenerator;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks private ApiKeyService apiKeyService;

    @Test
    void createApiKeyThrowsWhenClientMissing() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                ClientNotFoundException.class, () -> apiKeyService.createApiKey(1L, "default"));
    }

    @Test
    void createApiKeyPersistsGeneratedValue() {
        Client client = TestDataFactory.client(1L, "user@test.com");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(apiKeyGenerator.generate()).thenReturn("sk_live_123");
        when(apiKeyRepository.save(any(ApiKey.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, ApiKey.class));

        ApiKey apiKey = apiKeyService.createApiKey(1L, "my key");

        assertEquals("sk_live_123", apiKey.getKeyValue());
        assertTrue(apiKey.isActive());
    }

    @Test
    void validateApiKeyUsesCacheWhenPresent() {
        Client client = TestDataFactory.client(1L, "user@test.com");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("api_key:sk_live_cache")).thenReturn("1");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        Client result = apiKeyService.validateApiKey("sk_live_cache");

        assertEquals(1L, result.getClientId());
        verify(valueOperations)
                .set(eq("api_key:last_used:sk_live_cache"), any(String.class), any(Duration.class));
    }

    @Test
    void validateApiKeyCacheMissFetchesFromDatabaseAndCaches() {
        Client client = TestDataFactory.client(1L, "user@test.com");
        ApiKey apiKey = TestDataFactory.apiKey(1L, client, "sk_live_db", true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("api_key:sk_live_db")).thenReturn(null);
        when(apiKeyRepository.findByKeyValue("sk_live_db")).thenReturn(Optional.of(apiKey));

        Client result = apiKeyService.validateApiKey("sk_live_db");

        assertEquals(1L, result.getClientId());
        verify(valueOperations).set(eq("api_key:sk_live_db"), eq("1"), any(Duration.class));
    }

    @Test
    void validateApiKeyThrowsWhenInactive() {
        Client client = TestDataFactory.client(1L, "user@test.com");
        ApiKey apiKey = TestDataFactory.apiKey(1L, client, "sk_revoked", false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("api_key:sk_revoked")).thenReturn(null);
        when(apiKeyRepository.findByKeyValue("sk_revoked")).thenReturn(Optional.of(apiKey));

        assertThrows(
                InvalidApiKeyException.class, () -> apiKeyService.validateApiKey("sk_revoked"));
    }

    @Test
    void revokeApiKeyThrowsWhenWrongOwner() {
        Client owner = TestDataFactory.client(1L, "owner@test.com");
        Client other = TestDataFactory.client(2L, "other@test.com");
        ApiKey apiKey = TestDataFactory.apiKey(5L, other, "sk_live_5", true);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(apiKeyRepository.findById(5L)).thenReturn(Optional.of(apiKey));

        assertThrows(InvalidApiKeyException.class, () -> apiKeyService.revokeApiKey(1L, 5L));
    }

    @Test
    void revokeApiKeyDeactivatesAndDeletesCacheEntry() {
        Client owner = TestDataFactory.client(1L, "owner@test.com");
        ApiKey apiKey = TestDataFactory.apiKey(5L, owner, "sk_live_5", true);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(apiKeyRepository.findById(5L)).thenReturn(Optional.of(apiKey));

        apiKeyService.revokeApiKey(1L, 5L);

        assertTrue(!apiKey.isActive());
        verify(apiKeyRepository).save(apiKey);
        verify(redisTemplate).delete("api_key:sk_live_5");
    }

    @Test
    void maskApiKeyHandlesShortAndLongValues() {
        assertEquals("abc", apiKeyService.maskApiKey("abc"));
        assertEquals("0123456789...cdef", apiKeyService.maskApiKey("0123456789abcdefghijcdef"));
    }

    @Test
    void getClientApiKeyListItemsMasksValues() {
        Client client = TestDataFactory.client(1L, "user@test.com");
        ApiKey key = TestDataFactory.apiKey(1L, client, "0123456789abcdefghijcdef", true);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(apiKeyRepository.findByClient(client)).thenReturn(List.of(key));

        var items = apiKeyService.getClientApiKeyListItems(1L);

        assertEquals(1, items.size());
        assertTrue(items.get(0).keyPrefix().startsWith("0123456789..."));
    }
}
