# 09 — Error Handling System

Consistent, observable, recoverable errors across app and backend.

---

## 1. Unified error envelope (API)
Every non-2xx response:
```json
{
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Project not found.",
    "details": { "projectId": "uuid" },
    "traceId": "b3-abc123",
    "timestamp": "2026-05-30T10:00:00Z"
  }
}
```
- `code` — stable machine-readable enum (below).
- `message` — human-readable, safe to display.
- `details` — optional structured context (never secrets).
- `traceId` — correlate with backend logs/traces.

### Error code catalog (excerpt)
| Code | HTTP | Meaning |
|------|------|---------|
| `VALIDATION_FAILED` | 400 | DTO validation errors (with field list) |
| `UNAUTHORIZED` | 401 | Missing/invalid token |
| `TOKEN_EXPIRED` | 401 | Access token expired → client refreshes |
| `FORBIDDEN` | 403 | Not owner / insufficient role |
| `RESOURCE_NOT_FOUND` | 404 | — |
| `CONFLICT` | 409 | Sync/merge conflict (carries conflict data) |
| `RATE_LIMITED` | 429 | Includes `Retry-After` |
| `UPSTREAM_GITHUB` | 502 | GitHub API error (wrapped) |
| `UPSTREAM_AI` | 502 | AI provider error (wrapped, vendor msg sanitized) |
| `INTERNAL` | 500 | Unexpected; generic message, details suppressed |

## 2. Backend implementation (NestJS)
- **Global `AllExceptionsFilter`** converts thrown errors → envelope, attaches `traceId`,
  logs with pino (full stack server-side, sanitized client message).
- **Domain exceptions**: `AppException(code, httpStatus, message, details?)` subclasses.
- **Validation**: global `ValidationPipe` → `VALIDATION_FAILED` with field errors.
- **Upstream wrapping**: GitHub/AI calls wrapped in adapters that map vendor errors to
  `UPSTREAM_*`, with retry/backoff for transient 429/5xx.
- **Idempotency**: mutating sync endpoints dedupe by `Idempotency-Key`.

## 3. Android implementation
- **`Result<T>` / sealed `AppError`** in `:core:common`; no exceptions cross layers.
```kotlin
sealed interface AppError {
  data class Network(val cause: Throwable): AppError
  data class Http(val code: String, val message: String, val status: Int): AppError
  data class Auth(val expired: Boolean): AppError
  data class Git(val message: String): AppError
  data class Ssh(val message: String): AppError
  data class Ai(val provider: String, val message: String): AppError
  data class Unknown(val cause: Throwable): AppError
}
```
- **Mapping**: a single `errorMapper` turns HTTP envelopes, IO/JGit/SSHJ exceptions, and
  timeouts into `AppError`. ViewModels expose `UiState.error` + transient `Snackbar` events.
- **Auth recovery**: `TOKEN_EXPIRED` triggers an OkHttp `Authenticator` that refreshes
  once and replays the request; failure routes to login.
- **Offline**: network failures during edits are non-fatal — work is queued in
  `pending_change` and retried by WorkManager with exponential backoff.
- **User feedback**: inline banners for recoverable issues, blocking dialog only for
  destructive/unrecoverable ones, with **Retry** / **Report** actions.

## 4. Retry & resilience
- Exponential backoff with jitter for network, sync, AI streams, and WS reconnects.
- Circuit breaker per upstream (GitHub/AI) to fail fast when a provider is down.
- Terminal/WS auto-reconnect with session resume where possible.

## 5. Logging & diagnostics
- **App**: ring-buffer local logs (`log_entry`) viewable in-app (Build logs / Error logs
  screens); opt-in crash upload with `traceId` correlation.
- **Backend**: structured logs, request IDs, OpenTelemetry traces; PII/secret redaction
  interceptor scrubs tokens, keys, passwords.

## 6. Graceful degradation
| Failure | Behavior |
|---------|----------|
| Backend unreachable | Local editing/Git/terminal continue; cloud features disabled with banner |
| AI provider down | Suggest switching provider (incl. local Ollama) |
| Git push rejected | Show diff/conflict, offer pull-rebase or force-with-lease |
| SSH drop | Auto-reconnect, preserve terminal scrollback |
| Low memory | Shed inactive editor sessions, warn before opening huge files |
