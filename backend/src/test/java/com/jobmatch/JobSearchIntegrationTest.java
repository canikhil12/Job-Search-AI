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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class JobSearchIntegrationTest {

    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PGVECTOR_IMAGE);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> "integration-test-secret-at-least-32-bytes-long!!");
        registry.add("jobs.seed-on-startup", () -> "false");
        // jobs.search.provider + embedding.provider default to "fake".
    }

    @Autowired
    private TestRestTemplate rest;

    @BeforeEach
    void useJdkHttpClient() {
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void search_storesResults_andDedupesOnRepeat() {
        String token = registerAndGetToken("search-user@example.com");

        ResponseEntity<JsonNode> first = search(token, "Java Backend");
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody().size()).isEqualTo(2); // fake provider returns 2
        assertThat(first.getBody().get(0).has("postedAt")).isTrue();
        assertThat(first.getBody().get(0).get("sourceUrl").asText()).startsWith("https://");

        // repeat search: same postings are reused, not duplicated
        ResponseEntity<JsonNode> second = search(token, "Java Backend");
        assertThat(second.getBody().size()).isEqualTo(2);

        ResponseEntity<JsonNode> all = rest.exchange(
                "/api/jobs", HttpMethod.GET, new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(all.getBody().size()).isEqualTo(2); // deduped by external id
    }

    @Test
    void search_withoutToken_returns401() {
        ResponseEntity<JsonNode> res = rest.postForEntity(
                "/api/jobs/search", Map.of("query", "anything"), JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<JsonNode> search(String token, String query) {
        return rest.exchange("/api/jobs/search", HttpMethod.POST,
                new HttpEntity<>(Map.of("query", query, "maxDaysOld", 3, "limit", 10), bearer(token)),
                JsonNode.class);
    }

    private String registerAndGetToken(String email) {
        ResponseEntity<JsonNode> reg = rest.postForEntity(
                "/api/auth/register",
                Map.of("email", email, "password", "supersecret", "fullName", "Search User"),
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
