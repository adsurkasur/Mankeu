---
status: framework-implemented-not-wired
last_verified: 2026-10-20
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner decision 2026-10-20 (DEC-013): multi-device sync over Google Drive, end-to-end encrypted
  - app/src/main/java/com/mankeu/sync/
  - app/src/androidTest/java/com/mankeu/sync/SyncEngineTest.kt
  - app/src/androidTest/java/com/mankeu/data/MigrationTest.kt
---

# Multi-device sync (Google Drive)

## Status, stated plainly

The **framework is built and tested; the feature is not switched on.** Nothing in the UI calls it,
nothing schedules it, and no change set is ever uploaded unless code calls
`SyncCoordinator.syncOnce`. What exists:

| Piece | Where | State |
|---|---|---|
| Change capture | `sync/SyncSchema.kt`: SQLite triggers on every data table → `sync_outbox` | live since schema 8 |
| Device identity | `sync_meta.device_id` (UUID per install) | live |
| Change set format | `PortableBackup` + `_op` / `_changedAt` columns + `_sync` header | done |
| Encryption | `PasswordBackupCipher` (AES-256-GCM, PBKDF2 310k), same as `.aabk` | done |
| Collect / apply | `sync/SyncEngine.kt` | done, tested |
| One sync round | `sync/SyncCoordinator.kt` (pull others, then push own) | done, tested with in-memory store |
| Drive store | `GoogleDriveBackupClient.asSyncTransport` (appDataFolder, `appProperties.kind=sync`) | done, list parsing unit-tested; not exercised against real Drive |
| Passphrase onboarding, trigger, status UI | — | **open (see below)** |

## How it works

1. **Capture.** Every INSERT/UPDATE/DELETE on a data table (`AppDatabase.DATA_TABLES`) appends
   `(table, rowId, op, changedAt)` to `sync_outbox` through a trigger. App code never writes the
   outbox, so imports, restores and future code cannot forget. Foreign-key cascades fire triggers
   too, so deleting an event also records its journal and postings.
2. **Collect.** `SyncEngine.collectLocalChanges` collapses the outbox to the newest entry per row,
   reads each row's current values (or emits a DELETE when it is gone) and builds one change set
   with an increasing per-device `pushNumber`. When `sync_meta.requires_full_snapshot` is set
   (after the schema-8 migration and after every restore) it emits every row instead.
3. **Encrypt and push.** The change set is encoded with `PortableBackupCodec`, encrypted with the
   user's sync passphrase, and uploaded as `mankeu-sync-<device>-<push>.mks` with
   `appProperties {kind: sync, deviceId, pushNumber}`. Drive only ever stores ciphertext.
4. **Pull and apply.** Other devices list sync files, skip their own, download any
   `pushNumber` above their cursor for that device, decrypt, check the header matches the file
   name, and apply in push order inside one transaction with change capture **off**
   (`SyncSchema.quietly`), so applied changes never echo back.
5. **Conflicts.** Row-level last-writer-wins on `changedAt`: a remote change is skipped when this
   device still has an unpushed change to the same row that is newer. Upserts are applied parents
   first as UPDATE-then-INSERT (never `INSERT OR REPLACE`, which would cascade-delete journals);
   deletes children first. A row a constraint refuses is skipped and counted (`rejected`).

### Why ledger integrity survives

An event, its journal and its postings are written in one Room transaction, so they reach the
outbox together and travel in the same change set; the set is applied in one transaction. A
journal can therefore never arrive without its postings. Since DEC-019 a record is edited in
place (same id), so two devices editing the same record is a real conflict for the sync merge to resolve.

## Rules for future changes

- **A migration that rebuilds a data table (create-copy-drop-rename) drops its triggers.** It must
  call `SyncSchema.createTriggers(db)` afterwards. `MigrationTest` checks capture is live after 7→8;
  extend it for every new migration.
- A new data table must be added to `AppDatabase.DATA_TABLES` (backup and sync both walk it) and
  must have a TEXT `id` primary key. The migration that creates it must call
  `SyncSchema.createTriggers(db)`; `createTriggers` skips tables that do not exist yet, so a
  chained upgrade (7→8→9) never fails on a table from the future. Schema 9 added `products` and
  `product_cost_components` this way and dropped `scenario_assumptions`.
- `sync_outbox` and `sync_meta` are device-local: never backed up, never synced.
- A change set from a newer schema is refused ("perbarui aplikasi"); an older one is applied by
  live column name, like backup restore.

## Open decisions (owner) before switching it on

1. **Passphrase handling.** Ask once per device and keep it wrapped by Android Keystore, or ask on
   every sync? The Drive design already requires the passphrase to be unrecoverable if lost.
2. **Trigger.** A "Sinkronkan" button only (consistent with "nothing runs in the background without
   being asked" in `CLOUD_SYNC_AND_AUTH.md`), or also on app start / WorkManager when online.
3. **Rejected rows.** Most likely case: two devices create the same category name offline. Options:
   merge by normalized name on apply, or surface them for the owner to merge (F-10 merge feature).
4. **Compaction.** Change sets accumulate. Proposal: after a full snapshot is uploaded and every
   known device's cursor has passed it, delete older sets.
5. **Clock skew.** Last-writer-wins trusts device clocks; equal timestamps (second resolution in
   the trigger) apply the remote change. Acceptable for a single owner on 2–3 phones; revisit if
   shared editing is ever wanted.

## Verification

- `SyncEngineTest` (instrumented): two devices exchange encrypted change sets through an
  in-memory store: inserts, deletes with cascades, no echo, last-writer-wins, wrong passphrase
  leaves the database untouched.
- `MigrationTest` (instrumented): after 7→8, a write lands in `sync_outbox`; device id and
  full-snapshot flag exist.
- `GoogleDriveBackupClientTest` (unit): sync files are listed for sync and hidden from backup
  listing, so backup retention never prunes them.
