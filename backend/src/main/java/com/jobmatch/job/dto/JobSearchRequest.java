package com.jobmatch.job.dto;

import jakarta.validation.constraints.NotBlank;

/** Live job-search request: role/keywords, optional location, recency and result count. */
public record JobSearchRequest(
        @NotBlank(message = "query is required")
        String query,

        String location,

        Integer maxDaysOld,

        Integer limit
) {
}
