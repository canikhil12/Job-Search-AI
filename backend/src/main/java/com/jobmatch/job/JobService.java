package com.jobmatch.job;

import com.jobmatch.common.ResourceNotFoundException;
import com.jobmatch.embedding.EmbeddingClient;
import com.jobmatch.job.dto.CreateJobRequest;
import com.jobmatch.job.dto.JobResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobVectorRepository jobVectorRepository;
    private final EmbeddingClient embeddingClient;

    public JobService(JobRepository jobRepository,
                      JobVectorRepository jobVectorRepository,
                      EmbeddingClient embeddingClient) {
        this.jobRepository = jobRepository;
        this.jobVectorRepository = jobVectorRepository;
        this.embeddingClient = embeddingClient;
    }

    @Transactional
    public JobResponse ingest(CreateJobRequest request, String source) {
        Job job = new Job(
                UUID.randomUUID(),
                request.title(),
                request.company(),
                request.location(),
                request.description(),
                source,
                request.sourceUrl(),
                OffsetDateTime.now());
        float[] embedding = embeddingClient.embed(embeddingText(job));
        jobVectorRepository.insert(job, embedding);
        return JobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> list() {
        return jobRepository.findByOrderByCreatedAtDesc().stream()
                .map(JobResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobResponse get(UUID id) {
        return jobRepository.findById(id)
                .map(JobResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));
    }

    public long count() {
        return jobRepository.count();
    }

    /** The text we embed for a job — title, company, location, and description combined. */
    private String embeddingText(Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append(job.getTitle());
        if (job.getCompany() != null) {
            sb.append(" at ").append(job.getCompany());
        }
        if (job.getLocation() != null) {
            sb.append(" (").append(job.getLocation()).append(')');
        }
        sb.append("\n\n").append(job.getDescription());
        return sb.toString();
    }
}
