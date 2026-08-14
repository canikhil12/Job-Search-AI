package com.jobmatch;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AnalysisIntegrationTest {

    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PGVECTOR_IMAGE);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> "integration-test-secret-at-least-32-bytes-long!!");
        registry.add("jobs.seed-on-startup", () -> "false");
        // chat.provider defaults to "fake" -> canned stream, no API key/network.
    }

    @Autowired
    private TestRestTemplate rest;

    @Test
    void streamsGapAnalysisAsSse() {
        String token = registerAndGetToken("analysis-user@example.com");
        String resumeId = uploadResume(token);
        String jobId = createJob(token);

        ResponseEntity<String> analysis = rest.exchange(
                "/api/resumes/" + resumeId + "/jobs/" + jobId + "/analysis",
                HttpMethod.GET, new HttpEntity<>(bearer(token)), String.class);

        assertThat(analysis.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(analysis.getHeaders().getContentType().toString()).contains(MediaType.TEXT_EVENT_STREAM_VALUE);
        // SSE frames carry JSON-encoded tokens on separate `data:` lines, so the words appear but
        // not as one contiguous string.
        assertThat(analysis.getBody()).contains("data:").contains("Overall").contains("Suggestions");
    }

    @Test
    void unknownJob_returns404() {
        String token = registerAndGetToken("analysis-user2@example.com");
        String resumeId = uploadResume(token);
        ResponseEntity<JsonNode> res = rest.exchange(
                "/api/resumes/" + resumeId + "/jobs/" + UUID.randomUUID() + "/analysis",
                HttpMethod.GET, new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void withoutToken_returns401() {
        ResponseEntity<JsonNode> res = rest.getForEntity(
                "/api/resumes/" + UUID.randomUUID() + "/jobs/" + UUID.randomUUID() + "/analysis", JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String createJob(String token) {
        ResponseEntity<JsonNode> create = rest.exchange(
                "/api/jobs", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "Senior Backend Engineer",
                        "description", "Java, Spring Boot, AWS, Kubernetes"), bearer(token)),
                JsonNode.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return create.getBody().get("id").asText();
    }

    private String uploadResume(String token) {
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.APPLICATION_PDF);
        ByteArrayResource res = new ByteArrayResource(pdf("Senior Java engineer, Spring Boot, AWS")) {
            @Override
            public String getFilename() {
                return "cv.pdf";
            }
        };
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new HttpEntity<>(res, partHeaders));
        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<JsonNode> upload = rest.exchange(
                "/api/resumes", HttpMethod.POST, new HttpEntity<>(form, headers), JsonNode.class);
        assertThat(upload.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return upload.getBody().get("id").asText();
    }

    private String registerAndGetToken(String email) {
        ResponseEntity<JsonNode> reg = rest.postForEntity(
                "/api/auth/register",
                Map.of("email", email, "password", "supersecret", "fullName", "Analysis User"),
                JsonNode.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return reg.getBody().get("token").asText();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private byte[] pdf(String text) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(72, 720);
                cs.showText(text);
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build test PDF", ex);
        }
    }
}
