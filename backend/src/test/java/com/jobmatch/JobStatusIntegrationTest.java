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
class JobStatusIntegrationTest {

    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PGVECTOR_IMAGE);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> "integration-test-secret-at-least-32-bytes-long!!");
        registry.add("jobs.seed-on-startup", () -> "false");
    }

    @Autowired
    private TestRestTemplate rest;

    @BeforeEach
    void useJdkHttpClient() {
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void save_thenApplied_thenClear() {
        String token = registerAndGetToken("status-user@example.com");
        String jobId = createJob(token);

        // save
        assertThat(setStatus(token, jobId, "saved").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(statusFor(token, jobId)).isEqualTo("saved");

        // upsert to applied
        assertThat(setStatus(token, jobId, "applied").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(statusFor(token, jobId)).isEqualTo("applied");

        // clear
        ResponseEntity<Void> del = rest.exchange(
                "/api/jobs/" + jobId + "/status", HttpMethod.DELETE, new HttpEntity<>(bearer(token)), Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(statusFor(token, jobId)).isNull();
    }

    @Test
    void setStatus_invalidValue_returns400() {
        String token = registerAndGetToken("status-user2@example.com");
        String jobId = createJob(token);
        assertThat(setStatus(token, jobId, "maybe").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void setStatus_unknownJob_returns404() {
        String token = registerAndGetToken("status-user3@example.com");
        assertThat(setStatus(token, UUID.randomUUID().toString(), "saved").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<JsonNode> setStatus(String token, String jobId, String status) {
        return rest.exchange("/api/jobs/" + jobId + "/status", HttpMethod.PUT,
                new HttpEntity<>(Map.of("status", status), bearer(token)), JsonNode.class);
    }

    private String statusFor(String token, String jobId) {
        ResponseEntity<JsonNode> res = rest.exchange(
                "/api/jobs/statuses", HttpMethod.GET, new HttpEntity<>(bearer(token)), JsonNode.class);
        for (JsonNode n : res.getBody()) {
            if (n.get("jobId").asText().equals(jobId)) return n.get("status").asText();
        }
        return null;
    }

    private String createJob(String token) {
        ResponseEntity<JsonNode> create = rest.exchange(
                "/api/jobs", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "Engineer", "description", "Java, Spring"), bearer(token)),
                JsonNode.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return create.getBody().get("id").asText();
    }

    private String registerAndGetToken(String email) {
        ResponseEntity<JsonNode> reg = rest.postForEntity(
                "/api/auth/register",
                Map.of("email", email, "password", "supersecret", "fullName", "Status User"),
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
