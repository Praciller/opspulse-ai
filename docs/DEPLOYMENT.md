# OpsPulse-AI — Deployment Guide

> Status: Draft v0.1
> Last updated: 2026-07-10
> Companion: [ADR-004](ADR/ADR-004-slim-free-tier-deployment.md), [ARCHITECTURE.md](ARCHITECTURE.md), [RISK_REGISTER.md](RISK_REGISTER.md)

## 1. Deployment Philosophy

- **Hosted slim**: $0/month recurring; free-tier only; no short-lived trials; portable.
- **BYOK AI**: AI cost belongs to the user; the hosted demo runs the rule-based brief when no key is set.
- **Local full stack**: optional Kafka/Redpanda + Prometheus + Grafana for development & observability showcase.
- **Verify before deploy**: free-tier platform status drifts; the §10 checklist must pass before each deploy.

## 2. Hosted Slim Topology

```mermaid
flowchart LR
    User[End User Browser]
    Pages[Cloudflare Pages - React static]
    Render[Render Free Web Service - Spring Boot Docker]
    Neon[(Neon Free Postgres)]
    Upstash[(Upstash Redis - optional)]
    AI[AI Provider - BYOK]
    KeepAlive[Keep-alive ping - cron-job.org / GitHub Actions]

    User --> Pages
    Pages -->|HTTPS /api| Render
    Render --> Neon
    Render -.optional.-> Upstash
    Render -->|BYOK key from env| AI
    KeepAlive -->|GET /actuator/health every 12 min| Render
```

## 3. Frontend Deployment — Cloudflare Pages

### 3.1 Build settings

| Setting | Value |
|---|---|
| Framework preset | Vite |
| Build command | `npm run build` |
| Build output directory | `dist` |
| Node version | 20 |
| Install command | `npm ci` |

### 3.2 Environment variables

| Var | Value | Notes |
|---|---|---|
| `VITE_API_BASE_URL` | `https://<render-service>.onrender.com/api` | backend base |
| `VITE_DEMO_MODE` | `true` | shows demo banner |

### 3.3 Deploy steps

1. Push frontend to GitHub repo (separate folder or monorepo path `frontend/`).
2. Cloudflare Dashboard → Pages → Create project → Connect to Git.
3. Set build settings + env vars above.
4. Deploy. Cloudflare assigns `<project>.pages.dev` URL.
5. (Optional) Add custom domain; Cloudflare provisions TLS.

### 3.4 Limits to remember (verified 2026-07-10)

- 500 builds/month, 1 concurrent build.
- 20,000 files/site, 25 MiB max file.
- 100 custom domains/project.

## 4. Backend Deployment — Render Free Web Service

### 4.1 Service settings

| Setting | Value |
|---|---|
| Type | Web Service |
| Environment | Docker |
| Region | closest to Neon region (e.g., `oregon` ↔ Neon AWS us-east-2; verify latency) |
| Instance type | Free |
| Health Check Path | `/actuator/health` |
| Health Check Grace Period | 60s (cold start tolerance) |
| Docker Command | (image default: `java -XX:MaxRAMPercentage=75 -XX:+UseSerialGC -jar /app/app.jar`) |

### 4.2 Environment variables (backend)

