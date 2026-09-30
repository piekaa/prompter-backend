package com.prompter;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests. Boots the full Spring context against a
 * TestContainers PostgreSQL instance (Flyway migrations run on it).
 *
 * The container is a JVM-level shared static (started once, lives for the whole
 * surefire fork) instead of a @Testcontainers @Container: with the JUnit
 * extension the container is stopped when the FIRST test class that touches it
 * finishes, which breaks every subsequent test class that reuses the cached
 * Spring context (its DataSource still points at the dead container's port).
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("prompter")
            .withPassword("prompter");

    static {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
}
