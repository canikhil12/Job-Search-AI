package com.jobmatch.job;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

/**
 * Vector-aware persistence for jobs via JdbcTemplate. Hibernate doesn't understand pgvector,
 * so inserting the embedding (and, in Phase 4, similarity search) is done in native SQL, casting
 * the array to the {@code vector} type. Plain reads stay in {@link JobRepository}.
 */
@Repository
public class JobVectorRepository {

    private final JdbcTemplate jdbc;

    public JobVectorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Inserts a job together with its embedding in a single statement. */
    public void insert(Job job, float[] embedding) {
        jdbc.update(
                """
                INSERT INTO jobs (id, title, company, location, description, source, source_url, embedding, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS vector), ?)
                """,
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getDescription(),
                job.getSource(),
                job.getSourceUrl(),
                toVectorLiteral(embedding),
                Timestamp.from(job.getCreatedAt().toInstant()));
    }

    /** Stored embedding dimension for a job — used to verify the pipeline end-to-end. */
    public int embeddingDimension(java.util.UUID jobId) {
        Integer dims = jdbc.queryForObject(
                "SELECT vector_dims(embedding) FROM jobs WHERE id = ?", Integer.class, jobId);
        return dims == null ? 0 : dims;
    }

    /** pgvector text form: [0.1,0.2,...]. */
    static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8 + 2);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