| Var | Required | Default | Notes |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | yes | `prod` | |
| `PORT` | yes | (set by Render) | Spring binds to `$PORT` |
| `NEON_DATABASE_URL` | yes | – | `jdbc:postgresql://...` (pooled) |
| `NEON_DATABASE_USERNAME` | yes | – | |
| `NEON_DATABASE_PASSWORD` | yes | – | |
| `JWT_SECRET` | yes | – | 32+ char random; rotate quarterly |
| `JWT_ACCESS_TOKEN_TTL_SECONDS` | no | `900` | |
| `JWT_REFRESH_TOKEN_TTL_SECONDS` | no | `604800` | |
| `AI_API_KEY` | no | – | BYOK; absent → rule-based brief |
| `AI_PROVIDER` | no | `openai` | openai/anthropic/ollama |
| `AI_MODEL` | no | `gpt-4o-mini` | |
| `AI_BASE_URL` | no | – | override for OpenAI-compatible |
| `AI_TIMEOUT_SECONDS` | no | `15` | |
| `UPSTASH_REDIS_URL` | no | – | optional; absent → Caffeine in-memory cache |
| `CORS_ALLOWED_ORIGINS` | yes | `https://<project>.pages.dev` | allowlist |
| `ALLOW_NEGATIVE_STOCK` | no | `false` | admin config |
| `OUTBOX_POLL_INTERVAL_MS` | no | `5000` | |
| `RISK_SCAN_CRON` | no | `0 0 7 * * *` | 7am daily |
| `BRIEF_CRON` | no | `0 5 7 * * *` | 7:05am daily |

### 4.3 Deploy steps

1. Push backend to GitHub repo (path `backend/`).
2. Render Dashboard → New → Web Service → Connect to Git.
3. Choose Docker environment; Render auto-detects `Dockerfile`.
4. Set env vars above.
5. Deploy. Render builds image and starts service.
6. Verify `/actuator/health` returns 200 within 90s.
7. Run Flyway migrations (see §5.4).

### 4.4 Limits to remember (verified 2026-07-10)

- 750 instance-hours/workspace/month (single service 24/7 = 744h, fits).
- Spins down after 15 min idle; cold start 30–90s.
- 5 GB outbound bandwidth/month.
- 500 build minutes/month.
- No persistent disk, no WebSockets, no private networking, no SSH.

## 5. Database Deployment — Neon Free Postgres

### 5.1 Provision

1. Sign up at https://neon.com (no credit card required).
2. Create project `opspulse-ai-prod`, choose region closest to Render.
3. Create `main` branch (default).
4. Copy **pooled** connection string (pgbouncer) for runtime; copy direct connection for Flyway migrations.

### 5.2 Connection strings

- Runtime (pooled): `postgresql://user:pass@ep-...-pooler.region.aws.neon.tech/opspulse?sslmode=require`
- Migrations (direct): `postgresql://user:pass@ep-...-region.aws.neon.tech/opspulse?sslmode=require`

### 5.3 Limits to remember (verified 2026-07-10)

- 100 projects, 10 branches/project.
- 100 CU-hours/project/month; autoscale up to 2 CU (~8GB RAM).
- Scale to zero after 5 min idle (mandatory); first query after idle pays ~hundreds of ms.
- 0.5 GB storage/project (5 GB aggregate across 10 projects).
- 5 GB egress/project/month.
- 104 max connections (use pooled string for runtime).

### 5.4 Apply Flyway migrations

Render release stage command (runs before each deploy):
```bash
./gradlew flywayMigrate -Dflyway.url=$NEON_FLYWAY_URL -Dflyway.user=$NEON_DATABASE_USERNAME -Dflyway.password=$NEON_DATABASE_PASSWORD
```
Alternatively, Spring Boot runs Flyway on startup (`spring.flyway.enabled=true`); for first deploy, prefer release stage to fail fast on migration errors.

### 5.5 Demo data seeding

- `prod` profile does NOT seed demo data (`R__seed_demo_data.sql` gated by `local`/`test` profiles).
- For hosted demo, run a one-off `./gradlew seedDemo -Dprofile=prod` task (Phase 7) that inserts the demo dataset described in [DATA_MODEL.md §10.3](DATA_MODEL.md).

## 6. Optional Cache — Upstash Redis Free

### 6.1 When to use

- Dashboard aggregate caching (5s TTL) to reduce Neon load.
- Risk scan result caching (60s TTL).
- Skip on hosted demo unless dashboard latency exceeds 1.5s warm.

### 6.2 Provision

1. Sign up at https://upstash.com (no credit card).
2. Create Redis DB, single region, TLS endpoint.
3. Copy `UPSTASH_REDIS_URL` to Render env.
4. Spring Boot auto-configures `RedisCacheManager` when `UPSTASH_REDIS_URL` is set.

