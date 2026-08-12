package com.jobmatch;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthIntegrationTest {

    // pgvector image is required because V1__init.sql runs `CREATE EXTENSION vector`.
    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PGVECTOR_IMAGE);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> "integration-test-secret-at-least-32-bytes-long!!");
    }

    @Autowired
    private TestRestTemplate rest;

    /**
     * TestRestTemplate defaults to JDK {@code HttpURLConnection}, which throws
     * {@code HttpRetryException("cannot retry due to server authentication")} when a 401 comes
     * back on a POST whose body was streamed — it tries to re-authenticate and cannot rewind the
     * body. That masks the perfectly valid 401 from the login endpoint. {@code java.net.http}
     * has no such behaviour, so use it instead.
     */
    @BeforeEach
    void useJdkHttpClient() {
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void registerThenLoginThenMe_happyPath() {
        String email = "alice@example.com";
        String password = "supersecret";

        // register -> 201 with token + user
        ResponseEntity<JsonNode> register = rest.postForEntity(
                "/api/auth/register",
                Map.of("email", email, "password", password, "fullName", "Alice Example"),
                JsonNode.class);
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(register.getBody()).isNotNull();
        assertThat(register.getBody().get("token").asText()).isNotBlank();
        assertThat(register.getBody().get("user").get("email").asText()).isEqualTo(email);

        // login -> 200 with token
        ResponseEntity<JsonNode> login = rest.postForEntity(
                "/api/auth/login",
                Map.of("email", email, "password", password),
                JsonNode.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = login.getBody().get("token").asText();
        assertThat(token).isNotBlank();

        // /me with token -> 200 with the user
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<JsonNode> me = rest.exchange(
                "/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().get("email").asText()).isEqualTo(email);
        assertThat(me.getBody().get("fullName").asText()).isEqualTo("Alice Example");
    }

    @Test
    void register_duplicateEmail_returns409() {
        Map<String, String> body = Map.of(
                "email", "dup@example.com", "password", "supersecret", "fullName", "Dup User");
        rest.postForEntity("/api/auth/register", body, JsonNode.class);

        ResponseEntity<JsonNode> second =
                rest.postForEntity("/api/auth/register", body, JsonNode.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody().get("status").asInt()).isEqualTo(409);
    }

    @Test
    void login_wrongPassword_returns401() {
        Map<String, String> register = Map.of(
                "email", "bob@example.com", "password", "correcthorse", "fullName", "Bob");
        rest.postForEntity("/api/auth/register", register, JsonNode.class);

        ResponseEntity<JsonNode> login = rest.postForEntity(
                "/api/auth/login",
                Map.of("email", "bob@example.com", "password", "wrongpassword"),
                JsonNode.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_withoutToken_returns401() {
        ResponseEntity<JsonNode> me = rest.getForEntity("/api/auth/me", JsonNode.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
