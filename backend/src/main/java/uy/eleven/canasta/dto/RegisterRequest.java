package uy.eleven.canasta.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
                @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
                @Pattern(
                        regexp = "^[a-zA-Z0-9_]+$",
                        message = "Username can only contain letters, numbers, and underscores")
                String username,
        @NotBlank(message = "Password is required")
                @Size(min = 8, max = 255, message = "Password must be at least 8 characters")
                String password,
        @NotBlank(message = "Email is required") @Email(message = "Email should be valid")
                String email) {}
