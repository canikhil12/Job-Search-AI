package com.jobmatch.job.search;

import java.util.List;

/**
 * Port for live job search. The adapter is chosen by {@code jobs.search.provider}: Adzuna in prod,
 * a fake for tests/local (so search works without an API key).
 */
public interface JobSearchClient {

    /**
     * Searches recent postings.
     *
     * @param query      role / keywords
     * @param location   city/region (provider-specific; may be blank)
     * @param maxDaysOld only return postings at most this many days old (recency filter)
     * @param limit      max results
     */
    List<JobSearchResult> search(String query, String location, int maxDaysOld, int limit);
}
