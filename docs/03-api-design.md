# 03 — API Design

The backend exposes a versioned REST API under `/api/v1` plus WebSocket channels for the
terminal and live sync. All responses are JSON; errors follow the envelope in
deliverable 09. Auth uses short-lived JWT access tokens + rotating refresh tokens.

Base URL: `https://api.codespace-ide.app/api/v1`

> Convention: `🔒` = requires `Authorization: Bearer <accessToken>`.

---

## 1. Auth

| Method | Path | Body | Notes |
|--------|------|------|-------|
| POST | `/auth/register` | `{email, password, displayName}` | Email/password signup |
| POST | `/auth/login` | `{email, password, deviceId}` | Returns access + refresh |
| POST | `/auth/refresh` | `{refreshToken}` | Rotates refresh token |
| POST | `/auth/logout` 🔒 | `{refreshToken}` | Revokes token |
| GET  | `/auth/github` | — | Starts GitHub OAuth (device flow URL) |
| POST | `/auth/github/callback` | `{code, state}` | Exchanges code, links account |
| GET  | `/auth/me` 🔒 | — | Current user profile |

**Login response**
```json
{
  "user": { "id": "uuid", "email": "a@b.com", "displayName": "Ada" },
  "accessToken": "eyJ...",
  "accessTokenExpiresIn": 900,
  "refreshToken": "rt_..."
}
```

---

## 2. Workspaces & projects

| Method | Path | Description |
|--------|------|-------------|
| GET    | `/workspaces` 🔒 | List workspaces |
| POST   | `/workspaces` 🔒 | Create `{name, settings}` |
| PATCH  | `/workspaces/:id` 🔒 | Update |
| DELETE | `/workspaces/:id` 🔒 | Delete |
| GET    | `/workspaces/:id/projects` 🔒 | List projects |
| POST   | `/workspaces/:id/projects` 🔒 | Create project |
| GET    | `/projects/:id` 🔒 | Project detail |
| PATCH  | `/projects/:id` 🔒 | Update (rename, remote config) |
| DELETE | `/projects/:id` 🔒 | Delete |
| GET    | `/projects/recent` 🔒 | Recently opened |

**Create project**
```json
POST /workspaces/{id}/projects
{
  "name": "my-api",
  "kind": "git",                       // git | ssh | codespace | docker | local
  "gitRemoteUrl": "https://github.com/me/my-api.git",
  "defaultBranch": "main",
  "remoteConfig": {}
}
```

---

## 3. GitHub & source control

Most Git ops happen **on-device with JGit**. The backend brokers GitHub API calls that
need the OAuth token (PRs, codespaces, repo listing).

| Method | Path | Description |
|--------|------|-------------|
| GET  | `/github/repos` 🔒 | List user repos (paginated) |
| GET  | `/github/repos/:owner/:repo/branches` 🔒 | Branches |
| POST | `/github/repos/:owner/:repo/pulls` 🔒 | Create PR `{title, head, base, body}` |
| GET  | `/github/repos/:owner/:repo/pulls` 🔒 | List PRs |
| POST | `/github/repos/:owner/:repo/merge` 🔒 | Merge `{base, head, commitMessage}` |
| GET  | `/github/codespaces` 🔒 | List Codespaces |
| POST | `/github/codespaces/:name/start` 🔒 | Start |
| POST | `/github/codespaces/:name/stop` 🔒 | Stop |

> Clone/pull/push/commit/branch/merge/diff are exposed as **client-side use cases**
> (see `android/.../git`) and don't require backend round-trips except for auth tokens.

---

## 4. Remote (SSH / Docker / SFTP)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/remote/ssh/test` 🔒 | Validate `{host, port, user, auth}` |
| GET  | `/remote/sftp/list` 🔒 | `?connId=&path=` directory listing |
| GET  | `/remote/sftp/read` 🔒 | Stream file |
| PUT  | `/remote/sftp/write` 🔒 | Upload file |
| GET  | `/remote/docker/containers` 🔒 | List containers on a host |
| POST | `/remote/docker/exec` 🔒 | Open exec session (returns WS ticket) |

