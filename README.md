# JobMatch AI

[![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF?logo=vite&logoColor=white)](https://vite.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%20+%20pgvector-4169E1?logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)
[![Status](https://img.shields.io/badge/status-Phase%201-yellow)](./TRACKER.md)

An AI-powered resume & job-match platform (portfolio project). This repo is being built in
phases — see [`TRACKER.md`](./TRACKER.md) for status and [`DECISIONS.md`](./DECISIONS.md) for
the reasoning behind non-obvious choices.

**Phase 1 (current):** monorepo scaffold, Dockerized local dev, DB migrations, and JWT auth
(register / login / me). No resume upload, embeddings, or AI features yet.

## Stack
| Area      | Choice |
|-----------|--------|
| Backend   | Java 21, Spring Boot 3.3, Maven, package root `com.jobmatch` |
| Security  | Spring Security, stateless HS256 JWT (access token only) |
| Database  | PostgreSQL 16 + `pgvector` (local via Docker; Neon in prod) |
| Migrations| Flyway |
| Frontend  | React 19 + Vite + TypeScript, plain `fetch`, react-router |

## Layout
```
jobmatch-ai/
├── backend/     Spring Boot API (auth, user, config, common)
├── frontend/    Vite + React + TS SPA
├── docker-compose.yml   Postgres 16 (pgvector/pgvector:pg16)
├── TRACKER.md   phase checklist — read this first every session
└── DECISIONS.md design decisions + interview talking points
```

## Prerequisites
- JDK 21 (the Maven wrapper `./mvnw` is committed)
- Node 18+ / npm
- Docker (Desktop or Colima) — required for the database and for Testcontainers tests

## Quick start

### 1. Database
```bash
docker compose up -d        # Postgres 16 + pgvector on localhost:5432
```

### 2. Backend
```bash
cd backend
cp .env.example .env         # then edit JWT_SECRET for anything non-local
set -a && source .env && set +a
./mvnw spring-boot:run
```
The API listens on `http://localhost:8080`. Health: `GET /actuator/health`.

### 3. Frontend
```bash
cd frontend
npm install
npm run dev                  # http://localhost:5173  (proxies /api -> :8080)
```

## API (Phase 1)
| Method | Path                 | Auth | Body / Notes |
|--------|----------------------|------|--------------|
| POST   | `/api/auth/register` | no   | `{ email, password (>=8), fullName }` → 201 `{ token, expiresAt, user }` |
| POST   | `/api/auth/login`    | no   | `{ email, password }` → 200 `{ token, expiresAt, user }` |
| GET    | `/api/auth/me`       | yes  | Bearer token → 200 `{ id, email, fullName }` |
| GET    | `/actuator/health`   | no   | 200 `{ "status": "UP" }` |

## Environment variables
See [`backend/.env.example`](./backend/.env.example). Key ones:
- `JWT_SECRET` — **must be ≥ 32 bytes**; the app fails fast at startup otherwise.
- `DATABASE_URL` / `DATABASE_USER` / `DATABASE_PASSWORD` — default to the compose values.
- `CORS_ALLOWED_ORIGINS` — comma-separated; defaults to `http://localhost:5173`.

## Tests
```bash
cd backend
./mvnw verify                # unit tests + Testcontainers integration tests (needs Docker running)
```
Testcontainers spins up a real `pgvector/pgvector:pg16` container. If it fails to start, that is an
environment problem (Docker not running, image pull, resource limits) — not a test to disable.

<details>
<summary>If <code>docker</code> isn't on your PATH (e.g. Docker Desktop with a non-standard socket)</summary>

Point Testcontainers at the standard socket so its Ryuk cleanup container can bind-mount it
(mounting the per-user <code>~/.docker/run/docker.sock</code> path fails with "operation not supported"):

```bash
export PATH="/Applications/Docker.app/Contents/Resources/bin:$PATH"
export DOCKER_HOST="unix:///var/run/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/var/run/docker.sock"
./mvnw verify
```
</details>

## Phase 1 verification checklist
0. `docker info` succeeds (daemon running)
1. `docker compose up -d` → postgres healthy
2. `cd backend && ./mvnw verify` → all tests green (incl. Testcontainers)
3. `./mvnw spring-boot:run` → `curl localhost:8080/actuator/health` returns `UP`
4. curl register → login → `/me` with the token succeeds; `/me` without a token → 401
5. `cd frontend && npm install && npm run dev` → register + login through the UI lands on the dashboard
6. `docker build backend/` succeeds

### Example curl flow
```bash
# register
curl -sS -X POST localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"supersecret","fullName":"You"}'

# login (capture the token)
TOKEN=$(curl -sS -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"supersecret"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

# me (authorized)
curl -sS localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"

# me (unauthorized) -> 401
curl -sS -o /dev/null -w '%{http_code}\n' localhost:8080/api/auth/me
```
