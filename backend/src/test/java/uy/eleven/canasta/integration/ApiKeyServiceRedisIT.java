package uy.eleven.canasta.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import uy.eleven.canasta.model.ApiKey;
import uy.eleven.canasta.model.Client;
import uy.eleven.canasta.repository.ApiKeyRepository;
import uy.eleven.canasta.repository.ClientRepository;
import uy.eleven.canasta.service.ApiKeyService;
import uy.eleven.canasta.testsupport.IntegrationContainers;

@SpringBootTest
@ActiveProfiles("test")
class ApiKeyServiceRedisIT extends IntegrationContainers {

    @Autowired private ApiKeyService apiKeyService;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ApiKeyRepository apiKeyRepository;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void clean() {
        apiKeyRepository.deleteAll();
        clientRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void validateAndRevokeApiKeyAffectRedisCache() {
        Client client =
                Client.builder()
                        .username("user@test.com")
                        .email("user@test.com")
                        .password("encoded")
                        .isActive(true)
                        .build();
        client = clientRepository.save(client);

        ApiKey apiKey =
                ApiKey.builder()
                        .client(client)
                        .keyValue(
                                "sk_live_012345678901234567890123456789012345678901234567890123456789")
                        .name("integration")
                        .isActive(true)
                        .build();
        apiKey = apiKeyRepository.save(apiKey);

        apiKeyService.validateApiKey(apiKey.getKeyValue());
        assertEquals(
                String.valueOf(client.getClientId()),
                redisTemplate.opsForValue().get("api_key:" + apiKey.getKeyValue()));

        apiKeyService.revokeApiKey(client.getClientId(), apiKey.getId());
        assertNull(redisTemplate.opsForValue().get("api_key:" + apiKey.getKeyValue()));
    }
}
