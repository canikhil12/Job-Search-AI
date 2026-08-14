package com.jobmatch.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Production chat adapter — streams from Anthropic's native Messages API. Uses java.net.http so the
 * SSE response is consumed line-by-line (no WebFlux needed); each {@code text_delta} is forwarded to
 * {@code onDelta}. Active when {@code chat.provider=anthropic}; requires {@code ANTHROPIC_API_KEY}.
 */
@Component
@ConditionalOnProperty(name = "chat.provider", havingValue = "anthropic")
public class AnthropicChatClient implements ChatClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final String anthropicVersion;
    private final int maxTokens;

    public AnthropicChatClient(
            ObjectMapper objectMapper,
            @Value("${chat.anthropic.api-key:}") String apiKey,
            @Value("${chat.anthropic.base-url:https://api.anthropic.com}") String baseUrl,
            @Value("${chat.anthropic.model:claude-sonnet-5}") String model,
            @Value("${chat.anthropic.version:2023-06-01}") String anthropicVersion,
            @Value("${chat.anthropic.max-tokens:1024}") int maxTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY must be set when chat.provider=anthropic");
        }
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.model = model;
        this.anthropicVersion = anthropicVersion;
        this.maxTokens = maxTokens;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    @Override
    public void streamCompletion(String system, String user, Consumer<String> onDelta) {
        String body;
        try {
            body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "max_tokens", maxTokens,
                    "stream", true,
                    "system", system,
                    "messages", List.of(Map.of("role", "user", "content", user))));
        } catch (Exception ex) {
            throw new ChatException("Failed to build Anthropic request", ex);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/messages"))
                .header("x-api-key", apiKey)
                .header("anthropic-version", anthropicVersion)
                .header("content-type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<Stream<String>> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() != 200) {
                String err = response.body().collect(Collectors.joining("\n"));
                throw new ChatException("Anthropic API returned " + response.statusCode() + ": " + truncate(err));
            }
            try (Stream<String> lines = response.body()) {
                lines.forEach(line -> handleLine(line, onDelta));
            }
        } catch (ChatException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ChatException("Anthropic streaming request failed", ex);
        }
    }

    @Override
    public String complete(String system, String user) {
        String body;
        try {
            body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "max_tokens", maxTokens,
                    "system", system,
                    "messages", List.of(Map.of("role", "user", "content", user))));
        } catch (Exception ex) {
            throw new ChatException("Failed to build Anthropic request", ex);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/messages"))
                .header("x-api-key", apiKey)
                .header("anthropic-version", anthropicVersion)
                .header("content-type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ChatException("Anthropic API returned " + response.statusCode() + ": "
                        + truncate(response.body()));
            }
            JsonNode content = objectMapper.readTree(response.body()).path("content");
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    return block.path("text").asText();
                }
            }
            throw new ChatException("Anthropic response had no text content");
        } catch (ChatException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ChatException("Anthropic request failed", ex);
        }
    }

    // SSE lines are "event: ..." / "data: {...}" / blank. We only need the JSON on data lines.
    private void handleLine(String line, Consumer<String> onDelta) {
        if (line == null || !line.startsWith("data:")) {
            return;
        }
        String json = line.substring("data:".length()).trim();
        if (json.isEmpty() || "[DONE]".equals(json)) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            String type = node.path("type").asText();
            if ("content_block_delta".equals(type)) {
                JsonNode delta = node.path("delta");
                if ("text_delta".equals(delta.path("type").asText())) {
                    onDelta.accept(delta.path("text").asText());
                }
            } else if ("error".equals(type)) {
                throw new ChatException("Anthropic stream error: " + node.path("error").path("message").asText());
            }
        } catch (ChatException ex) {
            throw ex;
        } catch (Exception ex) {
            // A malformed/unknown event line shouldn't kill the stream.
        }
    }

    private static String truncate(String s) {
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
