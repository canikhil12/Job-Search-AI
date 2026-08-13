package com.jobmatch.embedding;

/** Wraps any failure generating an embedding (e.g. the OpenAI call). Mapped to 502. */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmbeddingException(String message) {
        super(message);
    }
}
