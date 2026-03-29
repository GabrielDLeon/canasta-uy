package uy.eleven.canasta.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uy.eleven.canasta.repository.ClientRepository;

@Configuration
public class DemoUserSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DemoUserSeeder(
            ClientRepository clientRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Value("${canasta.demo-user.email}")
    private String demoUserEmail;

    @Value("${canasta.demo-user.password}")
    private String demoUserPassword;

    @Bean
    @ConditionalOnProperty(prefix = "canasta.demo-user", name = "enabled", havingValue = "true")
    public ApplicationRunner seedDemoUser() {
        return args -> {
            if (clientRepository.existsByEmail(demoUserEmail)) {
                log.info("Demo user already exists: {}", demoUserEmail);
                return;
            }

            if (clientRepository.existsByUsername(demoUserEmail)) {
                log.warn(
                        "Username '{}' exists but email does not. Skipping demo user creation.",
                        demoUserEmail);
                return;
            }

            jdbcTemplate.update(
                    "INSERT INTO clients (username, email, password, is_active) VALUES (?, ?, ?, TRUE)",
                    demoUserEmail,
                    demoUserEmail,
                    passwordEncoder.encode(demoUserPassword));

            log.info("Demo user created: {}", demoUserEmail);
        };
    }
}
