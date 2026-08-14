package com.jobmatch.job.search;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Live job-search adapter for the Adzuna API. Active when {@code jobs.search.provider=adzuna};
 * requires {@code ADZUNA_APP_ID} and {@code ADZUNA_APP_KEY}. Supports the "posted in the last N days"
 * recency filter via {@code max_days_old}.
 */
@Component
@ConditionalOnProperty(name = "jobs.search.provider", havingValue = "adzuna")
public class AdzunaJobSearchClient implements JobSearchClient {

    private final RestClient client;
    private final String appId;
    private final String appKey;
    private final String country;

    public AdzunaJobSearchClient(
            @Value("${jobs.adzuna.app-id:}") String appId,
            @Value("${jobs.adzuna.app-key:}") String appKey,
            @Value("${jobs.adzuna.country:us}") String country,
            @Value("${jobs.adzuna.base-url:https://api.adzuna.com/v1/api}") String baseUrl) {
        if (appId == null || appId.isBlank() || appKey == null || appKey.isBlank()) {
            throw new IllegalStateException(
                    "ADZUNA_APP_ID and ADZUNA_APP_KEY must be set when jobs.search.provider=adzuna");
        }
        this.appId = appId;
        this.appKey = appKey;
        this.country = country;
        this.client = RestClient.builder().baseUrl(baseUrl.replaceAll("/$", "")).build();
    }

    @Override
    public List<JobSearchResult> search(String query, String location, int maxDaysOld, int limit) {
        try {
            JsonNode body = client.get()
                    .uri(b -> {
                        b.path("/jobs/{country}/search/1")
                                .queryParam("app_id", appId)
                                .queryParam("app_key", appKey)
                                .queryParam("results_per_page", limit)
                                .queryParam("what", query)
                                .queryParam("max_days_old", maxDaysOld)
                                .queryParam("content-type", "application/json");
                        if (location != null && !location.isBlank()) {
                            b.queryParam("where", location);
                        }
                        return b.build(country);
                    })
                    .retrieve()
                    .body(JsonNode.class);
            return parse(body);
        } catch (RestClientException ex) {
            throw new JobSearchException("Adzuna search request failed", ex);
        }
    }

    private List<JobSearchResult> parse(JsonNode body) {
        List<JobSearchResult> results = new ArrayList<>();
        if (body == null) {
            return results;
        }
        for (JsonNode r : body.path("results")) {
            String id = r.path("id").asText(null);
            if (id == null) {
                continue;
            }
            results.add(new JobSearchResult(
                    "adzuna:" + id,
                    r.path("title").asText(""),
                    r.path("company").path("display_name").asText(null),
                    r.path("location").path("display_name").asText(null),
                    r.path("description").asText(""),
                    r.path("redirect_url").asText(null),
                    parseDate(r.path("created").asText(null))));
        }
        return results;
    }

    private OffsetDateTime parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
