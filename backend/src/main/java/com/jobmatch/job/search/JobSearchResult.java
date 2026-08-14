package com.jobmatch.job.search;

import java.time.OffsetDateTime;

/** One posting returned by a live job-search provider (e.g. Adzuna), before it's embedded/stored. */
public record JobSearchResult(
        String externalId,
        String title,
        String company,
        String location,
        String description,
        String sourceUrl,
        OffsetDateTime postedAt
) {
}
