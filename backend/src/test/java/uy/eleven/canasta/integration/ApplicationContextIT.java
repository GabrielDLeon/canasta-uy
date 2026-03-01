package uy.eleven.canasta.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import uy.eleven.canasta.testsupport.IntegrationContainers;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextIT extends IntegrationContainers {

    @Test
    void contextLoads() {}
}
