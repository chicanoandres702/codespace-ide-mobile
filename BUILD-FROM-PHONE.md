# 📱 Build the APK Using Only Your Phone (No PC, No Android Studio)

**Why this exists:** Android Studio doesn't run on phones, and an APK must be *compiled*
before it can be installed. This guide makes **GitHub's free cloud servers** compile and
sign the APK for you. You only need a browser on your phone.

Total time: ~15 minutes (most of it waiting for the cloud build).

---

## What you need
- A free **GitHub account** (sign up at github.com — works fine in a phone browser)
- The project files (the `codespace-ide-mobile.zip` from this workspace)
- Your phone's browser (Chrome, etc.)

---

## Step 1 — Create a free GitHub account
1. Go to **https://github.com/signup**
2. Make an account (or sign in if you have one).

## Step 2 — Create an empty repository
1. Tap **+** (top right) ▸ **New repository**.
2. Name it `codespace-ide-mobile`.
3. Choose **Private** (or Public), leave everything else empty, tap **Create repository**.

## Step 3 — Upload the project files
1. On the new repo page, tap **uploading an existing file** (the link in the page text),
   or go to `Add file ▸ Upload files`.
2. Upload the **contents** of the unzipped `codespace-ide-mobile` folder.
   - On a phone the easiest way: use an app like **ZArchiver** (free) to unzip
     `codespace-ide-mobile.zip`, then upload the folders.
   - ⚠️ Upload the files *inside* the folder (so `android/`, `backend/`, `.github/`,
     `README.md` sit at the repo root), **not** the zip itself.
3. Scroll down, tap **Commit changes**.

> 💡 The important folder is **`.github/workflows/android-build.yml`** — that's the
> recipe that tells GitHub how to build your APK. Make sure it uploaded.

## Step 4 — Let GitHub build the APK
1. Open the **Actions** tab of your repo.
2. You'll see a workflow called **"Build Android APK"** start automatically (or tap it,
   then **Run workflow**).
3. Wait ~5–10 minutes. A green ✓ means success.

## Step 5 — Download your APK
1. Tap the finished (green ✓) workflow run.
2. Scroll to the **Artifacts** section at the bottom.
3. Tap **`codespace-ide-apk`** to download a `.zip`.
4. Unzip it (ZArchiver again) → inside is **`app-debug.apk`** (or `app-release.apk`).

## Step 6 — Install it
1. Tap the `.apk` file.
2. Android will ask to allow installing from unknown sources → **Allow / Settings ▸ enable**.
3. Tap **Install** ▸ **Open**. 🎉 The app is now on your phone.

---

## About signing (for a *release* build)
The debug APK above installs fine for personal use. For a **signed release** APK
(needed for the Play Store or sharing widely), add these repository **Secrets** under
`Settings ▸ Secrets and variables ▸ Actions`:

| Secret name | What it is |
|-------------|------------|
| `KEYSTORE_BASE64` | your keystore file, base64-encoded |
| `KEYSTORE_PASSWORD` | the keystore password |
| `KEY_ALIAS` | the key alias (e.g. `codespace`) |
| `KEY_PASSWORD` | the key password |

How to create a keystore is in **`docs/08-apk-build.md`**. Without these secrets the
workflow still produces a **debug APK**, which is perfect for testing on your own phone.

---

## Troubleshooting
| Problem | Fix |
|---------|-----|
| No "Build Android APK" in Actions | `.github/workflows/android-build.yml` wasn't uploaded to the repo root |
| Build fails on signing step | You're building release without secrets — use the debug APK, or add the secrets above |
| "App not installed" on phone | Uninstall any older version first; ensure enough storage |
| Can't unzip on phone | Install **ZArchiver** or **RAR** (free) from Play Store |

---

## TL;DR
1. GitHub account → 2. New repo → 3. Upload files → 4. Actions builds it →
5. Download APK from Artifacts → 6. Install. **All from your phone.**
