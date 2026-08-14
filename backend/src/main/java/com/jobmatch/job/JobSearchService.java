package com.jobmatch.job;

import com.jobmatch.embedding.EmbeddingClient;
import com.jobmatch.job.dto.JobResponse;
import com.jobmatch.job.search.JobSearchClient;
import com.jobmatch.job.search.JobSearchResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs a live job search, embeds + stores any new postings (deduped by provider id), and returns the
 * matching jobs. Existing postings are reused rather than re-embedded, so repeated searches are cheap.
 */
@Service
public class JobSearchService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 30;
    private static final int DEFAULT_MAX_DAYS_OLD = 3;

    private final JobSearchClient jobSearchClient;
    private final JobRepository jobRepository;
    private final JobVectorRepository jobVectorRepository;
    private final EmbeddingClient embeddingClient;

    public JobSearchService(JobSearchClient jobSearchClient,
                            JobRepository jobRepository,
                            JobVectorRepository jobVectorRepository,
                            EmbeddingClient embeddingClient) {
        this.jobSearchClient = jobSearchClient;
        this.jobRepository = jobRepository;
        this.jobVectorRepository = jobVectorRepository;
        this.embeddingClient = embeddingClient;
    }

    @Transactional
    public List<JobResponse> search(String query, String location, Integer maxDaysOld, Integer limit) {
        int max = clamp(limit, DEFAULT_LIMIT, MAX_LIMIT);
        int days = maxDaysOld == null || maxDaysOld < 1 ? DEFAULT_MAX_DAYS_OLD : maxDaysOld;

        List<JobSearchResult> results =
                jobSearchClient.search(query, location == null ? "" : location, days, max);

        List<String> externalIds = new ArrayList<>();
        for (JobSearchResult r : results) {
            if (r.externalId() == null) {
                continue;
            }
            externalIds.add(r.externalId());
            if (!jobRepository.existsByExternalId(r.externalId())) {
                ingest(r);
            }
        }
        if (externalIds.isEmpty()) {
            return List.of();
        }
        return jobRepository.findByExternalIdInOrderByPostedAtDesc(externalIds).stream()
                .map(JobResponse::from)
                .toList();
    }

    private void ingest(JobSearchResult r) {
        Job job = new Job(
                UUID.randomUUID(),
                r.title(),
                r.company(),
                r.location(),
                r.description(),
                "adzuna",
                r.sourceUrl(),
                r.externalId(),
                r.postedAt(),
                OffsetDateTime.now());
        float[] embedding = embeddingClient.embed(embeddingText(job));
        jobVectorRepository.insert(job, embedding);
    }

    private String embeddingText(Job job) {
        StringBuilder sb = new StringBuilder(job.getTitle());
        if (job.getCompany() != null) {
            sb.append(" at ").append(job.getCompany());
        }
        if (job.getLocation() != null) {
            sb.append(" (").append(job.getLocation()).append(')');
        }
        sb.append("\n\n").append(job.getDescription());
        return sb.toString();
    }

    private int clamp(Integer value, int fallback, int max) {
        if (value == null) {
            return fallback;
        }
        return Math.max(1, Math.min(value, max));
    }
}
