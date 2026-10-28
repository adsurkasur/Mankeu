# Google Play Store Release & Deployment Guide

This document defines the release automation architecture, SemVer version management, signing configuration, and Google Play Console deployment pipeline for **Mankeu** (`com.mankeu`).

---

## 1. Overview & Architecture

The deployment pipeline is built with two decoupled layers:

1. **Local SemVer Helper (`scripts/bump-version.js` & `package.json`)**:
   - Automatically inspects and increments `versionName` (SemVer `MAJOR.MINOR.PATCH`) and `versionCode` (integer increment) in `app/build.gradle.kts` and `package.json`.
   - Stages the modified configuration files, generates a standardized release commit (`chore(release): vX.Y.Z (build N)`), and creates an annotated Git tag (`vX.Y.Z`).

2. **Automated CI/CD Workflow (`.github/workflows/deploy_play_store.yml`)**:
   - Triggered automatically on version tag pushes (`v*`) or manual `workflow_dispatch`.
   - Decodes Google Services configuration (with fallback stub) and release signing credentials before compilation.
   - Validates code with unit tests (`./gradlew testDebugUnitTest assembleDebug`).
   - Builds signed release Android App Bundle (`./gradlew bundleRelease`) and signed release APK (`./gradlew assembleRelease`).
   - Deploys `.aab` to Google Play Console on the **Internal Testing** track via `r0adkll/upload-google-play`.
   - Publishes a **GitHub Release** via `softprops/action-gh-release@v2` attaching both the production `.apk` and `.aab` packages with auto-generated release notes.

---

## 2. Local SemVer Version Bumping

Run one of the following commands from the repository root:

```bash
# For bug fixes and patch updates (e.g. 0.0.1 -> 0.0.2, versionCode: +1)
npm run release:patch

# For new features and backward-compatible improvements (e.g. 0.0.2 -> 0.1.0, versionCode: +1)
npm run release:minor

# For major breaking updates or milestone releases (e.g. 0.1.0 -> 1.0.0, versionCode: +1)
npm run release:major
```

### What happens automatically:
1. `versionName` and `versionCode` in `app/build.gradle.kts` are updated.
2. `version` in root `package.json` is updated in sync.
3. A Git commit is made: `chore(release): vX.Y.Z (build N)`.
4. An annotated Git tag is created: `vX.Y.Z`.
5. The terminal prompts the operator to push the tag:
   ```bash
   git push origin <branch> --tags
   ```

---

## 3. Release Keystore Generation & Fingerprint Setup

> [!WARNING]
> **Security Guardrail:** Never commit `.jks`, `.keystore`, or password files into Git. Ensure your keystore is backed up in a secure offline password manager.

### Step 1: Generate Release Keystore (if not already created)
Run the following command using Java `keytool` in a secure directory outside the repository:

```bash
keytool -genkey -v \
  -keystore arina-release.jks \
  -alias arina-release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "<YOUR_SECURE_KEYSTORE_PASSWORD>" \
  -keypass "<YOUR_SECURE_KEY_PASSWORD>" \
  -dname "CN=Arina Agri, OU=Mobile, O=Mankeu, L=Surakarta, ST=Central Java, C=ID"
```

### Step 2: Extract SHA-1 and SHA-256 Fingerprints
To ensure Google Sign-In and Google Drive API integration work on production builds:

```bash
keytool -list -v -keystore arina-release.jks -alias arina-release
```

Look for the `Certificate fingerprints:` output:
```text
Certificate fingerprints:
  SHA1:   XX:XX:XX:XX:XX:...
  SHA256: YY:YY:YY:YY:YY:...
```

### Step 3: Register Release SHA-1 on the Google Cloud OAuth client
Mankeu uses Google Sign-In directly (no Firebase, no `google-services.json`; spec #8).
1. Open [Google Cloud Console](https://console.cloud.google.com/) -> **APIs & Services** -> **Credentials**.
2. Open (or create) the **Android** OAuth client for package `com.mankeu`.
3. Paste the **SHA-1** from the release keystore (and the Play App Signing SHA-1 once the app is on Play).

---

## 4. Google Play Console & Google Cloud Service Account Setup

To enable GitHub Actions to upload App Bundles to Google Play for the account `adsurkasur.dev@gmail.com`:

### Step 1: Enable Google Play Android Developer API
1. Go to [Google Cloud Console](https://console.cloud.google.com/).
2. Select your Google Cloud Project linked to Google Play Console.
3. Navigate to **APIs & Services** -> **Library**.
4. Search for **Google Play Android Developer API** and click **Enable**.

### Step 2: Create Service Account in Google Play Console
1. Open [Google Play Console](https://play.google.com/console).
2. Navigate to **Settings** -> **Developer account** -> **API access**.
3. Under **Service accounts**, click **Link existing project** or **Create new service account**.
4. Click **Google Cloud Console** link to generate a service account:
   - Service account name: `github-actions-deployer`
   - Role: `Service Account User`
5. In Google Cloud Console, click on the newly created service account -> **Keys** tab -> **Add Key** -> **Create new key** -> Select **JSON** -> Download the JSON key file.

### Step 3: Grant Play Console Permissions
1. Return to **Google Play Console** -> **API access** (or **Users and permissions**).
2. Locate the service account email and click **Manage permissions**.
3. Under **App permissions**, grant access to `com.mankeu`.
4. Under **Account permissions**, ensure the following are enabled:
   - **Manage track releases** (Draft, Edit, and Release to Internal / Closed testing tracks).
   - **Upload release assets**.
5. Click **Save** / **Invite user**.

---

## 5. GitHub Repository Secrets Reference

Configure the following secrets in **GitHub Repository** -> **Settings** -> **Secrets and variables** -> **Actions**:

| Secret Name | Description | Example / How to Generate |
| :--- | :--- | :--- |
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded string of `arina-release.jks` | PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("arina-release.jks"))`<br>Linux/macOS: `base64 -w 0 arina-release.jks` |
| `KEYSTORE_PASSWORD` | Password for the release keystore | Plaintext password |
| `KEY_ALIAS` | Key alias inside the keystore | `arina-release` |
| `KEY_PASSWORD` | Password for the specific key alias | Plaintext password |
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | Entire plaintext content of Google Play API Service Account JSON | Copy & paste downloaded `.json` content directly |

---

## 6. Release Checklist & Step-by-Step Operator Runbook

When you are ready to ship a new version to internal testers:

1. **Verify local test suite:**
   ```bash
   .\gradlew.bat testDebugUnitTest
   ```
2. **Execute version bump:**
   ```bash
   npm run release:patch   # or release:minor / release:major
   ```
3. **Push commit and release tag:**
   ```bash
   git push origin main --tags
   ```
4. **Monitor deployment:**
   - Open GitHub repository -> **Actions** -> select the running **Deploy to Google Play Store** workflow.
   - The workflow will build the bundle, sign it with `RELEASE_KEYSTORE_BASE64`, and upload `app-release.aab` to Google Play Console.
5. **Verify on Google Play Console:**
   - Navigate to **Testing** -> **Internal testing** in Google Play Console.
   - Confirm the new release build number is available for internal testers.

---

## 7. Security Invariants (`AGENTS.md`)

- **Zero-Telemetry Invariant:** Release builds do not include third-party advertising, analytics tracking, or telemetry SDKs.
- **Client-Side Data Ownership:** Cloud backups remain AES-256-GCM encrypted client-side with user passphrases before upload.
- **Offline-First:** The app functions 100% locally with zero internet dependency or mandatory login requirement.
