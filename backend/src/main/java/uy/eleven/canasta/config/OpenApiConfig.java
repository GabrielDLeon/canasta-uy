package uy.eleven.canasta.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de OpenAPI para documentación automática de la API.
 * Genera documentación code-first basada en anotaciones.
 */
@Configuration
public class OpenApiConfig {

    private static final String JWT_SCHEME = "bearer-jwt";
    private static final String API_KEY_SCHEME = "api-key";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers())
                .components(securityComponents())
                .addSecurityItem(securityRequirement());
    }

    private Info apiInfo() {
        return new Info()
                .title("CanastaUY API")
                .description("API REST para consulta de precios de productos en Uruguay. " +
                        "Proporciona acceso a datos históricos de precios, estadísticas por categoría, " +
                        "y análisis de tendencias de inflación.")
                .version("1.0.0")
                .contact(new Contact()
                        .name("CanastaUY Team")
                        .url("https://github.com/canasta-uy")
                        .email("contact@canasta.uy"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private List<Server> servers() {
        return List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("Servidor de desarrollo local"),
                new Server()
                        .url("https://api.canasta.uy")
                        .description("Servidor de producción"));
    }

    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes(JWT_SCHEME, jwtSecurityScheme())
                .addSecuritySchemes(API_KEY_SCHEME, apiKeySecurityScheme());
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Token JWT de autenticación. Obtén el token mediante /api/v1/auth/login");
    }

    private SecurityScheme apiKeySecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("API Key para autenticación de clientes. Genera una API key en tu cuenta.");
    }

    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement()
                .addList(JWT_SCHEME)
                .addList(API_KEY_SCHEME);
    }
}
