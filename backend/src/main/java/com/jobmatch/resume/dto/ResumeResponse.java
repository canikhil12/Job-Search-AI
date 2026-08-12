package com.jobmatch.resume.dto;

import com.jobmatch.resume.Resume;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Metadata view of a resume (no extracted text) — used for list responses. */
public record ResumeResponse(
        UUID id,
        String fileName,
        String contentType,
        long sizeBytes,
        OffsetDateTime createdAt
) {
    public static ResumeResponse from(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getFileName(),
                resume.getContentType(),
                resume.getSizeBytes(),
                resume.getCreatedAt());
    }
}
