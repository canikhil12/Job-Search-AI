-- Live job search (Adzuna): fetched postings carry a provider id (for dedupe) and a posted date
-- (for the "last 1–3 days" recency filter/display). Seed jobs leave these null.
ALTER TABLE jobs ADD COLUMN external_id TEXT;
ALTER TABLE jobs ADD COLUMN posted_at TIMESTAMPTZ;

-- One row per provider posting; NULLs (manual/seed jobs) are exempt from the uniqueness rule.
CREATE UNIQUE INDEX idx_jobs_external_id ON jobs (external_id) WHERE external_id IS NOT NULL;
