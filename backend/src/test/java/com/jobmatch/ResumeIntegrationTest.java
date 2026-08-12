package com.jobmatch;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.client.JdkClientHttpRequestFactory;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ResumeIntegrationTest {

    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PGVECTOR_IMAGE);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> "integration-test-secret-at-least-32-bytes-long!!");
        // resume.storage.provider defaults to "memory" -> InMemoryResumeStorage, no network.
    }

    @Autowired
    private TestRestTemplate rest;

    // TestRestTemplate's default JDK HttpURLConnection throws HttpRetryException on a 401 to a
    // POST with a body (it can't rewind to re-auth), masking the real 401. java.net.http doesn't.
    @BeforeEach
    void useJdkHttpClient() {
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void uploadListGetDownloadDelete_happyPath() {
        String token = registerAndGetToken("resume-user@example.com");
        byte[] pdf = pdfWithText("Ada Lovelace — Staff Software Engineer, Java & Postgres");

        // upload -> 201 with extracted text
        ResponseEntity<JsonNode> upload = rest.exchange(
                "/api/resumes", HttpMethod.POST, multipart(pdf, "ada.pdf", token), JsonNode.class);
        assertThat(upload.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = upload.getBody();
        assertThat(body).isNotNull();
        String resumeId = body.get("id").asText();
        assertThat(body.get("fileName").asText()).isEqualTo("ada.pdf");
        assertThat(body.get("extractedText").asText()).contains("Ada Lovelace");

        // list -> 1
        ResponseEntity<JsonNode> list = rest.exchange(
                "/api/resumes", HttpMethod.GET, new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody().size()).isEqualTo(1);

        // get by id -> has text
        ResponseEntity<JsonNode> get = rest.exchange(
                "/api/resumes/" + resumeId, HttpMethod.GET, new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get.getBody().get("extractedText").asText()).contains("Staff Software Engineer");

        // download -> original bytes
        ResponseEntity<byte[]> download = rest.exchange(
                "/api/resumes/" + resumeId + "/download", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), byte[].class);
        assertThat(download.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(download.getBody()).isEqualTo(pdf);

        // delete -> 204, then list empty
        ResponseEntity<Void> delete = rest.exchange(
                "/api/resumes/" + resumeId, HttpMethod.DELETE, new HttpEntity<>(bearer(token)), Void.class);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ResponseEntity<JsonNode> listAfter = rest.exchange(
                "/api/resumes", HttpMethod.GET, new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(listAfter.getBody().size()).isEqualTo(0);
    }

    @Test
    void upload_withoutToken_returns401() {
        ResponseEntity<JsonNode> upload = rest.exchange(
                "/api/resumes", HttpMethod.POST, multipart(pdfWithText("x"), "x.pdf", null), JsonNode.class);
        assertThat(upload.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void upload_unsupportedType_returns415() {
        String token = registerAndGetToken("txt-user@example.com");
        ByteArrayResource part = named("hi".getBytes(), "notes.txt");
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.TEXT_PLAIN);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new HttpEntity<>(part, partHeaders));
        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<JsonNode> upload = rest.exchange(
                "/api/resumes", HttpMethod.POST, new HttpEntity<>(form, headers), JsonNode.class);
        assertThat(upload.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    // --- helpers ---

    private String registerAndGetToken(String email) {
        ResponseEntity<JsonNode> reg = rest.postForEntity(
                "/api/auth/register",
                Map.of("email", email, "password", "supersecret", "fullName", "Test User"),
                JsonNode.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return reg.getBody().get("token").asText();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private HttpEntity<MultiValueMap<String, Object>> multipart(byte[] content, String filename, String token) {
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.APPLICATION_PDF);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new HttpEntity<>(named(content, filename), partHeaders));

        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(form, headers);
    }

    private ByteArrayResource named(byte[] content, String filename) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private byte[] pdfWithText(String text) {
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
