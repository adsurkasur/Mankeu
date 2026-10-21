# Progress Log & Implementation Audit

> **Dokumen riwayat.** Log kemajuan 2026-08-29. Pencocok semantik dan fitur kredit yang dibahas di sini sudah dihapus (DEC-011, DEC-012). Keadaan sekarang (Room schema 11): `README.md`, `docs/DOMAIN_MODEL.md`, `docs/V1_IMPLEMENTATION_SPEC.md`, `docs/diagrams/`, dan keputusan di `logs/DECISION_LOG.md`.

**Timestamp:** 2026-08-29 06:45:00 +07:00  
**Status:** All tasks and refinements implemented, verified with 38 unit tests (100% pass), built with `assembleDebug`, deployed, and validated on Android Emulator via ADB.

---

## 1. Executive Summary & Completed Milestones

| Component | Status | Details |
| :--- | :--- | :--- |
| **Component 1: Semantic Matcher Hardening** | `VERIFIED` | Pure Kotlin implementation of Levenshtein Distance, Jaro-Winkler string similarity, soft token overlap, and typo-tolerant domain alias resolution. Zero external ML dependencies. |
| **Component 2: Guided Tour Expansion & Fluidity** | `VERIFIED` | 6-step comprehensive onboarding walkthrough covering Farm & Field scope, Projection vs Realization isolation, Cash balance & health, Expense distribution breakdown, Quick transaction logging, and Feasibility reports (BEP/HPP/BCR). |
| **Component 3: Motion & Animation Compliance** | `VERIFIED` | Continuous vertical translation `targetCardY` replaces discrete alignment jumps. Fluid `FastOutSlowInEasing` gliding from step to step, plus graceful spawn (fade/scale in) and despawn (fade/scale out) lifecycle transitions governed by `LocalMotion.current`. |
| **Component 4: Context-Aware Import Workflow** | `VERIFIED` | Import dialog (`ImportCard`) dynamically detects and defaults to the active screen book mode (*Proyeksi* vs *Realisasi*), offers a visible `SingleChoiceSegmentedButtonRow` for user overrides, and routes rows directly to `viewModel.commitImport(target)`. |
| **Component 5: Package & App Title Refactoring** | `VERIFIED` | Migrated package namespace to `com.arinahub.arinaagri.calculator` across all 42 Kotlin source/test files, Gradle `namespace` and `applicationId`, and Room schemas. Updated app display titles to `"Arina Agri - Calculator"` and launcher label to `"Arina Agri Calculator"`. Resolved O-001 and O-002 in `DECISION_LOG.md`. |
| **Component 6: UI Redundancy Audit & Report** | `AUDITED` | Comprehensive redundancy report completed below. Guardrail strictly respected: layout and tiles on `HomeScreen.kt` and `ReportsScreen.kt` were left intact. |
| **Component 7: Local & On-Device Verification** | `PASSED` | 38/38 unit tests passing (`clean testDebugUnitTest`). Debug APK installed on `emulator-5554` with zero logcat crash loops; verified package launch and updated display titles. |

---

## 2. Component Implementation Details

### Component 1: Lightweight Pure-Kotlin Semantic Matcher
- **Files Modified:**
  - `app/src/main/java/com/example/arinaagri/calculator/importer/SemanticMatcher.kt`
  - `app/src/main/java/com/example/arinaagri/calculator/importer/CategoryClassifier.kt`
  - `app/src/test/java/com/example/arinaagri/calculator/importer/SemanticMatcherTest.kt`
- **Algorithms Implemented:**
  1. `levenshteinDistance(s1, s2)`: Memory-optimized two-row dynamic programming for edit distance.
  2. `normalizedLevenshtein(s1, s2)`: $1.0 - \frac{\text{dist}}{\max(|s1|, |s2|)}$.
  3. `jaroSimilarity(s1, s2)` & `jaroWinklerSimilarity(s1, s2, prefixScale = 0.1)`: Character matching within sliding window $\lfloor \max(|s1|,|s2|)/2 \rfloor - 1$ with transposition tracking and common prefix boost.
  4. `tokenSimilarity(t1, t2)`: Blended metric combining Normalized Levenshtein (40%) and Jaro-Winkler (60%).
  5. `similarity(s1, s2)`: Two-tier token intersection:
     - Exact token intersection (weight = 1.0)
     - Soft token matching via `tokenSimilarity >= 0.82` for typos (weight = similarity score)
     - Composite Score = $0.55 \times \text{Jaccard} + 0.45 \times \text{Containment}$.
  6. Domain Synonym Resolution: Typo-tolerant alias mapping in `tokens()` resolving misspelled agricultural keywords (`"trnsplanter"` -> `"mesin"`, `"pupk"` -> `"pupuk"`).
  7. Two-Pass Category Classifier: First pass checks exact priority keywords (labour first); second pass checks typo-tolerant fuzzy keywords; third pass checks category name similarity.

### Component 2 & 3: Guided Tour Spatial Interpolation & Lifecycle Transitions
- **Files Modified:**
  - `app/src/main/java/com/example/arinaagri/calculator/presentation/components/SpotlightTour.kt`
  - `app/src/main/java/com/example/arinaagri/calculator/presentation/ArinaCalculatorApp.kt`
  - `app/src/main/java/com/example/arinaagri/calculator/presentation/screens/HomeScreen.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-en/strings.xml`
- **Key Enhancements:**
  1. **Continuous Vertical Translation (`targetCardY`):** Eliminated the discrete `placeBelow`/`placeAbove` alignment switch that caused jumping between Step 2 (top) and Step 3 (bottom). The card now measures its own height via `onGloballyPositioned` and glides smoothly across the screen via `animateFloatAsState` with `FastOutSlowInEasing`.
  2. **Coordinated Cutout Hole Morphing:** The spotlight cutout coordinates (`left`, `top`, `right`, `bottom`) animate smoothly via `FastOutSlowInEasing`, accompanied by dynamic pulse stroke and corner radius smoothing.
  3. **Spawn & Despawn Lifecycle:**
     - **Spawn (Enter):** Scrim smoothly fades in (`0.0f` -> `0.72f`) and the tooltip card scales/fades in (`0.92f` -> `1.0f`).
     - **Despawn (Exit):** When tapping "Lewati" or "Mulai Gunakan", `isDismissing = true` initiates a smooth fade and scale out (`1.0f` -> `0.92f`, scrim `0.72f` -> `0.0f`) with a coroutine delay matching `motion.mediumMillis` before unmounting.
     - **Accessibility:** If `motion.enabled == false` or system animation scale is `0.0f`, durations collapse to 0ms for instant unmounting.

