package com.jobmatch.job.search;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Deterministic stand-in job search — returns a couple of canned postings so the search/ingest flow
 * works in tests and local dev without an API key. Active unless {@code jobs.search.provider=adzuna}.
 */
@Component
@ConditionalOnProperty(name = "jobs.search.provider", havingValue = "fake", matchIfMissing = true)
public class FakeJobSearchClient implements JobSearchClient {

    @Override
    public List<JobSearchResult> search(String query, String location, int maxDaysOld, int limit) {
        OffsetDateTime now = OffsetDateTime.now();
        List<JobSearchResult> all = List.of(
                new JobSearchResult("fake-1", "Senior " + query + " Engineer", "Acme Corp",
                        location.isBlank() ? "Remote" : location,
                        "Build and operate backend services with Java, Spring Boot, and AWS.",
                        "https://example.com/apply/fake-1", now.minusDays(1)),
                new JobSearchResult("fake-2", query + " Developer", "Globex",
                        location.isBlank() ? "Remote" : location,
                        "Work across the stack with TypeScript, React, and Node.js.",
                        "https://example.com/apply/fake-2", now.minusDays(2)));
        return all.stream().limit(Math.max(1, limit)).toList();
    }
}
