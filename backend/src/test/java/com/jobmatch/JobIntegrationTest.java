package com.jobmatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobmatch.job.JobVectorRepository;
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
class JobIntegrationTest {

    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PGVECTOR_IMAGE);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> "integration-test-secret-at-least-32-bytes-long!!");
        // embedding.provider defaults to "fake"; disable seeding so the test controls the data.
        registry.add("jobs.seed-on-startup", () -> "false");
    }

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private JobVectorRepository jobVectorRepository;

    @BeforeEach
    void useJdkHttpClient() {
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void createListGet_storesEmbedding() {
        String token = registerAndGetToken("job-user@example.com");

        Map<String, String> job = Map.of(
                "title", "Senior Backend Engineer",
                "company", "flexEngage",
                "location", "Remote",
                "description", "Java, Spring Boot, AWS, Kubernetes, Postgres");

        ResponseEntity<JsonNode> create = rest.exchange(
                "/api/jobs", HttpMethod.POST, new HttpEntity<>(job, bearer(token)), JsonNode.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String id = create.getBody().get("id").asText();
        assertThat(create.getBody().get("source").asText()).isEqualTo("manual");

        // the embedding was actually stored as a 1536-dim pgvector
        assertThat(jobVectorRepository.embeddingDimension(UUID.fromString(id))).isEqualTo(1536);

        ResponseEntity<JsonNode> list = rest.exchange(
                "/api/jobs", HttpMethod.GET, new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody().size()).isEqualTo(1);

        ResponseEntity<JsonNode> get = rest.exchange(
                "/api/jobs/" + id, HttpMethod.GET, new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get.getBody().get("title").asText()).isEqualTo("Senior Backend Engineer");
    }

    @Test
    void get_unknownId_returns404() {
        String token = registerAndGetToken("job-user2@example.com");
        ResponseEntity<JsonNode> get = rest.exchange(
                "/api/jobs/" + UUID.randomUUID(), HttpMethod.GET, new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void create_withoutToken_returns401() {
        Map<String, String> job = Map.of("title", "x", "description", "y");
        ResponseEntity<JsonNode> create = rest.postForEntity("/api/jobs", job, JsonNode.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String registerAndGetToken(String email) {
        ResponseEntity<JsonNode> reg = rest.postForEntity(
                "/api/auth/register",
                Map.of("email", email, "password", "supersecret", "fullName", "Job User"),
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