### 6.3 Limits to remember (verified 2026-07-10)

- 500,000 commands/month (changed from 10K/day in Mar 2025).
- 256 MB data, 10 GB bandwidth/month.
- Single region.

## 7. Environment Variables Master Table

See §4.2 (backend), §3.2 (frontend), §5.2 (database). Secrets (`JWT_SECRET`, `NEON_DATABASE_PASSWORD`, `AI_API_KEY`) live only in Render/Cloudflare env-var UI — never in Git.

## 8. Docker Build

### 8.1 Dockerfile (multi-stage, slim)

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew bootJar -x test

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:MaxDirectMemorySize=32m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

### 8.2 Build locally

```bash
docker build -t opspulse-ai/backend:latest backend/
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod -e NEON_DATABASE_URL=... opspulse-ai/backend:latest
```

### 8.3 Image size target

- Runtime image < 250 MB (JRE 21 + slim jar).

## 9. Health Check & Readiness

- `/actuator/health` exposes `livenessState` and `readinessState` (Spring Boot 3).
- Render health check: `GET /actuator/health` → 200.
- Cold start grace: 60s (Render) + 30s buffer for Spring Boot boot (~20–25s target).
- Neon wake: first query after idle ~hundreds of ms; frontend shows loading state.

## 10. Platform Validation Checklist

> Run before every deploy and quarterly. Every recorded status requires periodic manual verification because pricing is external and unstable. If a platform's free plan cannot be confirmed from its official page, mark **"requires manual verification"** and pause deploy until resolved.

| Platform | Free plan URL | Last verified | Hard limits (verified) | Status |
|---|---|---|---|---|
| Cloudflare Pages | https://developers.cloudflare.com/pages/platform/limits/ | 2026-07-10 | 500 builds/mo, 20K files, 25 MiB file, unlimited bandwidth/sites | Verified |
| Render Free Web Service | https://render.com/docs/free | 2026-07-10 | 750h/workspace/mo, 15 min idle spin-down, 5 GB egress, no disk/WebSockets | Verified |
| Neon Free Postgres | https://neon.com/faqs/free-plan-limits-and-quotas | 2026-07-10 | 100 CU-h/project/mo, 0.5 GB storage, 5 GB egress, 5-min scale-to-zero | Verified |
| Upstash Redis Free | https://upstash.com/pricing/redis | 2026-07-10 | 500K commands/mo, 256 MB data, 10 GB bandwidth, single region | Verified (optional) |

If any row becomes "requires manual verification", follow [ADR-004 alternatives](ADR/ADR-004-slim-free-tier-deployment.md) and update this table.

## 11. Local Minimal Dev (`docker-compose.yml`, profile `local`)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: opspulse
      POSTGRES_USER: opspulse
      POSTGRES_PASSWORD: opspulse
    ports: ["5432:5432"]
    volumes: ["pgdata:/var/lib/postgresql/data"]
  backend:
    build: ./backend
    environment:
      SPRING_PROFILES_ACTIVE: local
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/opspulse
    depends_on: [postgres]
    ports: ["8080:8080"]
volumes:
  pgdata:
```

## 12. Local Full Stack (`docker-compose.full.yml`, profile `fullstack`)

```yaml
services:
  postgres: { ... same as local ... }
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
  redpanda:
    image: docker.vectorized.io/vectorized/redpanda:latest
    command: redpanda start --overprovisioned --smp 1 --memory 1G
    ports: ["9092:9092", "9644:9644"]
  prometheus:
    image: prom/prometheus:latest
    volumes: ["./infra/prometheus.yml:/etc/prometheus/prometheus.yml"]
    ports: ["9090:9090"]
  grafana:
    image: grafana/grafana:latest
    environment: { GF_AUTH_ANONYMOUS_ENABLED: "true" }
    ports: ["3001:3000"]
  backend:
    build: ./backend
    environment:
      SPRING_PROFILES_ACTIVE: fullstack
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/opspulse
      SPRING_DATA_REDIS_HOST: redis
      KAFKA_BOOTSTRAP_SERVERS: redpanda:9092
    depends_on: [postgres, redis, redpanda]
    ports: ["8080:8080"]
