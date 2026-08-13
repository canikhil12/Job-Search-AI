# JobMatch AI — Phase Tracker

> **Every session starts by reading this file.** It is the source of truth for what is
> done and what is next. Check items off as they land; keep the current phase at the top of mind.

## Phase 1 — Scaffold + Auth  ✅ _complete + deployed_
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

### Phase 1 deployment (Supabase + Render + Vercel) — see DEPLOYMENT.md
- [x] Frontend API base URL made env-configurable (`VITE_API_BASE_URL`) + `frontend/.env.example`
- [x] Backend binds `$PORT`; Hikari pool sized for managed Postgres
- [x] `render.yaml` blueprint (Docker, health check, secrets as `sync:false`)
- [x] `DEPLOYMENT.md` runbook (Supabase session pooler, Render, Vercel, CORS wiring, troubleshooting)
- [x] Supabase project created + `vector` extension enabled (ca-central-1, session pooler)
- [x] Backend live on Render (`https://jobmatch-backend-wxjw.onrender.com`, Flyway V1 applied, register/login/me verified)
- [x] Frontend live on Vercel (`https://job-search-ai-green.vercel.app`, `VITE_API_BASE_URL` → Render URL)
- [x] `CORS_ALLOWED_ORIGINS` set to the Vercel URL; preflight + credentials verified via curl

**Phase 1 COMPLETE — deployed end-to-end (Vercel + Render + Supabase).**
Live URLs:
- Frontend: https://job-search-ai-green.vercel.app
- Backend:  https://jobmatch-backend-wxjw.onrender.com  (health: `/actuator/health`)

## Phase 2 — Resume Upload + Parse  ← _backend in progress_
- [x] `resume/` feature package (controller, service, entity, repository, DTOs)
- [x] `V2__resumes.sql` migration (resumes table, FK → users, index on user_id)
- [x] Storage abstraction: `ResumeStorage` port + `SupabaseResumeStorage` (prod) / `InMemoryResumeStorage` (dev/tests), selected via `resume.storage.provider`
- [x] Text extraction with Apache Tika (`ResumeTextExtractor`, PDF + DOCX)
- [x] Endpoints (all JWT-protected, ownership-scoped): POST upload, GET list, GET one, GET download, DELETE
- [x] Multipart size limit (5MB) + new error mappings (415/422/413/502) in GlobalExceptionHandler
- [x] Unit tests: `ResumeServiceTest` (validation, ownership, orchestration) — 5 green
- [x] Integration test: `ResumeIntegrationTest` (Testcontainers, real PDF via PDFBox) — needs Docker to run
- [x] Supabase Storage bucket `resumes` created + `SUPABASE_URL`/`SUPABASE_SERVICE_ROLE_KEY`/`RESUME_STORAGE_PROVIDER=supabase` set on Render
- [x] **Live verified in prod:** upload → Tika extract → Supabase Storage → download (byte-perfect) → delete
- [ ] `mvnw verify` green with Docker (runs the resume integration test)
- [x] Frontend: resume upload UI on dashboard (upload, view extracted text, list, delete) — live-verified in browser
- [x] `frontend/vercel.json` SPA rewrite (fixes deep-link 404 on hard refresh of sub-routes)

Note: Supabase migrated to new API keys — the backend uses the **Secret key** (`sb_secret_…`,
the modern `service_role`) in `SUPABASE_SERVICE_ROLE_KEY`, sent as both `Authorization: Bearer` and `apikey`.

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