### Component 4: Context-Aware Import Workflow Verification
- **Audit Findings:**
  - In `MoreScreen.kt:ImportCard`, `target` is initialized as `var target by rememberSaveable(currentBookMode) { mutableStateOf(currentBookMode) }`.
  - When the user opens the Import screen from *Proyeksi* (Plan), the import target defaults to *Proyeksi*.
  - When the user opens the Import screen from *Realisasi* (Actual), the import target defaults to *Realisasi*.
  - For files that do not hardcode a destination book mode, a prominent `SingleChoiceSegmentedButtonRow` allows switching between `Rencana` and `Aktual` before committing.
  - Tapping commit routes `target` directly into `viewModel.commitImport(target) -> repository.commitImport(businessId, projectId, preview, destination)`.

---

## 3. UI/Data Redundancy Audit Report

As mandated by project guardrails, no UI components were removed. This section documents the audit findings and recommended future enhancements:

### A. Beranda (Home Screen) Redundancy Analysis
In an initial or early-stage planting scenario where only initial expenses (e.g., `Rp22.159.000`) have been logged with no revenue or capital injected:
1. **HeroCard Balance Tile:** Displays `-Rp22.159.000` (Negative cash balance).
2. **SimpleMetricGrid Net Profit Tile:** Displays `-Rp22.159.000` (Revenue Rp0 - Expense Rp22.159.000).
3. **SimpleMetricGrid Operating Cash Flow Tile:** Displays `-Rp22.159.000` (Operating cash outflow).
4. **ExpenseDistributionCard Total Header:** Displays `Rp22.159.000` (Sum of recorded expense categories).

**Semantic Rationale:**
- Each tile represents a distinct accounting concept:
  - *Cash Balance* tracks liquidity in the cash box / bank account.
  - *Net Profit* reflects accrual profitability (Revenue minus Operating Expenses).
  - *Operating Cash Flow* isolates core farming cash movements from financing and investing.
  - *Total Expense* aggregates categorized operational outlays.
- In pure cash-expense scenarios without credit, loans, or initial capital, these three equations produce identical arithmetic totals:
  $$\text{Cash Balance} = 0 - 22.159.000 = -22.159.000$$
  $$\text{Net Profit} = 0 - 22.159.000 = -22.159.000$$
  $$\text{Cash Flow} = 0 - 22.159.000 = -22.159.000$$
- When owner equity (Modal Awal) or credit purchases are logged, these metrics immediately diverge and provide high-value differentiated insights.

**Recommendations for Future Enhancements:**
- Add explanatory subtitle chips or tooltips to each metric tile on Home (e.g., *"Kas Riil"*, *"Pendapatan - Biaya"*, *"Arus Kas Berjalan"*).
- When cash balance is negative due to unrecorded initial capital, surface an informational helper banner inviting the user to record *"Modal Awal / Suntikan Kas"*.

### Component 5: Firebase & Google Services Integration
- **Gradle Plugins & Dependencies:**
  - Google Services Gradle plugin `com.google.gms.google-services:4.4.2` registered in root and applied in `app/build.gradle.kts`.
  - Firebase BoM `com.google.firebase:firebase-bom:33.10.0`, `firebase-auth`, and `com.google.android.gms:play-services-auth:21.3.0` added.
  - Zero analytics or telemetry dependencies per privacy mandate (O-011).
  - Verified `app/google-services.json` parsing by `:app:processDebugGoogleServices`.

---

## 3. UI/Data Redundancy Audit Report

As mandated by project guardrails, no UI components were removed. This section documents the audit findings and recommended future enhancements:

### A. Beranda (Home Screen) Redundancy Analysis
In an initial or early-stage planting scenario where only initial expenses (e.g., `Rp22.159.000`) have been logged with no revenue or capital injected:
1. **HeroCard Balance Tile:** Displays `-Rp22.159.000` (Negative cash balance).
2. **SimpleMetricGrid Net Profit Tile:** Displays `-Rp22.159.000` (Revenue Rp0 - Expense Rp22.159.000).
3. **SimpleMetricGrid Operating Cash Flow Tile:** Displays `-Rp22.159.000` (Operating cash outflow).
4. **ExpenseDistributionCard Total Header:** Displays `Rp22.159.000` (Sum of recorded expense categories).

**Semantic Rationale:**
- Each tile represents a distinct accounting concept:
  - *Cash Balance* tracks liquidity in the cash box / bank account.
  - *Net Profit* reflects accrual profitability (Revenue minus Operating Expenses).
  - *Operating Cash Flow* isolates core farming cash movements from financing and investing.
  - *Total Expense* aggregates categorized operational outlays.
- In pure cash-expense scenarios without credit, loans, or initial capital, these three equations produce identical arithmetic totals:
  $$\text{Cash Balance} = 0 - 22.159.000 = -22.159.000$$
  $$\text{Net Profit} = 0 - 22.159.000 = -22.159.000$$
  $$\text{Cash Flow} = 0 - 22.159.000 = -22.159.000$$
- When owner equity (Modal Awal) or credit purchases are logged, these metrics immediately diverge and provide high-value differentiated insights.

**Recommendations for Future Enhancements:**
- Add explanatory subtitle chips or tooltips to each metric tile on Home (e.g., *"Kas Riil"*, *"Pendapatan - Biaya"*, *"Arus Kas Berjalan"*).
- When cash balance is negative due to unrecorded initial capital, surface an informational helper banner inviting the user to record *"Modal Awal / Suntikan Kas"*.

### B. Laporan (Reports Screen) Analysis
- Feasibility metrics (BEP rupiah vs BEP volume, HPP per unit, B/C Ratio) and Income Statement subtotals are cleanly partitioned into collapsible cards and tabs, avoiding unnecessary layout duplicates.

---

## 4. Test & Quality Gate Evidence

### Unit Tests
Executed `.\gradlew.bat testDebugUnitTest`:
- **Total Tests:** 38
- **Passed:** 38 (100%)
- **Failed:** 0
- **Key Test Suites Passed:**
  - `SemanticMatcherTest` (Typo tolerance, Jaro-Winkler, Levenshtein, category classification)
  - `GoldenWorkbookImportTest` (Deterministic 5-sheet workbook import, reconciliation, and provenance)
  - `GoldenWorkbookEndToEndTest` (Full import-to-report round trip)
  - `ImportCategoryClassificationTest` (Role & keyword mappings)
  - `ReportCalculatorTest` (Financial ratios, BEP, B/C ratio)
  - `EncryptedBackupTest` (AES-256-GCM backup & restore)

### APK Build & ADB Deployment
- **Commands:**
  - `.\gradlew.bat clean assembleDebug` -> `BUILD SUCCESSFUL in 1m 53s` (with `:app:processDebugGoogleServices`)
  - `.\gradlew.bat testDebugUnitTest` -> `BUILD SUCCESSFUL in 9s` (38/38 passing)
