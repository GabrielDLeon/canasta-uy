package uy.eleven.canasta.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Password is required")
                @Size(min = 8, max = 255, message = "Password must be at least 8 characters")
                String password,
        @NotBlank(message = "Email is required") @Email(message = "Email should be valid")
                String email) {}
