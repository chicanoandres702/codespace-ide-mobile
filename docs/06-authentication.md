# 06 — Authentication & Security

Defense-in-depth: secure tokens, encrypted secrets, least-privilege OAuth, and a
hardened client.

---

## 1. Identity model
- **Email + password** (Argon2id) and/or **OAuth** (GitHub primary; Google optional).
- A user may link multiple OAuth accounts (`oauth_accounts`).

## 2. Token strategy
| Token | Lifetime | Storage | Notes |
|-------|----------|---------|-------|
| Access (JWT) | 15 min | Memory / Keystore-backed | `sub`, `email`, `jti`, `exp` |
| Refresh (opaque) | 30 days, rotating | Android Keystore (encrypted) | Hash stored server-side |

**Rotation:** every `/auth/refresh` issues a new refresh token and revokes the old one
(reuse detection → revoke whole family = breach response).

## 3. GitHub OAuth (device flow on mobile)
```
App → GET /auth/github            → returns {verificationUri, userCode, deviceCode}
User → opens URL, enters code
App  → polls POST /auth/github/callback {deviceCode}
Backend → exchanges for token, stores encrypted, links/creates user → app tokens
```
Scopes requested are minimal: `repo`, `read:user`, `codespace` (only when needed).

## 4. Secret handling
- **Server:** OAuth & AI keys encrypted at rest (AES-256-GCM; key from KMS or
  `ENCRYPTION_KEY`). Never logged; redacted in error envelopes.
- **Client:** access token in memory; refresh token + BYOK AI keys in **EncryptedFile /
  EncryptedSharedPreferences** backed by the **Android Keystore** (hardware-backed where
  available, StrongBox preferred). Biometric unlock optional via `BiometricPrompt`.

## 5. Transport & client hardening
- TLS 1.2+ only; **certificate pinning** (OkHttp `CertificatePinner`) for the API host.
- No secrets in logs; `cleartextTrafficPermitted=false` (except explicit user-added local
  Ollama/SSH hosts via per-host network security config).
- Root/debug detection warning (non-blocking) for sensitive actions.

## 6. Authorization
- Every resource scoped to `user_id`; service-layer ownership checks + row guards.
- `RolesGuard` for publisher/admin (plugin marketplace moderation).

## 7. Session & device management
- `devices` table tracks sessions; users can list/revoke devices.
- Refresh tokens carry `device_id`; logout revokes per device or globally.

## 8. Threat mitigations
| Threat | Mitigation |
|--------|------------|
| Token theft | Short access TTL, rotation, reuse detection, Keystore storage |
| Credential stuffing | Argon2id, rate limiting, optional 2FA (TOTP) |
| MITM | TLS + cert pinning |
| Secret leakage in logs | Field redaction interceptor |
| SSRF via SSH/Ollama URLs | Allowlist + outbound egress controls on backend proxy |
| Malicious plugins | Signed packages, capability sandbox, permission prompts |
| Brute force on terminal | Per-session resource caps + auth tickets |

## 9. Compliance posture
- GDPR-friendly: data export + account deletion endpoints; backups purged on delete.
- Minimal PII; clear consent for telemetry (opt-in).
