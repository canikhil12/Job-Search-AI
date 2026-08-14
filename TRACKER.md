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

## Phase 2 — Resume Upload + Parse  ✅ _complete + deployed_
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
- [x] `mvnw verify` green with Docker — 18/18 tests pass (incl. resume Testcontainers integration test)
- [x] Frontend: resume upload UI on dashboard (upload, view extracted text, list, delete) — live-verified in browser
- [x] `frontend/vercel.json` SPA rewrite (fixes deep-link 404 on hard refresh of sub-routes)

Note: Supabase migrated to new API keys — the backend uses the **Secret key** (`sb_secret_…`,
the modern `service_role`) in `SUPABASE_SERVICE_ROLE_KEY`, sent as both `Authorization: Bearer` and `apikey`.

## Phase 3 — Job Ingest + Embeddings  ✅ _complete + deployed (real embeddings via OpenRouter)_
- [x] `V3__jobs.sql` — jobs table with `embedding vector(1536)` + HNSW cosine index
- [x] `embedding/` capability: `EmbeddingClient` port + `OpenAiEmbeddingClient` (prod) / `FakeEmbeddingClient` (tests/local), selected via `embedding.provider`
- [x] `job/` feature package: entity, JPA read repo, `JobVectorRepository` (JdbcTemplate vector insert), service, controller
- [x] Endpoints (JWT-protected): POST /api/jobs (ingest+embed), GET list, GET /{id}
- [x] `JobSeeder` — seeds 5 real Indeed postings on first startup, embedded through the active provider
- [x] Tests: FakeEmbeddingClientTest (4), JobServiceTest (2), JobIntegrationTest (3, verifies 1536-dim vector in pgvector) — full suite 27/27 green
- [x] Fixed a flaky Phase 1 test (JwtServiceTest tampered-token: flip first sig char, not last)
- [x] Env set on Render (EMBEDDING_PROVIDER=openai, EMBEDDING_API_KEY/BASE_URL/MODEL → OpenRouter); adapter targets any OpenAI-compatible endpoint
- [x] Verify live: `Seeded 5/5 jobs.`, GET /api/jobs serves 5 real-embedded postings
- [ ] Frontend: jobs list view (deferred — comes together with Phase 4 match UI)

**Note:** embeddings run through OpenRouter (`openai/text-embedding-3-small`, 1536d). Anthropic/OpenRouter
key reserved for Phases 5–6 (text generation).

## Phase 4 — Semantic Match  ✅ _complete + deployed_
- [x] `JobVectorRepository.search` — pgvector cosine KNN (`<=>` + HNSW), score = 1 - distance
- [x] `match/` feature package: MatchService + MatchController
- [x] `POST /api/matches` (match pasted text) and `GET /api/resumes/{id}/matches` (stored résumé, ownership-scoped), JWT-protected
- [x] Query text embedded on-demand through the same EmbeddingClient (OpenRouter in prod)
- [x] Tests: MatchServiceTest (5), MatchIntegrationTest (3, verifies ranking order + limit + auth) — full suite 35/35 green
- [x] Verify live: Java/Spring résumé ranks 3 backend jobs top, data-eng last (0.64 vs 0.50); a data-eng résumé inverts it (Cribl 0.50→0.72, #1) — genuinely semantic
- [x] Frontend: "Find matches" on the dashboard — ranked jobs, score bars, Apply links; live-verified in browser (backend_resume.pdf → 3 backend jobs top, Cribl last)

## Phase 5 — AI Gap Analysis (SSE)  ← _backend built, tests green; needs ANTHROPIC key + frontend_
- [x] `chat/` capability: ChatClient port + AnthropicChatClient (native Messages API, java.net.http streaming) / FakeChatClient
- [x] `analysis/` feature: AnalysisService (builds prompt, validates ownership → 404) + AnalysisController (SSE relay via SseEmitter on a TaskExecutor)
- [x] `GET /api/resumes/{id}/jobs/{jobId}/analysis` streams tokens (JSON-encoded frames)
- [x] Fixed Spring Security + async gotchas: JWT filter runs on async dispatch; no `produces` on the SSE endpoint (so errors render as JSON)
- [x] Tests: FakeChatClientTest (1) + AnalysisIntegrationTest (3, SSE stream + 404 + 401) — full suite 39/39 green
- [x] Frontend: "Analyze" on each matched job → live-streaming gap-analysis panel (fetch + ReadableStream SSE consumer, so the auth header works)
- [ ] Set `CHAT_PROVIDER=anthropic` + `ANTHROPIC_API_KEY` on Render
- [ ] Verify live: real streamed analysis from Claude

## Phase 6 — Cover Letters
- [ ] AI-generated, job-tailored cover letters

## Phase 7 — Dashboard
- [ ] Unified UI: matches, gaps, cover letters, application tracking

---

### Notes for Phase 1 verification
Verification steps 0–6 (Docker up, `mvnw verify`, run + curl, UI flow, `docker build`) require a running
Docker daemon. The Docker CLI was not on PATH in the scaffolding session — install/start Docker Desktop
(or Colima) and run the verification checklist in `README.md` before marking Phase 1 fully done.
