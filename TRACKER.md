# JobMatch AI — Phase Tracker

> **Every session starts by reading this file.** It is the source of truth for what is
> done and what is next. Check items off as they land; keep the current phase at the top of mind.

## Phase 1 — Scaffold + Auth  ← _in progress_
- [x] Monorepo layout (`backend/`, `frontend/`, root compose + docs)
- [x] Dockerized Postgres 16 (`pgvector/pgvector:pg16`) via `docker-compose.yml`
- [x] Flyway `V1__init.sql`: enable `vector` extension, create `users` table
- [x] Spring Boot backend (Java 21, Maven, package `com.jobmatch`, no Lombok, records for DTOs)
- [x] JWT auth: `JwtService` (HS256, 60-min expiry, fail-fast on missing/short secret)
- [x] `JwtAuthenticationFilter` (`OncePerRequestFilter`) populates SecurityContext
- [x] `POST /api/auth/register` (validation, BCrypt, 409 on duplicate email)
- [x] `POST /api/auth/login` (`{ token, expiresAt, user }`)
- [x] `GET /api/auth/me` (JWT-protected)
- [x] `SecurityConfig` (stateless, CSRF off, permit register/login/health), `CorsConfig` (localhost:5173)
- [x] `GlobalExceptionHandler` + `ApiError` record (no stack-trace leakage)
- [x] Actuator health endpoint only
- [x] `application.yml` reads DB/JWT from env with local defaults; `.env.example`
- [x] Frontend: Vite + React + TS, pages `/register` `/login` `/dashboard` (protected)
- [x] Auth context (token in memory + localStorage, Bearer header, redirect on 401), Vite dev proxy `/api`
- [x] Unit tests: `JwtService` (round-trip, expired, tampered, weak-secret)
- [x] Integration tests: Testcontainers register→login→/me, dup 409, wrong-pw 401, no-token 401
- [ ] **Verification pass** (needs Docker running): `docker compose up`, `mvnw verify`, run + curl, UI flow, `docker build`

## Phase 2 — Resume Upload + Parse
- [ ] File upload endpoint, storage, PDF/DOCX text extraction, parsed-resume persistence

## Phase 3 — Job Ingest + Embeddings
- [ ] Job ingestion pipeline, embedding generation, pgvector storage

## Phase 4 — Semantic Match
- [ ] Vector similarity search, resume↔job scoring, ranked results

## Phase 5 — AI Gap Analysis (SSE)
- [ ] Streaming (Server-Sent Events) gap analysis between a resume and a job

## Phase 6 — Cover Letters
- [ ] AI-generated, job-tailored cover letters

## Phase 7 — Dashboard
- [ ] Unified UI: matches, gaps, cover letters, application tracking

---

### Notes for Phase 1 verification
Verification steps 0–6 (Docker up, `mvnw verify`, run + curl, UI flow, `docker build`) require a running
Docker daemon. The Docker CLI was not on PATH in the scaffolding session — install/start Docker Desktop
(or Colima) and run the verification checklist in `README.md` before marking Phase 1 fully done.