```

Prometheus scrape config (`infra/prometheus.yml`):
```yaml
scrape_configs:
  - job_name: opspulse
    metrics_path: /actuator/prometheus
    static_configs: [{ targets: ["backend:8080"] }]
```

Grafana dashboards: import JSON for JVM, risk generation count, outbox lag, AI latency (Phase 7 deliverable).

## 13. Deploy Pipeline Flow

```mermaid
flowchart TB
    Push[Push to main]
    CI[GitHub Actions - build + test]
    PagesDeploy[Cloudflare Pages build + deploy]
    RenderBuild[Render build image]
    Release[Render release stage - Flyway migrate]
    Deploy[Render deploy new image]
    Smoke[Smoke test - /actuator/health + login + dashboard]
    Live[Live]

    Push --> CI
    CI -->|green| PagesDeploy
    CI -->|green| RenderBuild
    RenderBuild --> Release
    Release -->|success| Deploy
    Deploy --> Smoke
    Smoke -->|pass| Live
    Smoke -->|fail| Rollback[Render auto-rollback to previous image]
```

## 14. Rollback & Disable Path

- **Render**: automatic rollback to previous deploy if health check fails for 60s; manual rollback via dashboard.
- **Cloudflare Pages**: previous deployment always retained; promote via dashboard.
- **Neon**: point-in-time restore up to 6 hours (Free plan); branch from history for safer rollback.
- **Disable hosted demo**: set Render service to suspended; Cloudflare Pages project can remain (free, no cost).
- **Data purge**: `DELETE FROM import_jobs; DELETE FROM audit_logs WHERE created_at < now() - interval '90 days';` (manual; document in README).

## 15. Security Checklist

- [ ] No secrets in Git (`git-secrets` pre-commit hook).
- [ ] `JWT_SECRET` is 32+ char random; rotated quarterly.
- [ ] `AI_API_KEY` only in Render env; never in frontend.
- [ ] CORS allowlist set to Cloudflare Pages URL only.
- [ ] HTTPS enforced by Cloudflare + Render (TLS auto).
- [ ] `/actuator/prometheus` not public (admin token or Render-internal).
- [ ] Demo credentials documented; no real user data.
- [ ] Flyway migrations reviewed for destructive DDL.
- [ ] Dependency scan (Dependabot / OWASP Dependency-Check) in CI.

## 16. Free-Tier Limitations & Mitigations (summary)

| Limitation | Mitigation |
|---|---|
| Render cold start | Keep-alive ping every 12 min |
| Render 750h cap | Single service; 744h fits |
| Render no WebSockets | Outbox polling; SSE deferred |
| Neon 0.5 GB storage | Cleanup jobs; demo dataset ~100 MB |
| Neon 5-min idle wake | Frontend loading state; retry on first query |
| Upstash 500K cmd/mo | Skip Redis unless needed; Caffeine fallback |
| No persistent disk on Render | All state in Neon |
| Single instance (no HA) | Accepted for demo; durability in Neon |

## 17. References

- [ADR-004 — Slim Free-Tier Deployment](ADR/ADR-004-slim-free-tier-deployment.md)
- [ARCHITECTURE.md §2 — Hosted slim](ARCHITECTURE.md#2-hosted-slim-architecture)
- [ARCHITECTURE.md §3 — Local full stack](ARCHITECTURE.md#3-local-full-stack-architecture)
- [RISK_REGISTER.md R-01, R-02, R-03, R-11](RISK_REGISTER.md)
- Render Free docs: https://render.com/docs/free
- Neon Free FAQ: https://neon.com/faqs/free-plan-limits-and-quotas
- Cloudflare Pages limits: https://developers.cloudflare.com/pages/platform/limits/
- Upstash Redis pricing: https://upstash.com/pricing/redis
