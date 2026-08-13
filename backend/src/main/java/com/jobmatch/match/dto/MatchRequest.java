package com.jobmatch.match.dto;

import jakarta.validation.constraints.NotBlank;

/** Match arbitrary text (e.g. pasted résumé / skills) against the job corpus. */
public record MatchRequest(
        @NotBlank(message = "text is required")
        String text,

        Integer limit
) {
}
