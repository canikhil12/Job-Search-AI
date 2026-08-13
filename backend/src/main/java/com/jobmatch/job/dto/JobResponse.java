package com.jobmatch.job.dto;

import com.jobmatch.job.Job;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String title,
        String company,
        String location,
        String description,
        String source,
        String sourceUrl,
        OffsetDateTime createdAt
) {
    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getDescription(),
                job.getSource(),
                job.getSourceUrl(),
                job.getCreatedAt());
    }
}
