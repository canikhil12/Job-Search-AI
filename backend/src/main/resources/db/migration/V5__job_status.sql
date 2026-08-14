-- Per-user job tracking: a user can mark a job "saved" or "applied" (one status per job).
CREATE TABLE job_status (
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id     UUID        NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    status     TEXT        NOT NULL CHECK (status IN ('saved', 'applied')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, job_id)
);
