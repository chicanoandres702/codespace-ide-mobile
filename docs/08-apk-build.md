# 08 — APK Build Configuration

This is the turnkey path to a **signed, production APK**. An APK is a compiled artifact —
you produce it with the Android toolchain (locally or in CI) from the `android/` source.

You asked to have your build options explained, so here are all three, easiest first.

---

> **First step on a fresh clone:** the Gradle wrapper JAR (`gradle/wrapper/gradle-wrapper.jar`)
> is a binary and isn't checked in here. Android Studio generates it automatically on
> first open. From the CLI, run once (with a system Gradle 8.7+):
> ```bash
> cd android && gradle wrapper --gradle-version 8.7
> ```
> After that, `./gradlew` works everywhere (and CI uses it).

## Prerequisites (one-time)
- **JDK 17** (Temurin recommended)
- **Android SDK** (via Android Studio Hedgehog+ or `cmdline-tools`)
  - SDK Platform 34, Build-Tools 34.x, Platform-Tools
- Accept licenses: `sdkmanager --licenses`

---

## Option A — Android Studio (recommended, easiest)
1. **Open** the `android/` folder in Android Studio.
2. Wait for **Gradle sync** (it downloads dependencies).
3. Create a release keystore when prompted, or via **Build ▸ Generate Signed Bundle/APK**.
4. Choose **APK**, select/create keystore, pick **release**, finish.
5. Output: `android/app/build/outputs/apk/release/app-release.apk`.

For the Play Store choose **Android App Bundle (AAB)** instead in the same dialog.

---

## Option B — Command line (CI-friendly, reproducible)

### 1. Generate a release keystore (once, keep it safe & backed up)
```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias codespace \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$KEYSTORE_PASSWORD" -keypass "$KEY_PASSWORD" \
  -dname "CN=CodeSpace IDE, OU=Mobile, O=CodeSpace, C=US"
```

### 2. Provide signing config (don't commit secrets)
Create `android/keystore.properties` (git-ignored):
```properties
storeFile=../release.keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=codespace
keyPassword=YOUR_KEY_PASSWORD
```
`app/build.gradle.kts` already reads this file if present.

### 3. Build
```bash
cd android
./gradlew clean
./gradlew assembleRelease     # → app/build/outputs/apk/release/app-release.apk
# Play Store bundle:
./gradlew bundleRelease       # → app/build/outputs/bundle/release/app-release.aab
```

### 4. Verify the signature
```bash
$ANDROID_HOME/build-tools/34.0.0/apksigner verify --verbose \
  app/build/outputs/apk/release/app-release.apk
```

---

## Option C — CI build (no local toolchain)
Push to GitHub; `.github/workflows/android-build.yml` builds and signs in the cloud and
uploads the APK as an artifact. Set these **repository secrets**:

| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | `base64 -w0 release.keystore` |
| `KEYSTORE_PASSWORD` | store password |
| `KEY_ALIAS` | `codespace` |
| `KEY_PASSWORD` | key password |

The workflow decodes the keystore, runs `./gradlew assembleRelease`, and publishes the
APK. Download it from the run's **Artifacts**, or it's attached to a tagged release.

---

## Build configuration highlights (`app/build.gradle.kts`)
- **applicationId** `com.codespace.ide`
- **minSdk 26 / target 34 / compile 34**
- **Compose** + Kotlin 1.9.x, `buildFeatures { compose = true; buildConfig = true }`
- **R8/ProGuard** full-mode minify + resource shrink for release → smaller, faster APK
- **Build flavors** `dev` / `staging` / `prod` with per-flavor `API_BASE_URL`
- **ABI splits** (`arm64-v8a`, `armeabi-v7a`, `x86_64`) to keep each APK small for
  3–8 GB devices; universal APK optional
- **Signing** read from `keystore.properties` or env (CI)
- **packagingOptions** to dedupe native libs (JGit/SSHJ)

## Output sizes (typical)
- Per-ABI release APK ≈ 18–28 MB (runtimes for Node/Python downloaded on first use).
- Universal APK ≈ 40–55 MB.

## Troubleshooting
| Symptom | Fix |
|---------|-----|
| `SDK location not found` | Set `ANDROID_HOME` or add `local.properties` with `sdk.dir=` |
| `Could not find keystore` | Check `keystore.properties` `storeFile` path |
| OOM during build | `org.gradle.jvmargs=-Xmx2g` in `gradle.properties` |
| Duplicate `META-INF` | already handled in `packagingOptions` |
