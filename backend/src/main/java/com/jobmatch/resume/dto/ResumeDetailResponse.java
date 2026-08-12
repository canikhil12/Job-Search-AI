package com.jobmatch.resume.dto;

import com.jobmatch.resume.Resume;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Full view of a resume, including the extracted text — used for GET /{id}. */
public record ResumeDetailResponse(
        UUID id,
        String fileName,
        String contentType,
        long sizeBytes,
        String extractedText,
        OffsetDateTime createdAt
) {
    public static ResumeDetailResponse from(Resume resume) {
        return new ResumeDetailResponse(
                resume.getId(),
                resume.getFileName(),
                resume.getContentType(),
                resume.getSizeBytes(),
                resume.getExtractedText(),
                resume.getCreatedAt());
    }
}
