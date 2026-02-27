package uy.eleven.canasta.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para solicitud de inicio de sesión.
 */
@Schema(description = "Solicitud de login")
public record LoginRequest(
        @Schema(description = "Email del usuario", example = "usuario@ejemplo.com", required = true)
        @NotBlank @Email String email, 
        
        @Schema(description = "Contraseña del usuario (mínimo 8 caracteres)", example = "password123", required = true)
        @NotBlank @Size(min = 8) String password) {}
