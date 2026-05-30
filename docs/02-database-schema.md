# 02 — Database Schema

Two databases:

1. **Backend — PostgreSQL** (multi-device sync, auth, plugin registry, AI metering).
2. **On-device — SQLite via Room** (offline-first local state).

Both are versioned with migrations. SQL DDL below is the source of truth; the NestJS
TypeORM entities and Room entities mirror it.

---

## Part A — PostgreSQL (backend)

```sql
-- ─────────────────────────────────────────────────────────────
-- Extensions
-- ─────────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─────────────────────────────────────────────────────────────
-- Users & authentication
-- ─────────────────────────────────────────────────────────────
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           CITEXT UNIQUE NOT NULL,
    display_name    TEXT,
    avatar_url      TEXT,
    password_hash   TEXT,                         -- null when OAuth-only
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE oauth_accounts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider        TEXT NOT NULL,                -- 'github' | 'google' | ...
    provider_uid    TEXT NOT NULL,
    access_token    BYTEA NOT NULL,               -- encrypted (pgcrypto/KMS)
    refresh_token   BYTEA,
    scope           TEXT,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_uid)
);

CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      TEXT NOT NULL,                -- store hash, never the raw token
    device_id       TEXT,
    user_agent      TEXT,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_user ON refresh_tokens(user_id) WHERE revoked_at IS NULL;

-- ─────────────────────────────────────────────────────────────
-- Workspaces, projects, devices
-- ─────────────────────────────────────────────────────────────
CREATE TABLE workspaces (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            TEXT NOT NULL,
    settings        JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE projects (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name            TEXT NOT NULL,
    kind            TEXT NOT NULL,                -- 'git' | 'ssh' | 'codespace' | 'docker' | 'local'
    git_remote_url  TEXT,
    default_branch  TEXT,
    remote_config   JSONB NOT NULL DEFAULT '{}',  -- host, port, container id, etc.
    last_opened_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_projects_ws ON projects(workspace_id);

CREATE TABLE devices (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            TEXT,
    platform        TEXT,
    push_token      TEXT,
    last_seen_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─────────────────────────────────────────────────────────────
-- Sync, backups, bookmarks, tasks, logs
-- ─────────────────────────────────────────────────────────────
CREATE TABLE sync_state (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    device_id       UUID REFERENCES devices(id) ON DELETE SET NULL,
    rev             BIGINT NOT NULL DEFAULT 0,    -- monotonic vector for conflict detection
    cursor          JSONB NOT NULL DEFAULT '{}',
    last_synced_at  TIMESTAMPTZ,
    UNIQUE (project_id, device_id)
);

CREATE TABLE backups (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    storage_key     TEXT NOT NULL,                -- object storage path (S3/GCS)
    size_bytes      BIGINT NOT NULL,
    checksum        TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE bookmarks (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    file_path       TEXT NOT NULL,
    line            INT,
    note            TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tasks (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title           TEXT NOT NULL,
    body            TEXT,
    status          TEXT NOT NULL DEFAULT 'todo', -- 'todo'|'doing'|'done'
    priority        INT NOT NULL DEFAULT 0,
    due_at          TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE logs (
    id              BIGSERIAL PRIMARY KEY,
    project_id      UUID REFERENCES projects(id) ON DELETE CASCADE,
    kind            TEXT NOT NULL,                -- 'build' | 'error' | 'terminal'
    level           TEXT NOT NULL DEFAULT 'info',
    message         TEXT NOT NULL,
    meta            JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_logs_project_kind ON logs(project_id, kind, created_at DESC);

-- ─────────────────────────────────────────────────────────────
-- AI usage & snippets
-- ─────────────────────────────────────────────────────────────
CREATE TABLE ai_keys (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider        TEXT NOT NULL,                -- 'openai'|'claude'|'gemini'|'deepseek'|'ollama'
    api_key_enc     BYTEA,                        -- encrypted; null for ollama/local
    base_url        TEXT,                         -- for ollama / self-hosted
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, provider)
);

CREATE TABLE ai_usage (
    id              BIGSERIAL PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider        TEXT NOT NULL,
    model           TEXT NOT NULL,
    prompt_tokens   INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_usage_user ON ai_usage(user_id, created_at DESC);

CREATE TABLE snippets (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    language        TEXT NOT NULL,
    title           TEXT NOT NULL,
    prefix          TEXT,
    body            TEXT NOT NULL,
    is_public       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─────────────────────────────────────────────────────────────
-- Plugin marketplace
-- ─────────────────────────────────────────────────────────────
CREATE TABLE plugins (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    publisher_id    UUID REFERENCES users(id) ON DELETE SET NULL,
    slug            TEXT UNIQUE NOT NULL,
    name            TEXT NOT NULL,
    description     TEXT,
    category        TEXT,
    icon_url        TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE plugin_versions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plugin_id       UUID NOT NULL REFERENCES plugins(id) ON DELETE CASCADE,
    version         TEXT NOT NULL,
    manifest        JSONB NOT NULL,
    package_url     TEXT NOT NULL,
    signature       TEXT NOT NULL,                -- package signing for integrity
    downloads       BIGINT NOT NULL DEFAULT 0,
    published_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (plugin_id, version)
);
```

