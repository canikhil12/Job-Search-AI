package com.jobmatch.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Deterministic stand-in embedding — same text always yields the same unit vector of the
 * configured dimension. Meaningless for real semantic similarity, but it lets the ingest/search
 * pipeline run in tests and local dev without an API key. Active unless
 * {@code embedding.provider=openai}.
 */
@Component
@ConditionalOnProperty(name = "embedding.provider", havingValue = "fake", matchIfMissing = true)
public class FakeEmbeddingClient implements EmbeddingClient {

    private final int dimension;

    public FakeEmbeddingClient(@Value("${embedding.dimension:1536}") int dimension) {
        this.dimension = dimension;
    }

    @Override
    public float[] embed(String text) {
        // Seed by content so the vector is reproducible for the same input.
        Random rng = new Random(text == null ? 0 : text.hashCode());
        float[] vector = new float[dimension];
        double sumSquares = 0.0;
        for (int i = 0; i < dimension; i++) {
            float v = (float) (rng.nextDouble() * 2.0 - 1.0);
            vector[i] = v;
            sumSquares += (double) v * v;
        }
        // L2-normalize so values sit on the unit sphere, like real embeddings.
        float norm = (float) Math.sqrt(sumSquares);
        if (norm > 0) {
            for (int i = 0; i < dimension; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }

    @Override
    public int dimension() {
        return dimension;
    }
}
