package uy.eleven.canasta.service;

import lombok.AllArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uy.eleven.canasta.exception.ClientNotFoundException;
import uy.eleven.canasta.exception.DuplicateEmailException;
import uy.eleven.canasta.exception.DuplicateUsernameException;
import uy.eleven.canasta.exception.InvalidCredentialsException;
import uy.eleven.canasta.model.Client;
import uy.eleven.canasta.repository.ClientRepository;

@Service
@AllArgsConstructor
public class ClientService {
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Client register(String username, String password, String email) {
        if (clientRepository.existsByUsername(username))
            throw new DuplicateUsernameException(username);

        if (clientRepository.existsByEmail(email)) throw new DuplicateEmailException(email);

        Client client =
                Client.builder()
                        .username(username)
                        .email(email)
                        .password(passwordEncoder.encode(password))
                        .build();

        return clientRepository.save(client);
    }

    @Transactional(readOnly = true)
    public Client login(String email, String password) {
        Client client =
                clientRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new ClientNotFoundException("email", email));

        if (!passwordEncoder.matches(password, client.getPassword()))
            throw new InvalidCredentialsException();

        return client;
    }

    @Transactional(readOnly = true)
    public Client findById(Long clientId) {
        return clientRepository
                .findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));
    }

    @Transactional(readOnly = true)
    public Client findByUsername(String username) {
        return clientRepository
                .findByUsername(username)
                .orElseThrow(() -> new ClientNotFoundException("username", username));
    }

    @Transactional
    public Client updateProfile(Long clientId, String newEmail) {
        Client client =
                clientRepository
                        .findById(clientId)
                        .orElseThrow(() -> new ClientNotFoundException(clientId));

        if (newEmail != null && !newEmail.equals(client.getEmail())) {
            if (clientRepository.existsByEmail(newEmail)) {
                throw new DuplicateEmailException(newEmail);
            }
            client.setEmail(newEmail);
        }

        return clientRepository.save(client);
    }
}