- **Installation:** `adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk` -> `Success`
- **Launch:** `adb -s emulator-5554 shell am start -n com.arinahub.arinaagri.calculator/.MainActivity` -> `Success`
- **Visual Validation Evidence:**
  - `package_refactor_screen_home.png` (App header displaying "Arina Agri - Calculator")
  - `firebase_init_home.png` (Verified running app after Google Services & Firebase SDK initialization)

---

## [2026-08-29] Firebase Authentication & Google Drive Encrypted Backup (`drive.appdata`) Implementation

### Architectural Summary & Core Changes
1. **Authentication Layer (`auth` module)**:
   - `AuthModels.kt`: Defined `AuthUser(uid, email, displayName, photoUrl)`.
   - `FirebaseAuthManager.kt`: Implemented `AuthService` interface with `FirebaseAuthManager` using Firebase Auth & Google Play Services Auth API (`signInWithCredential`, `signInWithGoogleIdToken`, `signOut`, `setCustomUser`).
2. **Pure Kotlin Google Drive REST Client (`backup.cloud` module)**:
   - `GoogleDriveBackupClient.kt`: Implemented `CloudBackupClient` communicating with Google Drive v3 REST API (`drive.appdata` scope) via `HttpURLConnection`.
   - Client-side encryption invariant (`D-033`): Plaintext SQLite data never leaves the device. Data is encrypted into AES-256-GCM `.aabk` binary payload with user's passphrase before upload.
   - Multipart upload to `https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart` storing SHA-256 integrity hash in `appProperties`.
   - Automated pruning keeping top 5 backup snapshots.
   - Custom pure Kotlin JSON parser in companion object (`parseBackupListJson`, `parseSingleFilePure`) ensuring zero Android runtime stub dependencies and 100% JVM testability.
3. **Presentation & View Model Integration**:
   - `AppViewModel.kt`: Exposed `authUser`, `driveBackups`, `isDriveLoading` state flows and async handlers: `signInWithGoogleIdToken()`, `signOut()`, `fetchDriveBackups()`, `backupToDrive()`, `prepareDriveBackupRestore()`.
   - `AccountSettingsScreen.kt`: Upgraded `AccountCard` in `SettingsScreen` with authenticated view (initials, display name, email, "Google Drive Aktif" chip, "Keluar" button) and unauthenticated view ("Masuk dengan Google" button + offline-first explanatory card).
   - `MoreScreen.kt`: Upgraded `BackupDialog` and `DataScreen` with options for Local Backup, Local Restore, Google Drive Cloud Backup (passphrase protected), and Google Drive Cloud Restore (snapshot selector list + passphrase validation).
   - `ArinaCalculatorApp.kt`: Wired Google Sign-In activity result launcher, Google Play Services OAuth access token retriever (`GoogleAuthUtil.getToken(context, account, "oauth2:https://www.googleapis.com/auth/drive.appdata")`), and connected callbacks.
4. **Localization**:
   - Added Indonesian and English resources for Google Sign-In, Cloud Backup, Restore, Status chips, and Error messages in `strings.xml` and `values-en/strings.xml`.

### Verification & Test Outcomes
- **Automated Tests:** `.\gradlew.bat testDebugUnitTest` -> `BUILD SUCCESSFUL in 7s` (**41/41 unit tests passing**, including `GoogleDriveBackupClientTest`).
- **Build:** `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL in 5s`.
- **On-Device ADB Testing (`emulator-5554`):**
  - App installed & launched smoothly with zero runtime errors in Logcat.
  - Verified `BackupDialog` rendering Local Cadangan & Google Drive sections (`docs/archive/evidence/firebase_backup_dialog.png`).
  - Verified `SettingsScreen` rendering unauthenticated offline-first account card (`docs/archive/evidence/firebase_auth_settings_screen.png`).
  - Verified "Masuk dengan Google" action launching Google Play Services sign-in system prompt (`docs/archive/evidence/firebase_google_signin_prompt.png`).

---

## [2026-08-29] Excel (.xlsx) Import End-to-End Audit, Defect Remediation & UI Hardening

### Root Cause Analysis & Diagnostic Findings
1. **Unresponsive / Silent Commit Drop on Clean Database:**
   - In `AppViewModel.kt:commitImport()`, the previous implementation had guards `val businessId = selectedBusinessId.value ?: return@launch` and `val projectId = selectedProjectId.value ?: return@launch`.
   - On a clean install or prior to explicitly creating a business/plot, `selectedBusinessId` and `selectedProjectId` are `null`.
   - Clicking "Setujui dan impor" dropped the coroutine silently via `return@launch` with zero visual feedback, leaving the user on the preview card believing the app was frozen.
2. **File Picker MIME Type Narrowing:**
   - In `ArinaCalculatorApp.kt`, `ActivityResultContracts.GetContent()` with only `"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"` caused many Android system file managers to gray out or fail to return URIs for spreadsheets categorized under generic MIME types (`application/vnd.ms-excel`, `application/octet-stream`, `*/*`).
3. **Missing State Transitions & Error Granularity:**
   - Incomplete state handling when `WorkbookImportPreview` contained fatal errors (`!preview.canCommit`) previously left the card in `Ready` with a disabled button rather than transitioning to an explicit `ImportUiState.Failure`.
   - `ImportUiState.Loading` lacked cancel/dismiss controls if a user picked the wrong file or wanted to abort.

### Fixes Implemented
1. **Auto-Provisioning Workspace on Commit (`AppViewModel.kt`)**:
   - `commitImport()` now inspects `selectedBusinessId` and `selectedProjectId`: if either is `null`, it automatically creates a named business from `preview.sourceName` (e.g. "test_padi" or "Usaha Tani") and a default project ("Lahan Utama"), sets them in the ViewModel state, and commits the batch seamlessly.
   - Sets `importState.value = ImportUiState.Loading` during the commit transaction on `Dispatchers.IO`.
2. **Hardened Stream Reading & Error Routing (`AppViewModel.kt`)**:
   - `previewWorkbook()` wraps `contentResolver.openInputStream(uri)` with non-empty assertions, file size checks (up to 25 MB), and safely closes all streams on `Dispatchers.IO`.
   - Explicitly transitions to `ImportUiState.Failure(UiText.Raw(preview.errors.joinToString("\n")))` when `!preview.canCommit` and fatal errors exist.
   - Added `resetImportState()` to reset `importState` back to `ImportUiState.Idle`.
3. **Document Picker Modernization (`ArinaCalculatorApp.kt`)**:
   - Migrated from `ActivityResultContracts.GetContent()` to `ActivityResultContracts.OpenDocument()` with broad spreadsheet MIME types (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `application/vnd.ms-excel`, `application/octet-stream`, `*/*`).
4. **Enhanced UI States & Controls (`MoreScreen.kt`)**:
   - Upgraded `ImportCard`:
     - `ImportUiState.Loading`: Displays `CircularProgressIndicator`, loading description, and "Batal" button.
     - `ImportUiState.Failure`: Displays styled error banner with warning icon, "Pilih Berkas Lain", and "Tutup" button.
     - `ImportUiState.Committed`: Displays success banner with checkmark, "Impor Workbook Lain", and "Tutup" button.

