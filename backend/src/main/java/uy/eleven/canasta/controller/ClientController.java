package uy.eleven.canasta.controller;

import jakarta.validation.Valid;

import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import uy.eleven.canasta.dto.RegisterRequest;
import uy.eleven.canasta.dto.RegisterResponse;
import uy.eleven.canasta.model.Client;
import uy.eleven.canasta.service.ClientService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        Client result =
                clientService.register(request.username(), request.password(), request.email());

        URI location =
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(result.getClientId())
                        .toUri();

        return ResponseEntity.created(location).body(RegisterResponse.fromEntity(result));
    }
}
