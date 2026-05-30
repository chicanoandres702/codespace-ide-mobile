# 05 — Backend Services

**NestJS (TypeScript) + PostgreSQL + Redis.** Modular monolith that can be split into
microservices later (see deliverable 10). Concrete source in `backend/`.

---

## 1. Module map
```
AppModule
├── ConfigModule          env + validation
├── DatabaseModule        TypeORM (Postgres), migrations
├── AuthModule            JWT, refresh rotation, GitHub OAuth, guards
├── UsersModule           profile, devices
├── WorkspacesModule      workspaces + projects CRUD
├── GithubModule          repos, branches, PRs, codespaces (OAuth-brokered)
├── RemoteModule          SSH test, SFTP proxy, Docker exec tickets
├── TerminalGateway       WebSocket PTY (node-pty), session tickets
├── AiModule              provider adapters, streaming, key vault, usage metering
├── SyncModule            push/pull, conflict detection, sync WS gateway
├── BackupsModule         object-storage backups + restore
├── TasksModule / LogsModule / SnippetsModule
├── PluginsModule         marketplace registry, signed packages
└── CommonModule          filters, interceptors, pipes, rate limiting
```

## 2. Responsibilities per service

### AuthModule
- Argon2id password hashing; JWT access (15 min) + opaque refresh (rotating, hashed).
- GitHub OAuth (device + web flow); stores encrypted provider tokens.
- Guards: `JwtAuthGuard`, `RolesGuard`; `@CurrentUser()` decorator.

### GithubModule
- Wraps Octokit using the user's stored token. Endpoints for repos, branches, PR create/
  list, merge, and Codespaces lifecycle. Never returns raw tokens to clients.

### AiModule
- `AiProvider` interface with adapters: `OpenAiProvider`, `ClaudeProvider`,
  `GeminiProvider`, `DeepSeekProvider`, `OllamaProvider`.
- Streaming via SSE; normalizes each vendor's stream into `{delta}` + `{usage}` frames.
- **Key vault:** per-user keys encrypted at rest (AES-GCM via KMS/`pgcrypto`).
- **Metering:** writes `ai_usage` rows; enforces optional per-user soft quotas (off by
  default — no subscription gating).
- **Context builder:** assembles prompts from provided files/selection + retrieved chunks.

### TerminalGateway (WebSocket)
- Issues short-lived tickets, spawns `node-pty` for local sandboxes or proxies SSH PTY to
  remote hosts/containers. Streams stdout, accepts stdin & resize, cleans up on disconnect.
- Each session runs in a constrained cgroup/container with CPU/mem/time limits.

### SyncModule
- Optimistic, rev-based sync. Detects conflicts via monotonic `rev` + per-file checksums;
  returns conflicts for client-side 3-way merge. Live notifications over `ws/sync`.

### BackupsModule
- Snapshots project state to object storage (S3/GCS) with checksum + size; restore
  endpoint streams back. Scheduled via Bull queue.

## 3. Cross-cutting
- **Validation:** `class-validator` DTOs + global `ValidationPipe`.
- **Errors:** global exception filter → standard envelope (deliverable 09).
- **Logging:** pino structured logs + request IDs; sensitive fields redacted.
- **Rate limiting:** `@nestjs/throttler` backed by Redis.
- **Queues:** BullMQ (Redis) for backups, embeddings, webhook fan-out.
- **Docs:** Swagger/OpenAPI at `/api/docs`.
- **Health:** `/health` (liveness) and `/ready` (DB/Redis checks) for k8s.

## 4. Config (env)
See `backend/.env.example`. Key vars: `DATABASE_URL`, `REDIS_URL`, `JWT_SECRET`,
`JWT_REFRESH_SECRET`, `GITHUB_CLIENT_ID/SECRET`, `ENCRYPTION_KEY`, `S3_*`,
and optional default provider keys.

## 5. Local dev
```bash
cd backend
cp .env.example .env
docker compose up -d        # postgres + redis
npm install
npm run migration:run
npm run start:dev           # nest watch mode, http://localhost:8080
npm test                    # unit + e2e
```
