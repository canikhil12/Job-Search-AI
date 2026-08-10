# Deployment — Phase 1 (Supabase + Render + Vercel)

A live Phase 1 is **three** services:

```
  Browser
    │  (HTTPS, cross-origin → CORS)
    ▼
  Vercel  ──────────►  Render  ──────────►  Supabase
  (frontend: React)    (backend: Spring)    (Postgres 16 + pgvector)
   VITE_API_BASE_URL    Docker image         Session pooler, SSL
```

Deploy in this order: **Supabase → Render → Vercel → wire CORS back to Render**.

---

## 1. Supabase (database)

1. Create a project at [supabase.com](https://supabase.com) (or reuse an existing one). Save the **database password** you set — you can't see it again later (only reset it).
2. **Enable pgvector:** Dashboard → **Database → Extensions** → search `vector` → enable. (Our `V1__init.sql` also runs `CREATE EXTENSION IF NOT EXISTS vector`, which then no-ops. Enabling it in the dashboard first avoids any role-permission surprise during migration.)
3. **Get the SESSION pooler connection** (not the direct one): Dashboard → **Connect** → **Session pooler**. It looks like:
   ```
   postgresql://postgres.<PROJECT_REF>:[PASSWORD]@aws-0-<REGION>.pooler.supabase.com:5432/postgres
   ```
   > ⚠️ **Use the Session pooler (port 5432), not the Direct connection.** Supabase's direct connection (`db.<ref>.supabase.co`) is **IPv6-only** on the free tier, and Render is IPv4 — it can't reach it. The Session pooler is IPv4 and supports the prepared statements + advisory locks that Flyway and Hibernate need. (The *Transaction* pooler on port 6543 does **not** — don't use it here.)
4. Split that URL into the three env vars our app reads (Render, next step):
   | Env var | Value |
   |---------|-------|
   | `DATABASE_URL` | `jdbc:postgresql://aws-0-<REGION>.pooler.supabase.com:5432/postgres?sslmode=require` |
   | `DATABASE_USER` | `postgres.<PROJECT_REF>` |
   | `DATABASE_PASSWORD` | your database password |

   Note the `jdbc:` prefix and the added `?sslmode=require`.

---

## 2. Render (backend)

There's a committed [`render.yaml`](./render.yaml) blueprint.

1. Render dashboard → **New → Blueprint** → connect the GitHub repo `canikhil12/Job-Search-AI`. Render reads `render.yaml` and proposes the `jobmatch-backend` web service (Docker, free plan).
2. Fill in the env vars it prompts for (all marked `sync:false`, so nothing secret is in git):
   - `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` — from Supabase above.
   - `JWT_SECRET` — generate a real one (≥ 32 bytes) and paste it:
     ```bash
     openssl rand -base64 48
     ```
   - `CORS_ALLOWED_ORIGINS` — leave as a placeholder for now (e.g. `http://localhost:5173`); you'll set the real Vercel URL in step 4.
3. Deploy. Watch the logs for:
   - `Successfully applied 1 migration` (Flyway ran `V1__init.sql`), and
   - `Started JobMatchApplication`.
4. Verify:
   ```bash
   curl https://<your-service>.onrender.com/actuator/health      # -> {"status":"UP"}
   ```
   > Free tier spins down after ~15 min idle; the first request then takes ~50s (cold start). The `/actuator/health` path is your keep-warm ping target — hit it on a schedule (e.g. UptimeRobot, or a scheduled agent) if you want it always warm.

---

## 3. Vercel (frontend)

1. Vercel → **Add New → Project** → import the same GitHub repo.
2. **Root Directory: `frontend`** (important — the repo is a monorepo). Framework preset: **Vite** (auto-detected).
3. **Environment Variables** → add:
   | Key | Value |
   |-----|-------|
   | `VITE_API_BASE_URL` | `https://<your-service>.onrender.com` |
   (No trailing slash. This is what makes the SPA call Render instead of the dead `/api` on the Vercel domain.)
4. Deploy. Note the resulting URL, e.g. `https://job-search-ai.vercel.app`.

---

## 4. Wire CORS back to Render

Now the frontend origin is known, so let the backend accept it.

1. Render → `jobmatch-backend` → **Environment** → set:
   ```
   CORS_ALLOWED_ORIGINS = https://<your-app>.vercel.app
   ```
   (Comma-separate if you have multiple, e.g. a preview domain.)
2. Save → Render redeploys automatically.

> **Why this step exists (interview note):** in local dev the Vite proxy makes requests same-origin, so CORS never fires. In production the browser calls Render from the Vercel origin — a real cross-origin request — so `CorsConfig` must name that exact origin or the browser blocks the response. This is the one place the CORS config actually does work. See `DECISIONS.md`.

---

## 5. End-to-end verification

1. Open the Vercel URL → **Register** → you should land on `/dashboard` showing "Hello, {name}".
2. Reload the page — you stay logged in (token in localStorage), `/dashboard` re-fetches `/api/auth/me`.
3. In Supabase → **Table Editor → users**, confirm the row exists with a bcrypt `password_hash`.
4. DevTools → Network: the auth calls go to `https://<render-url>/api/auth/...` and return 200/201.

---

## Troubleshooting

| Symptom | Likely cause / fix |
|---------|--------------------|
| Backend fails at startup: `JWT_SECRET must be at least 32 bytes` | `JWT_SECRET` unset or too short. Regenerate with `openssl rand -base64 48`. |
| Backend can't connect / connection timeout | You used the **direct** connection (IPv6) instead of the **Session pooler**. Switch to the pooler host + `postgres.<ref>` user. |
| Flyway: `permission denied to create extension "vector"` | Enable the `vector` extension in the Supabase dashboard first (step 1.2); the migration then no-ops. |
| Login/register blocked in browser: CORS error | `CORS_ALLOWED_ORIGINS` doesn't exactly match the Vercel origin (scheme + host, no trailing slash). Update it on Render and redeploy. |
| Frontend calls hit the Vercel domain and 404 | `VITE_API_BASE_URL` not set (or set after build). Set it in Vercel and redeploy. |
| First request after idle is very slow | Render free-tier cold start (~50s). Expected; keep it warm by pinging `/actuator/health`. |
