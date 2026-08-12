package com.jobmatch.resume;

import com.jobmatch.common.ResourceNotFoundException;
import com.jobmatch.resume.dto.ResumeDetailResponse;
import com.jobmatch.resume.dto.ResumeDownload;
import com.jobmatch.resume.dto.ResumeResponse;
import com.jobmatch.resume.parse.ResumeTextExtractor;
import com.jobmatch.resume.storage.ResumeStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ResumeService {

    // Accepted upload types -> file extension used for the storage key.
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "application/pdf", "pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");

    private final ResumeRepository resumeRepository;
    private final ResumeStorage storage;
    private final ResumeTextExtractor textExtractor;

    public ResumeService(ResumeRepository resumeRepository,
                         ResumeStorage storage,
                         ResumeTextExtractor textExtractor) {
        this.resumeRepository = resumeRepository;
        this.storage = storage;
        this.textExtractor = textExtractor;
    }

    @Transactional
    public ResumeDetailResponse upload(UUID userId, MultipartFile file) {
        byte[] content = readBytes(file);
        String contentType = normalizeContentType(file);
        String extension = ALLOWED_TYPES.get(contentType);
        if (extension == null) {
            throw new UnsupportedFileTypeException(
                    "Unsupported file type '" + contentType + "'. Upload a PDF or DOCX resume.");
        }

        UUID resumeId = UUID.randomUUID();
        String storageKey = userId + "/" + resumeId + "." + extension;

        // Store the raw file first, then parse. If parsing fails we roll back the DB row,
        // but the object is already uploaded — acceptable (a re-upload overwrites the key).
        storage.upload(storageKey, contentType, content);
        String extractedText = textExtractor.extract(content);

        Resume resume = new Resume(
                resumeId,
                userId,
                originalFileName(file),
                contentType,
                content.length,
                storageKey,
                extractedText,
                OffsetDateTime.now());
        resumeRepository.save(resume);
        return ResumeDetailResponse.from(resume);
    }

    @Transactional(readOnly = true)
    public List<ResumeResponse> list(UUID userId) {
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ResumeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeDetailResponse get(UUID userId, UUID resumeId) {
        return ResumeDetailResponse.from(requireOwned(userId, resumeId));
    }

    @Transactional(readOnly = true)
    public ResumeDownload download(UUID userId, UUID resumeId) {
        Resume resume = requireOwned(userId, resumeId);
        byte[] content = storage.download(resume.getStorageKey());
        return new ResumeDownload(resume.getFileName(), resume.getContentType(), content);
    }

    @Transactional
    public void delete(UUID userId, UUID resumeId) {
        Resume resume = requireOwned(userId, resumeId);
        resumeRepository.delete(resume);
        // Remove the object after the row; a leftover object on failure is harmless.
        storage.delete(resume.getStorageKey());
    }

    private Resume requireOwned(UUID userId, UUID resumeId) {
        return resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found: " + resumeId));
    }

    private byte[] readBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UnsupportedFileTypeException("No file was uploaded.");
        }
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read uploaded file", ex);
        }
    }

    private String normalizeContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType == null ? "" : contentType.split(";")[0].trim().toLowerCase();
    }

    private String originalFileName(MultipartFile file) {
        String name = file.getOriginalFilename();
        return (name == null || name.isBlank()) ? "resume" : name;
    }
}