### Entity-relationship summary
```
users ─┬─< oauth_accounts
       ├─< refresh_tokens
       ├─< devices
       ├─< ai_keys / ai_usage
       ├─< snippets
       └─< workspaces ─< projects ─┬─< sync_state
                                   ├─< backups
                                   ├─< bookmarks
                                   ├─< tasks
                                   └─< logs
plugins ─< plugin_versions
```

---

## Part B — SQLite / Room (on-device)

```sql
-- Open editor tabs (survive process death)
CREATE TABLE tab (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id    TEXT NOT NULL,
    file_path     TEXT NOT NULL,
    is_active     INTEGER NOT NULL DEFAULT 0,
    cursor_offset INTEGER NOT NULL DEFAULT 0,
    scroll_y      INTEGER NOT NULL DEFAULT 0,
    is_dirty      INTEGER NOT NULL DEFAULT 0,
    pane          INTEGER NOT NULL DEFAULT 0      -- 0 = primary, 1 = split
);

-- Recent projects
CREATE TABLE recent_project (
    id            TEXT PRIMARY KEY,
    name          TEXT NOT NULL,
    kind          TEXT NOT NULL,
    path_or_url   TEXT NOT NULL,
    last_opened   INTEGER NOT NULL
);

-- Local cache of remote file metadata for offline tree
CREATE TABLE file_node (
    project_id    TEXT NOT NULL,
    path          TEXT NOT NULL,
    is_dir        INTEGER NOT NULL,
    size          INTEGER,
    mtime         INTEGER,
    etag          TEXT,
    PRIMARY KEY (project_id, path)
);

-- Offline edits queued for sync
CREATE TABLE pending_change (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id    TEXT NOT NULL,
    file_path     TEXT NOT NULL,
    op            TEXT NOT NULL,                  -- 'write'|'delete'|'rename'
    payload_path  TEXT,                           -- local blob path
    created_at    INTEGER NOT NULL
);

CREATE TABLE bookmark   (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id TEXT, file_path TEXT, line INTEGER, note TEXT);
CREATE TABLE task       (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id TEXT, title TEXT, status TEXT, priority INTEGER, due_at INTEGER);
CREATE TABLE log_entry  (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id TEXT, kind TEXT, level TEXT, message TEXT, created_at INTEGER);
CREATE TABLE snippet    (id INTEGER PRIMARY KEY AUTOINCREMENT, language TEXT, title TEXT, prefix TEXT, body TEXT);

-- Secure tokens are NOT stored here — they live in Android Keystore / EncryptedFile.

-- Local semantic index for "chat with project" (sqlite-vec)
CREATE TABLE doc_embedding (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id    TEXT NOT NULL,
    file_path     TEXT NOT NULL,
    chunk         TEXT NOT NULL,
    embedding     BLOB NOT NULL                   -- float32 vector
);
```

### Migration strategy
- **Backend:** TypeORM migrations in `backend/src/database/migrations`, run on deploy.
- **Android:** Room `AutoMigration` + manual `Migration` classes; schema JSON exported to
  `android/app/schemas/` for testing and diffing.
