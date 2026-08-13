package com.jobmatch.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatch.job.dto.CreateJobRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * On startup, if the jobs table is empty, ingests the bundled real-world job postings
 * (seed-jobs.json) through {@link JobService} so each gets a real embedding from the active
 * provider. Idempotent: once jobs exist, it does nothing. Toggle with {@code jobs.seed-on-startup}.
 */
@Component
@ConditionalOnProperty(name = "jobs.seed-on-startup", havingValue = "true", matchIfMissing = true)
public class JobSeeder {

    private static final Logger log = LoggerFactory.getLogger(JobSeeder.class);
    private static final String SEED_FILE = "seed-jobs.json";

    private final JobService jobService;
    private final ObjectMapper objectMapper;
    private final String embeddingProvider;

    public JobSeeder(JobService jobService,
                     ObjectMapper objectMapper,
                     @Value("${embedding.provider:fake}") String embeddingProvider) {
        this.jobService = jobService;
        this.objectMapper = objectMapper;
        this.embeddingProvider = embeddingProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        // Only seed with a real embedding provider — seeding under the deterministic fake would
        // fill the table with meaningless vectors, and "seed only if empty" would then never let
        // the real provider re-embed them.
        if ("fake".equalsIgnoreCase(embeddingProvider)) {
            log.info("Embedding provider is 'fake'; skipping job seed (set EMBEDDING_PROVIDER to seed).");
            return;
        }
        if (jobService.count() > 0) {
            log.info("Jobs already present ({}), skipping seed.", jobService.count());
            return;
        }
        List<CreateJobRequest> seeds;
        try (InputStream in = new ClassPathResource(SEED_FILE).getInputStream()) {
            seeds = objectMapper.readValue(in, new com.fasterxml.jackson.core.type.TypeReference<>() {
            });
        } catch (Exception ex) {
            log.warn("Could not read {}; skipping seed.", SEED_FILE, ex);
            return;
        }

        int ok = 0;
        for (CreateJobRequest seed : seeds) {
            try {
                jobService.ingest(seed, "seed:indeed");
                ok++;
            } catch (Exception ex) {
                // Best-effort: a failing embedding (e.g. bad API key) must not stop startup.
                log.warn("Failed to seed job '{}': {}", seed.title(), ex.getMessage());
            }
        }
        log.info("Seeded {}/{} jobs.", ok, seeds.size());
    }
}
