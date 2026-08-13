package com.jobmatch;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class MatchIntegrationTest {

    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PGVECTOR_IMAGE);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> "integration-test-secret-at-least-32-bytes-long!!");
        registry.add("jobs.seed-on-startup", () -> "false"); // fake embeddings; control data here
    }

    @Autowired
    private TestRestTemplate rest;

    @BeforeEach
    void useJdkHttpClient() {
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void matchText_returnsJobsRankedByScoreRespectingLimit() {
        String token = registerAndGetToken("match-user@example.com");
        createJob(token, "Senior Backend Engineer", "Java, Spring Boot, AWS, Kubernetes");
        createJob(token, "Data Engineer", "Snowflake, dbt, Python, ELT pipelines");
        createJob(token, "Frontend Engineer", "React, TypeScript, CSS, accessibility");

        ResponseEntity<JsonNode> match = rest.exchange(
                "/api/matches", HttpMethod.POST,
                new HttpEntity<>(Map.of("text", "experienced java spring boot backend developer", "limit", 2),
                        bearer(token)),
                JsonNode.class);

        assertThat(match.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = match.getBody();
        assertThat(body.size()).isEqualTo(2); // limit respected
        // results are ordered best-first (non-increasing score)
        assertThat(body.get(0).get("score").asDouble())
                .isGreaterThanOrEqualTo(body.get(1).get("score").asDouble());
        assertThat(body.get(0).has("title")).isTrue();
    }

    @Test
    void matchText_withoutToken_returns401() {
        ResponseEntity<JsonNode> match = rest.postForEntity(
                "/api/matches", Map.of("text", "anything"), JsonNode.class);
        assertThat(match.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void matchResume_unknownId_returns404() {
        String token = registerAndGetToken("match-user2@example.com");
        ResponseEntity<JsonNode> match = rest.exchange(
                "/api/resumes/" + UUID.randomUUID() + "/matches", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(match.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void createJob(String token, String title, String description) {
        ResponseEntity<JsonNode> create = rest.exchange(
                "/api/jobs", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", title, "description", description), bearer(token)),
                JsonNode.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private String registerAndGetToken(String email) {
        ResponseEntity<JsonNode> reg = rest.postForEntity(
                "/api/auth/register",
                Map.of("email", email, "password", "supersecret", "fullName", "Match User"),
                JsonNode.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return reg.getBody().get("token").asText();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
