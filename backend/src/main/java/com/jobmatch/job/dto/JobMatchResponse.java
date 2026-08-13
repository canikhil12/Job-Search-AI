package com.jobmatch.job.dto;

import java.util.UUID;

/**
 * A job ranked against a query, with its cosine similarity score in [-1, 1]
 * (1 = identical direction). Description is omitted to keep match lists lean.
 */
public record JobMatchResponse(
        UUID id,
        String title,
        String company,
        String location,
        String sourceUrl,
        double score
) {
}
