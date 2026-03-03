package uy.eleven.canasta.security.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.jsonwebtoken.Claims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import uy.eleven.canasta.dto.ApiResponse;
import uy.eleven.canasta.model.Client;
import uy.eleven.canasta.service.ClientService;
import uy.eleven.canasta.service.JwtService;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ClientService clientService;

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_COOKIE_NAME = "canasta_access_token";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = extractJwtToken(request);
        String path = request.getRequestURI();

        log.debug("JwtAuthFilter processing request to: {}", path);
        log.debug("JWT present in request: {}", jwt != null);

        if (jwt == null || jwt.isBlank()) {
            log.warn("JWT required but not provided for path: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            ApiResponse<Void> errorResponse =
                    ApiResponse.error("JWT token required");

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(
                    com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            response.getWriter().write(mapper.writeValueAsString(errorResponse));
            return;
        }

        try {
            Claims claims = jwtService.validateAndGetClaims(jwt);
            String subject = claims.getSubject();
            Long clientId = Long.parseLong(subject);

            Client client = clientService.findById(clientId);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            client.getEmail(),
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            ApiResponse<Void> errorResponse =
                    ApiResponse.error("Invalid or expired token");

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(
                    com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            response.getWriter().write(mapper.writeValueAsString(errorResponse));
            return;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Apply filter to /account/* and /auth/logout (both require JWT)
        boolean shouldFilter =
                path.startsWith("/api/v1/account") || path.equals("/api/v1/auth/logout");
        return !shouldFilter;
    }

    private String extractJwtToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (ACCESS_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
