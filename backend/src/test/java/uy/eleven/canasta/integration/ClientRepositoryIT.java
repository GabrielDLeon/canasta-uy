package uy.eleven.canasta.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import uy.eleven.canasta.model.Client;
import uy.eleven.canasta.repository.ClientRepository;
import uy.eleven.canasta.testsupport.IntegrationContainers;

@SpringBootTest
@ActiveProfiles("test")
class ClientRepositoryIT extends IntegrationContainers {

    @Autowired private ClientRepository clientRepository;

    @BeforeEach
    void clean() {
        clientRepository.deleteAll();
    }

    @Test
    void findersAndExistsWork() {
        Client client =
                Client.builder()
                        .username("user@test.com")
                        .email("user@test.com")
                        .password("encoded")
                        .isActive(true)
                        .build();
        clientRepository.save(client);

        assertTrue(clientRepository.findByEmail("user@test.com").isPresent());
        assertTrue(clientRepository.findByUsername("user@test.com").isPresent());
        assertTrue(clientRepository.existsByEmail("user@test.com"));
    }

    @Test
    void duplicateEmailViolatesUniqueConstraint() {
        Client first =
                Client.builder()
                        .username("one@test.com")
                        .email("dup@test.com")
                        .password("encoded")
                        .isActive(true)
                        .build();
        Client second =
                Client.builder()
                        .username("two@test.com")
                        .email("dup@test.com")
                        .password("encoded")
                        .isActive(true)
                        .build();
        clientRepository.saveAndFlush(first);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> {
                    clientRepository.saveAndFlush(second);
                });
        assertEquals(1, clientRepository.count());
    }
}
