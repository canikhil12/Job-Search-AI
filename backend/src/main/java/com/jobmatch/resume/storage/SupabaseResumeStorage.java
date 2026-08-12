package com.jobmatch.resume.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Production storage adapter — talks to the Supabase Storage REST API using the
 * service-role key (backend-only; it bypasses row-level security). Active when
 * {@code resume.storage.provider=supabase}.
 *
 * <p>Object URL shape: {@code <supabase-url>/storage/v1/object/<bucket>/<key>}.</p>
 */
@Component
@ConditionalOnProperty(name = "resume.storage.provider", havingValue = "supabase")
public class SupabaseResumeStorage implements ResumeStorage {

    private final RestClient client;
    private final String bucket;

    public SupabaseResumeStorage(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-role-key}") String serviceRoleKey,
            @Value("${supabase.storage.bucket:resumes}") String bucket) {
        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            throw new IllegalStateException("supabase.url must be set when resume.storage.provider=supabase");
        }
        if (serviceRoleKey == null || serviceRoleKey.isBlank()) {
            throw new IllegalStateException(
                    "supabase.service-role-key must be set when resume.storage.provider=supabase");
        }
        this.bucket = bucket;
        this.client = RestClient.builder()
                .baseUrl(supabaseUrl.replaceAll("/$", "") + "/storage/v1")
                .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                .defaultHeader("apikey", serviceRoleKey)
                .build();
    }

    @Override
    public void upload(String key, String contentType, byte[] content) {
        MediaType mediaType = safeMediaType(contentType);
        try {
            client.post()
                    .uri("/object/{bucket}/{key}", bucket, key)
                    // x-upsert lets a re-upload overwrite instead of 400-ing on an existing key.
                    .header("x-upsert", "true")
                    .contentType(mediaType)
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new StorageException("Failed to upload object to Supabase Storage: " + key, ex);
        }
    }

    @Override
    public byte[] download(String key) {
        try {
            byte[] body = client.get()
                    .uri("/object/{bucket}/{key}", bucket, key)
                    .retrieve()
                    .body(byte[].class);
            if (body == null) {
                throw new StorageException("Empty response downloading object: " + key);
            }
            return body;
        } catch (RestClientException ex) {
            throw new StorageException("Failed to download object from Supabase Storage: " + key, ex);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.delete()
                    .uri("/object/{bucket}/{key}", bucket, key)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            // Deleting a missing object should be a no-op, not a hard failure.
            throw new StorageException("Failed to delete object from Supabase Storage: " + key, ex);
        }
    }

    private static MediaType safeMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (RuntimeException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
