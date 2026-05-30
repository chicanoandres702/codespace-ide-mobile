# 07 — Deployment Instructions

Covers backend deployment and app distribution. (APK *build* specifics are in
deliverable 08.)

---

## 1. Backend — Docker Compose (single host / staging)
```bash
cd backend
cp .env.example .env          # set secrets
docker compose -f docker-compose.yml up -d --build
# services: api (NestJS), postgres, redis
docker compose exec api npm run migration:run
curl http://localhost:8080/health
```

`docker-compose.yml` provisions:
- `api` — the NestJS app (multi-stage Dockerfile, distroless runtime)
- `postgres:16`
- `redis:7`
- optional `ollama` for local AI models

## 2. Backend — Kubernetes (production)
- Container image pushed to your registry (`docker build -t registry/codespace-api .`).
- Apply manifests in `backend/deploy/k8s/` (Deployment, Service, HPA, Ingress, Secrets).
- **TLS:** cert-manager + Let's Encrypt; Ingress (NGINX) with WebSocket support enabled
  (`nginx.ingress.kubernetes.io/proxy-read-timeout: "3600"` for terminals).
- **Secrets:** use sealed-secrets or cloud secret manager; never commit `.env`.
- **DB:** managed Postgres (RDS/Cloud SQL) with automated backups + read replica.
- **Cache/queue:** managed Redis (ElastiCache/Memorystore).
- **Object storage:** S3/GCS bucket for project backups.
- **Autoscaling:** HPA on CPU + custom WS-connection metric.

Recommended deploy flow:
```bash
docker build -t $REG/codespace-api:$(git rev-parse --short HEAD) backend
docker push $REG/codespace-api:TAG
kubectl set image deploy/codespace-api api=$REG/codespace-api:TAG
kubectl rollout status deploy/codespace-api
```

## 3. Environments
| Env | API host | Notes |
|-----|----------|-------|
| dev | localhost:8080 | docker compose |
| staging | staging-api.codespace-ide.app | k8s namespace `staging` |
| prod | api.codespace-ide.app | k8s namespace `prod`, HPA, alerts |

The Android app's API base URL is a build flavor field (`dev`/`staging`/`prod`) — see
`android/app/build.gradle.kts` `buildConfigField`.

## 4. App distribution
- **Direct APK:** host the signed `app-release.apk` for sideloading (enable "install
  unknown apps").
- **Google Play:** upload the **AAB** (`./gradlew bundleRelease`) to Play Console;
  Play signs/optimizes per-device. Required for store listing.
- **Internal testing:** Play internal track or Firebase App Distribution.
- **OTA for sideload:** optional in-app updater checks a JSON manifest for new APK
  versions and prompts install.

## 5. Observability
- Backend: pino logs → Loki/ELK; metrics → Prometheus/Grafana; traces → OpenTelemetry.
- App: opt-in crash reporting (self-hosted Sentry compatible) + local error logs.
- Alerts: error-rate, p95 latency, WS disconnect spikes, DB saturation.

## 6. Rollback
- Backend: `kubectl rollout undo deploy/codespace-api`; DB migrations are
  forward-only with reversible down-migrations tested in staging.
- App: keep previous signed APK/AAB; Play supports staged rollout + halt.

## 7. Pre-launch checklist
- [ ] Secrets set, not in VCS  - [ ] Migrations applied  - [ ] TLS + cert pinning host matches
- [ ] OAuth callback URLs registered  - [ ] Rate limits tuned  - [ ] Backups verified (restore test)
- [ ] Health/ready probes green  - [ ] Crash reporting wired  - [ ] APK signed with release keystore
