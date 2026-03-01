package uy.eleven.canasta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import uy.eleven.canasta.exception.ClientNotFoundException;
import uy.eleven.canasta.exception.DuplicateEmailException;
import uy.eleven.canasta.exception.InvalidCredentialsException;
import uy.eleven.canasta.model.Client;
import uy.eleven.canasta.repository.ClientRepository;
import uy.eleven.canasta.testsupport.TestDataFactory;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock private ClientRepository clientRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private ClientService clientService;

    @Test
    void registerSavesClientWhenEmailIsAvailable() {
        when(clientRepository.existsByEmail("user@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(clientRepository.save(any(Client.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Client.class));

        Client saved = clientService.register("user@test.com", "password123");

        assertEquals("user@test.com", saved.getEmail());
        assertEquals("user@test.com", saved.getUsername());
        assertEquals("encoded", saved.getPassword());
    }

    @Test
    void registerThrowsOnDuplicateEmail() {
        when(clientRepository.existsByEmail("user@test.com")).thenReturn(true);

        assertThrows(
                DuplicateEmailException.class,
                () -> clientService.register("user@test.com", "password123"));
    }

    @Test
    void loginReturnsClientWhenPasswordMatches() {
        Client client = TestDataFactory.client(1L, "user@test.com");
        client.setPassword("encoded");
        when(clientRepository.findByEmail("user@test.com")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);

        Client result = clientService.login("user@test.com", "password123");

        assertEquals(client.getClientId(), result.getClientId());
    }

    @Test
    void loginThrowsWhenClientIsMissing() {
        when(clientRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(
                ClientNotFoundException.class,
                () -> clientService.login("missing@test.com", "pwd"));
    }

    @Test
    void loginThrowsWhenPasswordDoesNotMatch() {
        Client client = TestDataFactory.client(1L, "user@test.com");
        client.setPassword("encoded");
        when(clientRepository.findByEmail("user@test.com")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> clientService.login("user@test.com", "wrong"));
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ClientNotFoundException.class, () -> clientService.findById(99L));
    }

    @Test
    void findByUsernameThrowsWhenNotFound() {
        when(clientRepository.findByUsername("missing@test.com")).thenReturn(Optional.empty());
        assertThrows(
                ClientNotFoundException.class,
                () -> clientService.findByUsername("missing@test.com"));
    }

    @Test
    void updateProfileUpdatesEmailWhenValid() {
        Client client = TestDataFactory.client(1L, "old@test.com");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(clientRepository.save(client)).thenReturn(client);

        Client updated = clientService.updateProfile(1L, "new@test.com");

        assertEquals("new@test.com", updated.getEmail());
        verify(clientRepository).save(client);
    }

    @Test
    void updateProfileThrowsWhenEmailAlreadyExists() {
        Client client = TestDataFactory.client(1L, "old@test.com");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientRepository.existsByEmail("new@test.com")).thenReturn(true);

        assertThrows(
                DuplicateEmailException.class,
                () -> clientService.updateProfile(1L, "new@test.com"));
    }
}