### Verification & On-Device Validation
- **Automated Tests:** `.\gradlew.bat testDebugUnitTest` -> **`BUILD SUCCESSFUL` (41/41 unit tests passing)** including `GoldenWorkbookImportTest` and `GoldenWorkbookEndToEndTest`.
- **On-Device ADB Testing (`emulator-5554`):**
  - Pushed `CATATAN KEUANGAN PADI 1 Ha ADE.xlsx` to `/sdcard/Download/test_padi.xlsx`.
  - Opened "Berkas dan Data" and launched document picker (`docs/archive/evidence/import_doc_picker.png`).
  - Selected `test_padi.xlsx` and verified preview populated with 38 rows and 1 deterministic enrichment (`docs/archive/evidence/import_preview_ready.png`).
  - Tapped "Setujui dan impor 38 baris" -> transitioned cleanly to `ImportUiState.Committed` ("Impor selesai: 38 baris, 1 dipulihkan otomatis", `docs/archive/evidence/import_committed_success.png`).
  - Returned to Home screen (`docs/archive/evidence/import_home_with_data.png`) and verified:
    - Auto-provisioned Usaha ("test_padi") & Lahan ("Lahan Utama").
    - Kas Realisasi: Rp23.341.000 (Rp45.500.000 income - Rp22.159.000 expense).
    - Expense breakdown chart fully populated with 9 categories.
  - Verified Laporan screen (`docs/archive/evidence/import_laporan_screen.png`) with operational income and profit/loss.
  - Verified Logcat `adb logcat -d *:E` clean with 0 app runtime errors.

---

## [2026-08-29] SemVer Release Automation, Dynamic Gradle Signing & GitHub Actions Play Store Deployment Pipeline

### Implementation Details
1. **SemVer Release Helper Scripts (`package.json` & `scripts/bump-version.js`)**:
   - Added root `package.json` with npm scripts: `release:patch`, `release:minor`, and `release:major`.
   - Implemented `scripts/bump-version.js`:
     - Reads current `versionName` (SemVer `MAJOR.MINOR.PATCH`) and `versionCode` (integer) from `app/build.gradle.kts`.
     - Calculates new version string and increments `versionCode` by +1.
     - Synchronizes updates to `app/build.gradle.kts` and `package.json`.
     - Automatically stages, commits (`chore(release): vX.Y.Z (build N)`), and creates an annotated Git tag (`vX.Y.Z`).
     - Provides clear interactive instructions to push tags: `git push origin <branch> --tags`.
2. **Release Signing Isolation in Gradle (`app/build.gradle.kts`)**:
   - Configured `signingConfigs.create("release")` to read signing credentials dynamically from environment variables: `KEYSTORE_PATH` / `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
   - When environment variables are absent, local builds, unit tests, and debug APK assemblies succeed gracefully without signing errors.
3. **R8 / ProGuard Keep Rules (`app/proguard-rules.pro`)**:
   - Added explicit retain rules for Room Database (`AppDatabase`, DAOs), Domain calculation models, Coroutines/Flow reflection, Firebase Auth / Google Play Services, and Cloud Backup DTOs.
4. **CI/CD Deployment Workflow (`.github/workflows/deploy_play_store.yml`)**:
   - Configured GitHub Actions workflow triggering on `v*` tags and `workflow_dispatch`.
   - Decodes base64 keystore and google-services credentials dynamically.
   - Runs `./gradlew testDebugUnitTest` and builds signed release bundle `./gradlew bundleRelease`.
   - Deploys release bundle to Google Play Console Internal Testing track via `r0adkll/upload-google-play@v1`.
5. **Operator Guide (`docs/PLAY_STORE_DEPLOYMENT.md`)**:
   - Created comprehensive documentation detailing local release command usage, keystore generation via `keytool`, SHA-1 extraction & Firebase Console registration, Play Console Service Account setup for `adsurkasur.dev@gmail.com`, and GitHub Repository Secrets mapping.

### Verification & Validation
- **Automated Tests:** `.\gradlew.bat testDebugUnitTest assembleDebug` -> `BUILD SUCCESSFUL` (41/41 tests passing).
- **Bundle Generation:** `.\gradlew.bat bundleDebug` -> `BUILD SUCCESSFUL` (`app.aab` packaging verified).
- **CLI Helper:** Validated `node scripts/bump-version.js` error handling and argument parsing.

---

## [2026-08-29] SemVer Bump Script BOM Stripping & PATCH Release (v0.0.2) Verification

### Root Cause & Remediation
- **BOM Issue:** UTF-8 Byte Order Mark (`\uFEFF`) in `package.json` or config files could cause `JSON.parse()` to throw a SyntaxError when read in strict Node.js runtime environments.
- **Fix:** Enhanced `scripts/bump-version.js` with `readFileWithoutBom(filePath)` which strips leading `/^\uFEFF/` characters across all configuration file readers before invoking `JSON.parse()` or regular expression replacements. Re-saved `package.json` and `app/build.gradle.kts` in clean UTF-8 without BOM.

### PATCH Release Verification (`npm run release:patch`)
- Executed `npm run release:patch` targeting baseline version `0.0.1` (code: 2).
- **Output:**
  - `versionName`: `0.0.1` -> `0.0.2`
  - `versionCode`: `2` -> `3`
  - Git commit: `chore(release): v0.0.2 (build 3)` (`fa7f7f5`)
  - Git tag: `v0.0.2`
  - Prompted operator with `git push origin <branch> --tags` to trigger Google Play deployment.

### Quality Gate & Build Verification
- **Unit Tests:** `.\gradlew.bat testDebugUnitTest` -> **`BUILD SUCCESSFUL` (41/41 unit tests passing)**.
- **Debug Assembly:** `.\gradlew.bat assembleDebug` -> **`BUILD SUCCESSFUL in 2s`** (clean compilation and packaging).

---

## [2026-08-29] Baseline Reset to v0.0.1, Workflow Hardening & Dual Release (Google Play + GitHub Releases)

### Workflow Hardening & Reordering (`.github/workflows/deploy_play_store.yml`)
1. **Pre-Build Secret Decoding**: Decodes `GOOGLE_SERVICES_JSON_BASE64` (with fallback mock JSON generation to prevent `:app:processDebugGoogleServices` failure) and `RELEASE_KEYSTORE_BASE64` before invoking Gradle tasks.
2. **Top-Level Permissions**: Configured `permissions: contents: write` for releasing assets.
3. **Dual Release Pipeline**:
   - Compiles and signs release App Bundle (`./gradlew bundleRelease`) and release APK (`./gradlew assembleRelease`).
   - Deploys release bundle to Google Play Console on the **Internal Testing** track via `r0adkll/upload-google-play@v1`.
   - Publishes a **GitHub Release** via `softprops/action-gh-release@v2` attaching `app-release.apk` and `app-release.aab` with auto-generated release notes.

### Baseline Reset
- Reset `versionName` to `"0.0.1"` and `versionCode` to `1` in `app/build.gradle.kts` and `package.json`.
- Purged previous draft/erroneous tags (`v0.0.2`, `v0.0.3`).
- Created standardized baseline release tag `v0.0.1`.

---

## [2026-08-29] Privacy Policy Webpage, Google Play Compliance Guide & Store Listing Package

### 1. Bilingual Privacy Policy Webpage (`docs/privacy-policy.html`)
- Created a standalone, responsive, accessible HTML5 document with Material 3 styling and dark mode support.
- Fully reflects the app's real architecture:
  - **Local-First / SQLite Room**: All farm bookkeeping, plot data, cash flows, and feasibility calculations remain strictly on device.
  - **Optional Google Sign-In & AES-256-GCM Backup**: Encrypted client-side with user passphrase prior to storage in hidden `drive.appdata` folder.
  - **On-Device Excel (.xlsx) Processing**: Pure Kotlin POI-free in-memory workbook reading and writing.
  - **Zero Telemetry & Zero Ads**: 100% free of advertising SDKs, behavioral tracking, and analytics (`O-011`).
- Included GitHub Pages deployment guide for public URL: `https://adsurkasur.github.io/arina-agri-calculator/privacy-policy.html`.

