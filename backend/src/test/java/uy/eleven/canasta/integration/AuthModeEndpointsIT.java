package uy.eleven.canasta.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import uy.eleven.canasta.model.ApiKey;
import uy.eleven.canasta.model.Client;
import uy.eleven.canasta.repository.ApiKeyRepository;
import uy.eleven.canasta.repository.ClientRepository;
import uy.eleven.canasta.service.JwtService;
import uy.eleven.canasta.testsupport.IntegrationContainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthModeEndpointsIT extends IntegrationContainers {

    @Autowired private MockMvc mockMvc;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ApiKeyRepository apiKeyRepository;
    @Autowired private JwtService jwtService;

    private String jwtToken;
    private String apiKeyValue;

    @BeforeEach
    void setup() {
        apiKeyRepository.deleteAll();
        clientRepository.deleteAll();

        Client client =
                Client.builder()
                        .username("auth@test.com")
                        .email("auth@test.com")
                        .password("encoded")
                        .isActive(true)
                        .build();
        client = clientRepository.save(client);

        ApiKey apiKey =
                ApiKey.builder()
                        .client(client)
                        .keyValue(
                                "sk_live_abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ")
                        .name("integration")
                        .isActive(true)
                        .build();
        apiKey = apiKeyRepository.save(apiKey);

        jwtToken = jwtService.generateAccessToken(client.getClientId(), client.getUsername());
        apiKeyValue = apiKey.getKeyValue();
    }

    @Test
    void dataEndpointsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/products")).andExpect(status().isUnauthorized());
    }

    @Test
    void dataEndpointsAcceptJwt() throws Exception {
        mockMvc.perform(
                        get("/api/v1/products")
                                .cookie(new Cookie("canasta_access_token", jwtToken)))
                .andExpect(status().isOk());
    }

    @Test
    void dataEndpointsAcceptApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/products").header("Api-Key", apiKeyValue))
                .andExpect(status().isOk());
    }

    @Test
    void accountEndpointsAcceptJwt() throws Exception {
        mockMvc.perform(
                        get("/api/v1/account/api-keys")
                                .cookie(new Cookie("canasta_access_token", jwtToken)))
                .andExpect(status().isOk());
    }

    @Test
    void accountEndpointsRejectApiKeyOnly() throws Exception {
        mockMvc.perform(get("/api/v1/account/api-keys").header("Api-Key", apiKeyValue))
                .andExpect(status().isUnauthorized());
    }
}
