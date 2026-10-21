---
status: implemented
last_verified: 2026-10-22
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner requirement to learn native development
  - official Android architecture and Compose guidance reviewed 2026-08-15
  - docs/DOMAIN_MODEL.md
---

# Architecture

## Decision

Build the Android application in **Kotlin with Jetpack Compose**, using native Android architecture and adaptive Material 3 components. Capacitor is excluded because its primary model is a web application running through a native container. Flutter is a credible compiled cross-platform option, but its own rendering/runtime model does not best serve the current goal of learning the Android-native stack.

This choice can be revisited if an iOS requirement becomes funded and concrete. It is not a judgment that Flutter is a WebView.

## Stack in use

| Concern | In use (schema 11) |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 adaptive layouts |
| State | One `AppViewModel` exposing `StateFlow<AppUiState>`; coroutines and Room `Flow` |
| Local structured data | Room over SQLite, schema 11, exported schemas in `app/schemas/` |
| Preferences | `SharedPreferences` via `AppPreferencesState` (DataStore considered and rejected, D-082) |
| Background work | None scheduled; backup, export and import run on user action in coroutines |
| File selection/export | Android Storage Access Framework |
| Dependency injection | Manual, a lazy container in `MankeuApplication` and `viewModelFactory` (Hilt rejected, D-082) |
| Serialization | `PortableBackupCodec` (versioned binary snapshot) for backups and sync change sets; `org.json` for Drive |
| Charts | Vico |
| Identity and cloud backup | Firebase Auth, Google Drive REST `appDataFolder` (optional) |
| Tests | JVM unit tests, Room integration and migration tests (`MigrationTestHelper`), screenshot walks on the emulator |

Official references:

- [Jetpack Compose](https://developer.android.com/develop/ui/compose)
- [Adaptive Compose layouts](https://developer.android.com/develop/ui/compose/layouts/adaptive)
- [Android architecture guidance](https://developer.android.com/topic/architecture)
- [Offline-first data layer](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Room](https://developer.android.com/training/data-storage/room)
- [Android App Bundles](https://developer.android.com/guide/app-bundle)

## Layer boundaries

```text
Compose UI
  ↓ user intents / observable state
Application use cases
  ↓
Pure domain model and calculation policies
  ↓ ports
Repositories ── Room / files / importers / optional remote adapters
```

- **Domain** (`domain/`): money, five cash event types, balanced journals (`LedgerPolicy`), business plan and break-even (`PlanCalculator`, `ActualBepCalculator`), stock, cash-flow schedules. No Android dependency.
- **Data** (`data/`): Room entities and DAO, `FinanceRepository` as the single write funnel (every rule enforced inside `withTransaction`), migrations, legacy cleanups for old schemas and backups.
- **I/O** (`importer/`, `exporter/`, `backup/`, `sync/`): template import and export, PDF, encrypted backup, Drive client, sync framework.
- **Presentation:** Compose routes, state holders, formatting, adaptive layout, accessibility.

Import parsing never writes to the database. `FinanceWorkbookImporter` builds a preview from the Mankeu template only (DEC-012); `FinanceRepository.reviewImport` adds what only the database knows, and `commitImport` writes everything in one transaction under a recorded import batch.

## Presentation-layer cross-cutting concerns

Three concerns cut across every screen and are therefore owned by the shell rather than duplicated:

- **Appearance.** `MankeuTheme` interpolates every `ColorScheme` and `MankeuSemanticColors` token
  towards its target instead of swapping schemes, so a Terang/Gelap change animates across the whole
  hierarchy without a second subcomposition and without discarding UI state. Language is the
  opposite case: it goes through `Activity.recreate()` behind a loading overlay, because an
  in-composition locale override does not reach `Dialog`/`Popup` subcompositions (D-017). Both read
  the duration budget from `LocalMotion`, and `LocalMotion` itself is `MankeuMotion.resolve(...)` of
  the user's Gerakan setting and the system animation scale, so both the app control and the
  platform accessibility setting are honoured (D-025).
- **Guided tour targets.** `LocalTourTargets` carries a `TourTargetRegistry` that composables write
  their root-space bounds into via `Modifier.tourTarget(...)`. `SpotlightTour` reads it to punch a
  real cutout over live UI. The registry is write-only from the screens' point of view and costs
  nothing when no tour is running, which keeps the tour out of the screens' own logic.
- **System back.** A single `BackHandler` in the shell states the whole precedence order — modal
  tour, open overlay, non-start destination, then the platform default. Screens do not register
  their own handlers, so the order cannot drift with composition order.
- **Asking the user something.** Every modal is `MankeuSheet` — one `ModalBottomSheet` with the title
  at the top, scrolling content in the middle and the actions at the bottom, where a thumb is
  (D-023). There is no `AlertDialog` anywhere in `app/src/main`. The sheet's content column
  `animateContentSize`s, so a section opening inside it grows the sheet rather than snapping it, and
  its `SheetState` is created internally so the experimental Material 3 opt-in does not spread to
  every call site.

Presentation state that is not accounting data (theme, motion, language, local display name, whether
the tour has been seen) lives in `SharedPreferences` via `AppPreferencesState`, deliberately outside
the audited Room schema, its migrations, and its export/backup contract.

## Module shape

One Gradle module, `:app`, with packages `domain`, `data`, `importer`, `exporter`, `backup`, `sync`, `auth` and `presentation`. The dependency direction is kept by package: `domain` imports no Android or other app package. Split into Gradle modules only when build time or reuse calls for it.

## Local-first data ownership

- The local Room database is authoritative for normal operation.
- Core functions require no account, network, Firebase, Drive, or application server.
- UI observes local data; optional remote work writes back through explicit repositories rather than bypassing the database.
- Backups have a version, database/schema version, creation time, checksum, encryption metadata, and manifest of included businesses.
- Restore validates integrity and compatibility before changing the active database and retains a recovery copy where storage permits.

## Backup, Drive, sync, and authentication

- **Backup/restore** (implemented): `EncryptedBackupManager` snapshots every table in `AppDatabase.DATA_TABLES`, encrypts it with AES-256-GCM under a PBKDF2-HMAC-SHA256 key, and writes it to a user-chosen file or to Drive `appDataFolder` (latest five kept). Restore validates the schema (3 to current), keeps an encrypted recovery copy, and upgrades old snapshots with the same cleanups as the migrations.
- **Erase all data** (implemented, DEC-020): `FinanceRepository.eraseAllData` empties every data table and the sync state; `GoogleDriveBackupClient.deleteAllAppData` removes every file in the app folder; `FirebaseAuthManager.deleteAccount` removes the sign-in user.
- **Authentication** (implemented, optional): Firebase Auth with Google Sign-In, only to reach the user's Drive (D-033, D-040).
- **Synchronization** (framework, not wired): SQLite triggers record changes in `sync_outbox`; `SyncEngine` and `SyncCoordinator` exchange encrypted change sets over Drive with row-level last-writer-wins (DEC-013). Details and open owner questions in `docs/SYNC_ARCHITECTURE.md`.

## Security and privacy baseline

- No real workbook, database, research response, backup, credential, API key, signing key, service configuration, or token belongs in Git.
- Use Android Keystore-backed key protection for secrets where applicable; select the database/file encryption implementation only after a small threat model and maintenance review.
- Export and backup are explicit user actions with destination and sensitivity warnings.
- Logs redact descriptions, amounts where not required, file paths, tokens, and personal data.
- Analytics is off by default until events, consent, retention, and research separation are approved.
- Debug builds and test fixtures must not share production signing or cloud projects.

## Release format and platform requirements

- Use APKs for local installation and device testing.
- Publish new Google Play applications as Android App Bundles (`.aab`).
- As of the planning date, Google Play states that new apps and updates must target Android 16 / API level 36 from 31 August 2026. Reverify policy immediately before release: [target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878).
- Package `com.mankeu`, minSdk 26, targetSdk 36 are set. A representative low/mid-range device baseline still needs user-device evidence.
- Signing custody and Play Console ownership: see `docs/PLAY_STORE_DEPLOYMENT.md`.

## Architecture decisions

Recorded in `docs/DECISION_LOG.md` (D-001 to D-087) and, from DEC-001 on, in `logs/DECISION_LOG.md`. Still open: representative device baseline, analytics/crash reporting (off; none planned), signing custody and release ownership, and how sync is offered to users.