### 2. Google Play Console Compliance Guide (`docs/STORE_LISTING_AND_POLICIES.md`)
- Prepared exhaustive declarations for:
  - **App Access**: All features available without special access.
  - **Ads**: No ads.
  - **Content Rating (IARC)**: Rated 3+ / Everyone (Utility/Productivity).
  - **Target Audience**: 18+ (Adult farmers, agribusines managers, advisors).
  - **Data Safety Form**: Name/email optional for backup identity; zero third-party sharing; client-side AES-256-GCM encryption.
  - **Financial Features**: Declared as Personal/Business accounting and expense tracking tool for farm feasibility (not banking or lending).

### 3. Store Listing Copy & Visual Assets (`docs/STORE_LISTING_AND_POLICIES.md` & `docs/assets/store/`)
- Indonesian and English Store Listing Copy (App name, 80-char short description, and structured full description emphasizing 5 core features).
- Organized high-resolution screenshots in `docs/assets/store/`:
  - `screenshot-01-beranda.png` (Home & farm summary)
  - `screenshot-02-kelayakan-bep-hpp.png` (BEP, HPP, B/C Ratio calculations)
  - `screenshot-03-impor-excel.png` (Excel import & semantic matcher)
  - `screenshot-04-laporan-keuangan.png` (Cash flow reports)
  - `screenshot-05-buku-kas-anggaran.png` (Dual isolated ledgers)
  - `screenshot-06-cadangan-terenkripsi.png` (Encrypted Drive backup)

---

## [2026-08-29] Two-Tier Guided Tour, Hard Deletion, Scope Validation, and UX Architecture Restructuring

### Architectural Summary & Core Changes
1. **Two-Tier Modular Guided Tour Architecture (`SpotlightTour.kt`, `AppPreferences.kt`, `ArinaCalculatorApp.kt`)**:
   - Replaced monolithic single-pass tour with a two-tier contextual tour system:
     - **Tier 1 (Core Workflow Tour)**: 6 steps covering Scope Bar (Usaha & Lahan), Book Mode toggle (Proyeksi vs Realisasi), Dashboard Metrics (Hero Saldo & Profit), Catatan Menu, Laporan Menu (BEP/HPP/BCR), and Berkas & Data Menu.
     - **Tier 2 (Contextual Per-Page Feature Tours)**: Triggered on first entry to specific sub-pages or on-demand via the new "Panduan Halaman" header action:
       - *Pengaturan & Akun (`AccountSettingsScreen.kt`)*: Google Drive cloud sync (`drive.appdata`), AES-256-GCM encryption, local reset.
       - *Kelola Usaha & Lahan (`ManageScopeScreen.kt`)*: Farm ventures, field plots, crop commodities, planting seasons.
       - *Impor Berkas (`MoreScreen.kt:ImportCard`)*: Excel workbook selection, semantic matcher, target ledger selection.
   - Independent persistent flags in `AppPreferences`: `hasSeenCoreTour`, `hasSeenSettingsTour`, `hasSeenScopeTour`, `hasSeenImportTour`.
2. **Transaction Hard Deletion (`FinanceDao.kt`, `FinanceRepository.kt`, `AppViewModel.kt`, UI)**:
   - Added permanent deletion (`deleteEvent(eventId)`) alongside accounting reversal.
   - `FinanceRepository.deleteEvent` executes inside Room transaction, clearing reversal references and cascading deletion across journal entries, postings, and obligations via foreign keys.
   - Upgraded `EventCard` context menu offering explicit "Hapus Transaksi" (permanent delete with destructive confirmation dialog) and "Balik / Koreksi Transaksi" (reversal journal entry).
3. **Navigation Restructuring & Semantic Icon ("Catatan")**:
   - Renamed bottom navigation label from `"Catat"` to `"Catatan"` across strings (`strings.xml`, `values-en/strings.xml`).
   - Replaced generic plus icon with semantic ledger icon (`Icons.AutoMirrored.Filled.ReceiptLong`).
   - Relocated full transaction history from `HomeScreen.kt` to `EntryScreen.kt`.
   - `EntryScreen.kt` now features a SegmentedButton top switcher: *"Buat Catatan"* (Form) vs *"Daftar Catatan"* (Searchable history feed with direction filter chips).
   - `HomeScreen.kt` streamlined to executive cards with recent preview and direct shortcut to "Catatan".
4. **Scope / Lahan Validation & Visual Feedback (`EntryScreen.kt`, `AppViewModel.kt`)**:
   - Added persistent warning banner on `EntryScreen` when no active Lahan exists, with inline *"Tambah Lahan Sekarang"* button navigating to scope manager.
   - Guarded submit action in `EntryScreen` and `AppViewModel.record()`: displays error snackbar/message *"Pilih lahan terlebih dahulu sebelum mencatat transaksi."* instead of dropping silently.
5. **Modal Bottom Sheet Gesture Decoupling (`ArinaSheet.kt`, `EntryScreen.kt`, `ManageScopeScreen.kt`)**:
   - Set `skipPartiallyExpanded = true` across all bottom sheets and paired with inner `verticalScroll(rememberScrollState())`, preventing awkward mid-scroll sheet resizing.

