-- Enable pgvector. Unused in Phase 1, but the production DB (Neon) will store embeddings
-- for semantic job matching in a later phase, so we provision the extension up front.
CREATE EXTENSION IF NOT EXISTS vector;

-- gen_random_uuid() is built into PostgreSQL 13+ (no pgcrypto extension required).
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         TEXT        NOT NULL UNIQUE,
    password_hash TEXT        NOT NULL,
    full_name     TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
