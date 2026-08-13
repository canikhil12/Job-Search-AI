package com.jobmatch.match;

import com.jobmatch.common.ResourceNotFoundException;
import com.jobmatch.embedding.EmbeddingClient;
import com.jobmatch.job.JobVectorRepository;
import com.jobmatch.job.dto.JobMatchResponse;
import com.jobmatch.resume.Resume;
import com.jobmatch.resume.ResumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MatchService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final EmbeddingClient embeddingClient;
    private final JobVectorRepository jobVectorRepository;
    private final ResumeRepository resumeRepository;

    public MatchService(EmbeddingClient embeddingClient,
                        JobVectorRepository jobVectorRepository,
                        ResumeRepository resumeRepository) {
        this.embeddingClient = embeddingClient;
        this.jobVectorRepository = jobVectorRepository;
        this.resumeRepository = resumeRepository;
    }

    /** Ranks jobs against arbitrary text (embedded on the fly). */
    public List<JobMatchResponse> matchText(String text, Integer limit) {
        float[] query = embeddingClient.embed(text);
        return jobVectorRepository.search(query, clampLimit(limit));
    }

    /** Ranks jobs against a stored résumé's extracted text (ownership-scoped). */
    @Transactional(readOnly = true)
    public List<JobMatchResponse> matchResume(UUID userId, UUID resumeId, Integer limit) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found: " + resumeId));
        String text = resume.getExtractedText();
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return jobVectorRepository.search(embeddingClient.embed(text), clampLimit(limit));
    }

    private int clampLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
