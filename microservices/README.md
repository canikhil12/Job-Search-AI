# JobMatch AI — Microservices Platform

A microservices decomposition of the JobMatch monolith, demonstrating a Spring Cloud +
Kafka + Kubernetes architecture. **The monolith (`../backend`) remains the deployed product**
(Render/Vercel/Supabase); this platform is designed to run locally on Docker Compose / Kubernetes,
because 7 services + Kafka + Eureka don't fit a free cloud tier.

## Architecture
```
  React SPA ──► API Gateway (Spring Cloud Gateway, JWT)
                     │ service discovery via Eureka, config via Config Server
   ┌────────────┬────┴──────────┬──────────────┐
 auth-service  resume-service  job-service   ai-service
  authdb        resumedb        jobdb+pgvector  (stateless, Claude)
   └──────────── Kafka events (resume.uploaded → embed, …) ───────────┘
```

**Patterns:** service discovery (Eureka), centralized config (Spring Cloud Config), API gateway
(Spring Cloud Gateway), database-per-service, inter-service calls (WebClient + Eureka + Resilience4j),
event-driven async embedding (Kafka), Kubernetes deployment.

## Modules
| Module | Port | Role |
|--------|------|------|
| `config-server` | 8888 | Spring Cloud Config (native, serves `config/`) |
| `discovery-server` | 8761 | Eureka service registry |
| `api-gateway` | 8080 | Single entry point, routing, JWT (browser-facing) |
| `auth-service` | 8081 | users, register/login, JWT _(phase 2)_ |
| `resume-service` | 8082 | upload, parse, embed résumés _(phase 3)_ |
| `job-service` | 8083 | Adzuna search, pgvector match, ATS data _(phase 3)_ |
| `ai-service` | 8084 | Claude gap analysis / cover letter / tailor _(phase 3)_ |

## Run locally
```bash
cd microservices
docker compose up --build          # postgres/pgvector + kafka + config + eureka + gateway
```
- Eureka dashboard: http://localhost:8761
- Gateway: http://localhost:8080 (the SPA points `VITE_API_BASE_URL` here)
- Config: http://localhost:8888/<service>/default

## Build
```bash
mvn -q -B package        # builds all modules
```

## Status
- [x] **Phase 1** — foundation: parent POM, Config Server, Eureka, Gateway, compose (Postgres + Kafka)
- [ ] Phase 2 — auth-service behind the gateway
- [ ] Phase 3 — resume / job / ai services (ported domain logic, database-per-service)
- [ ] Phase 4 — Kafka async embedding pipeline
- [ ] Phase 5 — Kubernetes manifests (kind/minikube)