### Verification & Quality Gate
- **Unit Tests:** `.\gradlew.bat testDebugUnitTest` -> **`BUILD SUCCESSFUL` (42/42 tests passing)** including `TransactionManagementTest`.
- **Debug Assembly:** `.\gradlew.bat assembleDebug` -> **`BUILD SUCCESSFUL in 6s`** (clean compilation and APK generation).

---

## [2026-08-29] Super Comprehensive Spotlight Tour Expansion & Agribusiness Educational Content

### Architectural Summary & Core Changes
1. **Target & Step List Expansion (`SpotlightTour.kt`)**:
   - Added 3 new `TourTarget` enum values: `REPORTS_CASHFLOW` (bound to Cash Flow card on Reports screen), `DATA_EXPORT` (bound to Export & Backup card on More screen), and `SETTINGS_DISPLAY` (bound to Display/Theme card on Settings screen).
   - Expanded step lists:
     - `ReportsTourSteps`: expanded from 3 to 4 steps (+Operating vs Investing vs Financing Cash Flows).
     - `SettingsTourSteps`: expanded from 2 to 3 steps (+Display, Theme, Language, Motion speed).
     - `ImportTourSteps`: expanded from 2 to 3 steps (+Excel/PDF export and encrypted backups).
   - Preserved 100% of the animation architecture (`FastOutSlowInEasing`, coordinate interpolation, dimmed canvas scrim with `BlendMode.Clear`, pulsing highlight ring, smooth card translation and scale).
2. **Deep Agribusiness Domain & Educational Tooltips (`strings.xml`, `values-en/strings.xml`)**:
   - Enriched 12+ string tooltips in both Indonesian and English with concrete, farmer-actionable explanations:
     - *Hero Summary*: Added practical troubleshooting for negative cash balances (reminding farmers to enter "Modal Sendiri" / Starting Capital).
     - *Scope & Farm Isolation*: Explained total independence of venture datasets (deleting one does not affect others).
     - *Book Modes*: Detailed the farming lifecycle workflow (Proyeksi before planting -> Realisasi during season -> Perbandingan tab for plan accuracy audit).
     - *Transaction Entry*: Explained auto-journal creation rules (Debit Cash/Credit Revenue, Debit Expense/Credit Payable) and volume/unit calculations.
     - *Audit & Hard Deletion*: Clarified when to use reversal journals (retaining audit trails) vs permanent deletion (correcting input mistakes).
     - *Feasibility Indicators*: Deepened BEP Rupiah, BEP Volume, HPP/Unit, and B/C Ratio decision criteria.
     - *Security & Privacy*: Explicitly stated private Google Drive appdata isolation and zero third-party server exposure.
3. **Screen Replay & Header Action Consistency (`EntryScreen.kt`, `ReportsScreen.kt`, `ArinaCalculatorApp.kt`)**:
   - Added `onStartTour: (() -> Unit)? = null` parameter to `EntryScreen` and `ReportsScreen`.
   - Rendered "Panduan Halaman" `TextButton` with `Icons.Default.Info` in the top header row across all destinations (`MoreScreen`, `ManageScopeScreen`, `EntryScreen`, `ReportsScreen`), providing uniform per-page replay capabilities.
   - Re-routed `onStartTour` from `ScreenContent` to trigger `"ENTRY"` and `"REPORTS"` tours.
4. **Test Suite Verification (`TransactionManagementTest.kt`)**:
   - Updated assertions to verify all 22 `TourTarget` enum entries, 7 Core tour stages, 3 Entry stages, 4 Reports stages, 3 Settings stages, 2 Scope stages, and 3 Import stages.

### Verification & Quality Gate
- **Unit Tests:** `.\gradlew.bat testDebugUnitTest` -> **`BUILD SUCCESSFUL` (42/42 tests passing)**.
- **Debug Assembly:** `.\gradlew.bat assembleDebug` -> **`BUILD SUCCESSFUL` (Clean APK assembly in 10s)**.

---

## [2026-08-29] Release v0.0.3 (Build 3) Published

### Release Summary
- **Version:** `0.0.3` (versionCode: `3`)
- **Git Tag:** `v0.0.3`
- **Key Features Included:**
  - Expanded 22-target Spotlight Tour architecture with 6 contextual step lists.
  - Deep agribusiness educational content across 12+ tooltips in Indonesian and English.
  - Per-page guided tour replay header buttons (*"Panduan Halaman"*) across all screens.
  - Cash flow breakdown education (Operating, Investing, Financing) on Reports screen.
  - Export and encrypted backup guidance on More/Data screen.
  - Display and localization options guidance on Settings screen.
- **Pipeline:** Triggered `.github/workflows/deploy_play_store.yml` deploying signed `.aab` to Google Play Console Internal Track and publishing GitHub Release assets.

---

## [2026-08-29] Auto-Scroll & Interactive Multi-Page Spotlight Tour, Responsive Layouts, and Smooth Venture Transitions

### Architectural Summary & Core Changes
1. **Adaptive Tab Switcher & Anti-Overlap (`EntryScreen.kt`)**:
   - Refactored `"Buat Catatan"` vs `"Daftar Catatan"` top switcher using `BoxWithConstraints`.
   - On narrow container widths (< 340dp), conditionally hides leading icons to prioritize clean, un-truncated text with `maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis`, eliminating all icon-text overlap.
2. **Auto-Scroll via `BringIntoViewRequester` (`SpotlightTour.kt`)**:
   - Integrated `BringIntoViewRequester` directly into `Modifier.tourTarget()` and `TourTargetRegistry`.
   - On every step transition, automatically calls `requester.bringIntoView()`, smoothly scrolling targets located below the fold or off-screen into the viewport before placing the highlight cutout.
3. **Interactive Multi-Page Live Guided Tour Navigation (`SpotlightTour.kt`, `ArinaCalculatorApp.kt`)**:
   - Extended `TourStep` to hold target `Destination`, `Overlay`, and `manageScope` metadata.
   - Expanded `CoreTourSteps` into a 17-step live journey that automatically navigates across pages:
     - *Beranda*: Usaha & Lahan Scope, Book Mode Toggle, Saldo Kas Hero, 9-Category Cost Distribution.
     - *Catatan* (Live transition): Form switcher, Transaction Kinds (Cash In/Out, Capital, Credit, Volume/HOK), History feed & Reversal/Deletion.
     - *Laporan* (Live transition): Financial/Feasibility/Comparison perspectives, P&L Statement, Operating Cash Flow, Feasibility metrics (BEP Rupiah, BEP Volume, HPP, B/C Ratio).
     - *Berkas & Data* (Live sheet): Excel workbook importer (Semantic Matcher), Target Book selection, PDF/Excel export & AES-256-GCM backups.
     - *Pengaturan* (Live sheet): Google Drive cloud sync (`drive.appdata`), Passphrase security, Display & Language settings.
   - On tour dismissal or finish, safely resets `destination = Destination.HOME`, `overlay = Overlay.NONE`, `manageScope = false`.
