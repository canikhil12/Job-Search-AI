package com.jobmatch.embedding;

/**
 * Port for turning text into an embedding vector. The concrete adapter is chosen by the
 * {@code embedding.provider} property: OpenAI in prod, a deterministic fake for tests/local
 * (so the pipeline runs without an API key or network).
 */
public interface EmbeddingClient {

    /** Embeds the given text into a fixed-length vector. */
    float[] embed(String text);

    /** The dimension every returned vector has (must match the DB column, vector(1536)). */
    int dimension();
}
