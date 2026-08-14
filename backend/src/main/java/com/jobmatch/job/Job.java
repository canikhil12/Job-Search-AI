package com.jobmatch.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A job posting. The {@code embedding} column (pgvector) is deliberately NOT mapped here —
 * Hibernate doesn't know the vector type, so embeddings are written/queried via JdbcTemplate
 * ({@link JobVectorRepository}). This entity covers plain CRUD reads.
 */
@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String company;

    private String location;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String source;

    @Column(name = "source_url")
    private String sourceUrl;

    /** Provider posting id (e.g. Adzuna) for dedupe; null for manual/seed jobs. */
    @Column(name = "external_id")
    private String externalId;

    /** When the posting went live at the provider; null for manual/seed jobs. */
    @Column(name = "posted_at")
    private OffsetDateTime postedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Job() {
        // for JPA
    }

    public Job(UUID id, String title, String company, String location, String description,
               String source, String sourceUrl, OffsetDateTime createdAt) {
        this(id, title, company, location, description, source, sourceUrl, null, null, createdAt);
    }

    public Job(UUID id, String title, String company, String location, String description,
               String source, String sourceUrl, String externalId, OffsetDateTime postedAt,
               OffsetDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.description = description;
        this.source = source;
        this.sourceUrl = sourceUrl;
        this.externalId = externalId;
        this.postedAt = postedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCompany() {
        return company;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public String getSource() {
        return source;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getExternalId() {
        return externalId;
    }

    public OffsetDateTime getPostedAt() {
        return postedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