4. **Smooth Venture & Data Transformations (`HomeScreen.kt`)**:
   - Keyed `HeroCard`'s `AnimatedContent` on `Pair(mode, projectId)` with `slideInVertically` / `slideOutVertically` using `FastOutSlowInEasing`.
   - Applied `Modifier.animateContentSize()` to `SimpleMetricGrid` and `ExpenseDistributionCard` for seamless visual adjustments during farm or plot switching.
5. **Action Button Symmetry & Centering (`ManageScopeScreen.kt`)**:
   - Set balanced `contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)` and `TextAlign.Center` on `"Pakai"` (`OutlinedButton`), `"Ubah"`, and `"Hapus"` actions in `ScopeRow`.
6. **Multi-DPI Typography & Wrap Prevention (`ReportsScreen.kt`)**:
   - Applied `maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis` to `PrimaryTabRow` tabs, preventing awkward multi-line hyphenation (e.g. `"Perbandinga\nn"`).

### Verification & Quality Gate
- **Unit Tests:** `.\gradlew.bat testDebugUnitTest` -> **`BUILD SUCCESSFUL` (42/42 tests passing)** including updated `TransactionManagementTest`.
- **Debug Assembly:** `.\gradlew.bat assembleDebug` -> **`BUILD SUCCESSFUL in 5s`** (clean debug APK output).

---

## [2026-08-29] Release v0.0.4 (Build 4) Published

### Release Summary
- **Version:** `0.0.4` (versionCode: `4`)
- **Git Tag:** `v0.0.4`
- **Key Features Included:**
  - Auto-Scroll via `BringIntoViewRequester` in Spotlight Tour for targets outside the viewport.
  - Interactive multi-page live guided tour (17 stages) across Beranda, Catatan, Laporan, Berkas & Data, and Pengaturan.
  - Adaptive Catatan tab switcher (`BoxWithConstraints`) eliminating icon-text overlap on narrow viewports.
  - Smooth venture/plot/book switching animations on Home metrics and HeroCard.
  - Symmetrical `contentPadding` and centering for action buttons in Scope Manager.
  - Text-wrap prevention on Reports tabs (`maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis`).
- **Pipeline:** Triggered `.github/workflows/deploy_play_store.yml` deploying signed `.aab` to Google Play Console Internal Track and publishing GitHub Release assets.

---

## [2026-08-29] Reactive Calculation Synchronization (Unit Price × Volume = Total Nominal) and Data Reconciliation

### Audit & Root Cause
- **Diagnosis:** `EntryScreen.kt` managed `amount`, `quantity`, and `unitPrice` as disconnected local string states with zero reactive binding. Typing volume and unit price never updated the total amount, while entering total amounts directly left unit price un-derived or stale. On submission, the form dispatched unsynchronized raw values to `onRecord`, allowing mathematically contradictory entries (e.g. 10 kg @ Rp5.000 with Rp100.000 total) to persist in Room database and corrupt report unit costs.

### Core Changes & Architectural Solution
1. **Pure Domain Calculation Engine (`TransactionCalculation.kt`)**:
   - `parseQuantity(raw: String?)`: Robust parsing supporting Indonesian comma (`2,5`) and standard point (`2.5`) decimals, trimming, whitespace, and non-numeric safety.
   - `computeTotalRupiah(quantity, unitPrice)`: Exact integer rupiah calculation using `BigDecimal` and `RoundingMode.HALF_UP`.
   - `computeUnitPriceRupiah(total, quantity)`: Exact integer rupiah unit price derivation (`total / quantity`).
   - `reconcile(amount, quantity, unitPrice)`: Guarantees 100% non-contradictory triplets for database storage.
2. **Reactive Smart Field Binding (`EntryScreen.kt` & `BudgetScreen.kt`)**:
   - **Scenario A (Volume + Unit Price)**: Auto-computes `amount = quantity * unitPrice` and displays visual feedback with calculation icon (`Icons.Default.Calculate`) and supporting text: `"✓ Dihitung otomatis: X sat × Rp Y"`.
   - **Scenario B (Direct Lump-Sum Total)**: If user enters Total and subsequently enters Volume, auto-derives `unitPrice = total / volume` with supporting text: `"Dihitung dari Total ÷ Volume (Rp X / sat)"`.
   - **Scenario C (Editing Total with Volume)**: Adjusting Total directly or tapping Quick Amount chips (`+50 rb`, etc.) re-derives `unitPrice` to maintain instant mathematical harmony without manual calculation.
   - **Clearing & Reset**: Safe handling of empty/zero/cleared states without exceptions or `NaN`/`Infinity`.
3. **Database-Level Reconciliation (`AppViewModel.kt` & `FinanceRepository.kt`)**:
   - `AppViewModel.record()`, `createBudgetItem()`, and `updateBudgetItem()` pass all incoming records through `TransactionCalculation.reconcile()` prior to persistence, ensuring absolute integrity of stored journals, ledger lines, and budget items.
4. **Comprehensive Unit Test Suite (`TransactionCalculationTest.kt`)**:
   - Tested integer calculations, comma and dot decimal volumes, half-up fractional rounding, lump-sum preservation, derived unit prices, and edge cases.

### Verification & Quality Gate
- **Unit Tests:** `.\gradlew.bat testDebugUnitTest` -> **`BUILD SUCCESSFUL` (47/47 tests passing)**.
- **Debug Assembly:** `.\gradlew.bat assembleDebug` -> **`BUILD SUCCESSFUL in 5s`**.
- **Live Local Emulator Verification (`emulator-5554` / Pixel 7 API 34):**
  - Interactive onboarding flow & Spotlight Tour rendering verified.
  - Usaha ("Kebun Jagung") & Lahan ("Petak Barat") creation verified.
  - Scenario A: Input Volume `2,5 kg` and Unit Price `Rp 20.000` -> reactive total auto-computed to `Rp 50.000` with `✓ Dihitung otomatis: 2,5 kg × Rp20.000` badge.
  - Scenario C: Tapped quick chip `+50 rb` -> total adjusted to `Rp 100.000` and unit price auto-derived to `Rp 40.000 / kg` with `Dihitung dari Total ÷ Volume (Rp40.000 / kg)` feedback.
  - Form submission -> verified balanced double-entry journal creation, history card rendering (`-Rp100.000`), Beranda dashboard metric updates, and cost breakdown donut chart.
  - Zero runtime crashes or logcat errors.

---

## 2026-08-29 - Release v0.0.5 (Build 5)

### Release Highlights
- **Reactive Transaction Calculation & Field Synchronization**:
  - Auto-multiplication of Volume and Unit Price to Total Amount in real-time.
  - Auto-derivation of Unit Price from Total Amount and Volume for lump-sum entries.
  - Quick amount chip synchronization.
  - Exact integer rupiah rounding with pure domain engine (`TransactionCalculation.kt`).
  - Database-level reconciliation across `record()`, `createBudgetItem()`, and `updateBudgetItem()`.
