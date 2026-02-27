package uy.eleven.canasta.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de inicio de sesión exitoso.
 * Incluye tokens de acceso y refresh.
 */
@Schema(description = "Respuesta de login con tokens JWT")
public record LoginResponse(
        @Schema(description = "Token de acceso JWT (válido por 15 minutos)", 
                example = "eyJhbGciOiJIUzI1NiIs...")
        String accessToken, 
        
        @Schema(description = "Token de refresh (válido por 7 días)", 
                example = "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...")
        String refreshToken, 
        
        @Schema(description = "Tiempo de expiración del access token en segundos", 
                example = "900")
        Long expiresIn) {}
