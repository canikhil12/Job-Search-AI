package com.jobmatch.job;

import com.jobmatch.job.dto.JobMatchResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

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

    private static final RowMapper<JobMatchResponse> MATCH_MAPPER = (rs, i) -> new JobMatchResponse(
            UUID.fromString(rs.getString("id")),
            rs.getString("title"),
            rs.getString("company"),
            rs.getString("location"),
            rs.getString("source_url"),
            rs.getDouble("score"));

    /**
     * Nearest jobs to the query embedding by cosine similarity, best first. Uses pgvector's
     * {@code <=>} (cosine distance with the HNSW index); score = 1 - distance, so higher is closer.
     */
    public List<JobMatchResponse> search(float[] queryEmbedding, int limit) {
        String literal = toVectorLiteral(queryEmbedding);
        return jdbc.query(
                """
                SELECT id, title, company, location, source_url,
                       1 - (embedding <=> CAST(? AS vector)) AS score
                FROM jobs
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """,
                MATCH_MAPPER, literal, literal, limit);
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