- **Release Tag & Deployment**:
  - Version bumped: `0.0.4` -> `0.0.5` (versionCode `4` -> `5`).
  - Git tag `v0.0.5` created and pushed to `origin main --tags`.
  - GitHub Actions CI/CD workflow triggered for Play Store Internal Track & GitHub Releases.

---

## 2026-08-29 - Spotlight Tour Window Dimming & Z-Index Architecture Resolution + Hero Card Negative Red Gradient

### 1. Spotlight Tour Window Dimming, Touch Hit-Testing, & Z-Index Resolution
- **Root Cause Analysis**:
  - Wrapping `SpotlightTour` in an Android `Dialog` triggered OS-level `WindowManager.LayoutParams.FLAG_DIM_BEHIND` with `dimAmount = 0.6f`, causing the entire Activity window behind the spotlight cutout to appear dark/dimmed rather than brightly illuminated.
  - Furthermore, using `graphicsLayer { translationY = cardY }` on `TourCard` shifted only the draw layer while leaving Compose layout hit-test bounds at `y = 0`, causing tap gestures on "Lanjut" / "Mulai Gunakan" to miss the card bounds.
- **Architectural Solution**:
  - Restored `SpotlightTour` as a pure Compose overlay with `Modifier.zIndex(100f)`.
  - Embedded `ScopeTourSteps` directly inside `ManageScopeSheet`'s modal container, allowing bottom sheet tours to run inside the bottom sheet window without cross-window collision.
  - Replaced `graphicsLayer { translationY }` with `Modifier.offset { IntOffset(0, cardY.roundToInt()) }`, aligning physical touch hit-testing bounds with animated card visual position.
  - Moved touch consumption (`pointerInput`) from the parent `Box` to the `Canvas` (scrim) layer, ensuring all button clicks on `TourCard` register reliably.
  - Removed double status/navigation bar padding on `TourCard` to guarantee pixel-perfect coordinate tracking across varying screen heights.

### 2. Negative Hero Card Gradient ("Uang Kas ...") in Coral Red (`semantic.danger`) & Smooth Color Transitions
- **`HomeScreen.kt` (`HeroCard`)**:
  - Added dynamic condition for `amount < 0`:
    - **Light Mode**: Gradient from `semantic.danger` (`#9A442D`) to `#7C2E19` with `#FFFFFF` text.
    - **Dark Mode**: Gradient from `semantic.dangerContainer` (`#7C2E19`) to `#5D1805` with `#FFDBD2` text.
    - **Positive/Zero**: Retained emerald/teal gradient (`primary` to `tertiary`).
  - Implemented smooth color transition interpolation using `animateColorAsState` with `tween(motion.mediumMillis, easing = FastOutSlowInEasing)` for `heroStart`, `heroEnd`, and `onHero`. Switching book modes or updating balances smoothly morphs card colors between emerald green and danger red with zero visual pop.

### Verification Evidence
- **Unit Tests:** `.\gradlew.bat testDebugUnitTest` -> **`BUILD SUCCESSFUL` (47/47 tests passing)**.
- **Debug Assembly:** `.\gradlew.bat assembleDebug` -> **`BUILD SUCCESSFUL in 3s`**.
- **Live Local Emulator Verification (`emulator-5554` / Pixel 7 API 34):**
  - Verified Proyeksi hero card (-Rp100.000) displays in rich danger red gradient, while Realisasi (Rp0) stays fresh green.
  - Verified Spotlight Tour cutouts are 100% bright, undimmed, and razor-sharp across both root destinations and bottom sheets.

---

## 2026-08-29 - Release v0.0.7: Directional Book Mode Parallax Transition, Adaptive Brand Logo, Google OAuth Configuration & Diagnostic Feedback

### 1. Directional Screen & Book Mode Animation Transitions
- **`ArinaCalculatorApp.kt` (`ScreenContent`)**:
  - Keyed `AnimatedContent` on `Pair(destination, state.selectedBookMode)` to animate state switches seamlessly.
  - Added horizontal directional parallax transition when switching between Proyeksi and Realisasi:
    - **Proyeksi $\rightarrow$ Realisasi**: Glides right-to-left with subtle `FastOutSlowInEasing` and cross-fade.
    - **Realisasi $\rightarrow$ Proyeksi**: Glides left-to-right with cross-fade.
  - Preserved standard vertical lift & fade when switching between main bottom bar destinations.

### 2. Adaptive Brand Logo System (*Green on Light* & *Green on Dark*)
- **`SharedComponents.kt` (`ArinaAppLogo`)**:
  - Created theme-adaptive logo composable with dynamic container and primary brand tint:
    - **Light Mode**: Deep forest green (`#226A4D`) on soft mint container (`#A9F2CC`, 55% alpha) $\rightarrow$ *Green on Light*.
    - **Dark Mode**: Luminous mint green (`#8ED5B1`) on dark slate container (`#2C322E`) $\rightarrow$ *Green on Dark*.
  - Uniformly applied in `ScopeHeader`, `AppRail`, and `AccountSettingsScreen` (About card).
- **Adaptive Launcher Icon (Android 13+ Themed Icons)**:
  - Enabled `<monochrome android:drawable="@drawable/ic_launcher_foreground_inset" />` in `ic_launcher.xml` and `ic_launcher_round.xml`.

### 3. Google Sign-In Diagnostic Error Handling & OAuth Synchronization
- **`ArinaCalculatorApp.kt` (`googleSignInLauncher`)**:
  - Replaced generic error snackbar with actionable diagnostics distinguishing status codes:
    - `10`: `DEVELOPER_ERROR` (SHA-1/OAuth Client ID unregistered).
    - `12500`: `SIGN_IN_FAILED` (Play Services/account error).
    - `7`: `NETWORK_ERROR`.
    - `12501`: Gracefully suppressed when user cancels account picker.
- **`app/google-services.json`**:
  - Synchronized complete `oauth_client` array containing Debug SHA-1 (`f3c5c2...`), Release SHA-1 (`1bcf8a...`), and Web Client ID (`457160288003-...apps.googleusercontent.com`).

### Verification Evidence
- **Unit Tests:** `.\gradlew.bat testDebugUnitTest` -> **`BUILD SUCCESSFUL` (47/47 tests passing)**.
- **Assembly:** `.\gradlew.bat assembleDebug` -> **`BUILD SUCCESSFUL`**.
- **Live Local Emulator Verification (`emulator-5554` / Pixel 7 API 34):**
  - Uninstalled old instances cleanly.
  - Verified Proyeksi $\leftrightarrow$ Realisasi glide and color transitions.
  - Verified adaptive logo badge rendering in header and settings.
  - Verified hardware BackHandler dismisses active tour overlays immediately.
