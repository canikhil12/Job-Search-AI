package com.jobmatch.jobstatus;

import com.jobmatch.jobstatus.dto.JobStatusResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Persistence for per-user job tracking (saved/applied) via JdbcTemplate upsert. */
@Repository
public class JobStatusRepository {

    private final JdbcTemplate jdbc;

    public JobStatusRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Sets (or updates) the status for a user's job. */
    public void upsert(UUID userId, UUID jobId, String status) {
        jdbc.update(
                """
                INSERT INTO job_status (user_id, job_id, status, updated_at)
                VALUES (?, ?, ?, now())
                ON CONFLICT (user_id, job_id) DO UPDATE SET status = EXCLUDED.status, updated_at = now()
                """,
                userId, jobId, status);
    }

    public void delete(UUID userId, UUID jobId) {
        jdbc.update("DELETE FROM job_status WHERE user_id = ? AND job_id = ?", userId, jobId);
    }

    public List<JobStatusResponse> findByUser(UUID userId) {
        return jdbc.query(
                "SELECT job_id, status FROM job_status WHERE user_id = ? ORDER BY updated_at DESC",
                (rs, i) -> new JobStatusResponse(UUID.fromString(rs.getString("job_id")), rs.getString("status")),
                userId);
    }
}
