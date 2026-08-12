-- Phase 2: uploaded resumes. The raw file lives in Supabase Storage; this table holds
-- metadata + the extracted text (which later phases embed for semantic matching).
CREATE TABLE resumes (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name      TEXT        NOT NULL,
    content_type   TEXT        NOT NULL,
    size_bytes     BIGINT      NOT NULL,
    -- Object key in the Supabase Storage bucket, e.g. "<user_id>/<resume_id>.pdf".
    storage_key    TEXT        NOT NULL,
    extracted_text TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Resumes are always listed/filtered by owner.
CREATE INDEX idx_resumes_user_id ON resumes (user_id);
