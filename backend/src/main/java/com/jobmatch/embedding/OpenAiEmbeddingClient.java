package com.jobmatch.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Production embedding adapter for any OpenAI-compatible embeddings API — OpenAI itself, or a
 * gateway like OpenRouter — selected purely by {@code embedding.base-url} and {@code embedding.model}.
 * Active when {@code embedding.provider=openai}; requires {@code EMBEDDING_API_KEY} (fails fast if missing).
 */
@Component
@ConditionalOnProperty(name = "embedding.provider", havingValue = "openai")
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private final RestClient client;
    private final String model;
    private final int dimension;

    public OpenAiEmbeddingClient(
            @Value("${embedding.api-key:}") String apiKey,
            @Value("${embedding.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${embedding.model:text-embedding-3-small}") String model,
            @Value("${embedding.dimension:1536}") int dimension) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "EMBEDDING_API_KEY must be set when embedding.provider=openai");
        }
        this.model = model;
        this.dimension = dimension;
        this.client = RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/$", ""))
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public float[] embed(String text) {
        try {
            EmbeddingResponse response = client.post()
                    .uri("/embeddings")
                    .body(Map.of("model", model, "input", text, "encoding_format", "float"))
                    .retrieve()
                    .body(EmbeddingResponse.class);
            if (response == null || response.data() == null || response.data().isEmpty()) {
                throw new EmbeddingException("OpenAI returned no embedding");
            }
            float[] vector = response.data().get(0).embedding();
            if (vector == null || vector.length != dimension) {
                throw new EmbeddingException(
                        "Expected embedding of dimension " + dimension + " but got "
                                + (vector == null ? "null" : vector.length));
            }
            return vector;
        } catch (RestClientException ex) {
            throw new EmbeddingException("OpenAI embeddings request failed", ex);
        }
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmbeddingResponse(List<Item> data) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Item(float[] embedding) {
        }
    }
}
