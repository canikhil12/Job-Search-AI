-- Phase 3: job postings with embeddings for semantic matching.
-- embedding dimension 1536 = OpenAI text-embedding-3-small.
CREATE TABLE jobs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       TEXT         NOT NULL,
    company     TEXT,
    location    TEXT,
    description TEXT         NOT NULL,
    source      TEXT         NOT NULL DEFAULT 'manual',
    source_url  TEXT,
    embedding   vector(1536) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- HNSW index for cosine-distance nearest-neighbour search (no training step, good recall).
-- Built on the empty table; Phase 4 queries it with the `<=>` operator.
CREATE INDEX idx_jobs_embedding ON jobs USING hnsw (embedding vector_cosine_ops);