> Direct SSH/SFTP can also run **client-side** via SSHJ when the phone has network
> reach to the host. The backend path is for hosts only reachable from the cloud.

---

## 5. AI

Supports BYOK (key in app) and proxy mode (key on backend). Streaming via SSE.

| Method | Path | Description |
|--------|------|-------------|
| GET  | `/ai/providers` 🔒 | Configured providers for user |
| PUT  | `/ai/keys/:provider` 🔒 | Set/clear `{apiKey, baseUrl}` |
| POST | `/ai/chat` 🔒 | `{provider, model, messages, context}` → SSE stream |
| POST | `/ai/action` 🔒 | `{action, code, language, context}` → SSE |
| POST | `/ai/embed` 🔒 | `{provider, input[]}` → vectors |

`action` ∈ `explain | generate | refactor | fix | document | test`.

**Chat request**
```json
POST /ai/chat
{
  "provider": "openai",
  "model": "gpt-4o-mini",
  "messages": [{ "role": "user", "content": "Why does this throw?" }],
  "context": {
    "projectId": "uuid",
    "files": ["src/index.ts"],
    "selection": { "file": "src/index.ts", "start": 10, "end": 24 }
  }
}
```
**Stream (SSE)**
```
event: token
data: {"delta":"The error occurs because "}

event: token
data: {"delta":"`user` is undefined..."}

event: done
data: {"usage":{"promptTokens":420,"completionTokens":88}}
```

---

## 6. Sync, backups, tasks, logs, snippets

| Method | Path | Description |
|--------|------|-------------|
| POST | `/sync/:projectId/push` 🔒 | Push local changes `{rev, changes[]}` |
| GET  | `/sync/:projectId/pull` 🔒 | `?sinceRev=` returns remote changes |
| POST | `/backups/:projectId` 🔒 | Trigger backup |
| GET  | `/backups/:projectId` 🔒 | List backups |
| POST | `/backups/restore/:backupId` 🔒 | Restore |
| GET/POST/PATCH/DELETE | `/tasks` 🔒 | Task CRUD |
| GET/POST | `/logs/:projectId` 🔒 | Build/error/terminal logs |
| GET/POST/DELETE | `/snippets` 🔒 | Snippet library |

---

## 7. Plugin marketplace

| Method | Path | Description |
|--------|------|-------------|
| GET  | `/plugins` | Search/list `?q=&category=&page=` |
| GET  | `/plugins/:slug` | Detail + versions |
| GET  | `/plugins/:slug/download/:version` | Signed package URL |
| POST | `/plugins` 🔒 | Publish (publishers) |

---

## 8. WebSocket channels

| Channel | Purpose | Messages |
|---------|---------|----------|
| `wss://.../ws/terminal?ticket=` | PTY session | `in`(stdin), `out`(stdout), `resize`, `exit` |
| `wss://.../ws/sync?projectId=` | Live sync events | `change`, `ack`, `conflict` |

**Terminal frames**
```json
// client → server
{ "type": "in", "data": "ls -la\n" }
{ "type": "resize", "cols": 120, "rows": 30 }
// server → client
{ "type": "out", "data": "total 12\n..." }
{ "type": "exit", "code": 0 }
```

`ticket` is a short-lived signed token from `POST /remote/docker/exec` or
`POST /terminal/session` to avoid passing JWTs in WS query strings.

---

## 9. Conventions
- **Pagination:** `?page=1&pageSize=30` → `{ items, page, pageSize, total }`.
- **Idempotency:** mutating sync endpoints accept `Idempotency-Key` header.
- **Versioning:** path-based (`/api/v1`); breaking changes bump the prefix.
- **Rate limits:** per-user token bucket; `429` with `Retry-After`.
- **OpenAPI:** auto-generated at `/api/docs` (Swagger) — see backend.
