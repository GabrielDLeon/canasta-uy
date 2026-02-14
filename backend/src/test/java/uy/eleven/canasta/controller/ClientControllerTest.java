package uy.eleven.canasta.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import uy.eleven.canasta.model.Client;
import uy.eleven.canasta.service.ClientService;

import java.time.LocalDateTime;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ClientService clientService;

    @Test
    void shouldReturnOkWhenRegisteringClient() throws Exception {
        String username = "testuser", password = "testpassword123", email = "testuser@gmail.com";
        when(clientService.register(anyString(), anyString(), anyString()))
                .thenReturn(
                        Client.builder()
                                .clientId(1L)
                                .username(username)
                                .password(password)
                                .email(email)
                                .createdAt(LocalDateTime.now())
                                .build());

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "username": "testuser",
                                            "password": "testpassword123",
                                            "email": "testuser@gmail.com"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(1L))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.createdAt").exists());
    }
}
