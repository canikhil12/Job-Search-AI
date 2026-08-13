package com.jobmatch.job.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateJobRequest(
        @NotBlank(message = "title is required")
        String title,

        String company,

        String location,

        @NotBlank(message = "description is required")
        String description,

        String sourceUrl
) {
}
