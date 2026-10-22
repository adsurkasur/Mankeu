---
status: implemented (release feature, DEC-024)
last_verified: 2026-10-22
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner decision 2026-10-20 (DEC-013): multi-device sync over Google Drive, end-to-end encrypted
  - owner decisions 2026-10-22 (DEC-024): sync is a release feature; one Google account; passphrase once per phone; automatic + button; automatic merge with a notice
  - app/src/main/java/com/mankeu/sync/
  - app/src/androidTest/java/com/mankeu/sync/SyncEngineTest.kt
  - app/src/androidTest/java/com/mankeu/data/MigrationTest.kt
---

# Multi-device sync (Google Drive)

## Status, stated plainly

**Sync is a release feature (DEC-024).** The owner switches it on per phone in Pengaturan →
*Sinkronisasi antar-HP*. Not verified yet against a real Google account on two physical phones
(needs the owner's sign-in); verified with two devices over an in-memory store (`SyncEngineTest`).

| Piece | Where | State |
|---|---|---|
| Change capture | `sync/SyncSchema.kt`: triggers on every data table → `sync_outbox`, **only while `sync_meta.sync_enabled` is set** | schema 12 |
| Device identity | `sync_meta.device_id` (UUID per install) | live |
| Change set format | `PortableBackup` + `_op` / `_changedAt` columns + `_sync` header | done |
| Encryption | `PasswordBackupCipher` (AES-256-GCM, PBKDF2 310k), same as `.aabk` | done |
| Collect / apply, merge by name | `sync/SyncEngine.kt` | done, tested |
| One sync round | `sync/SyncCoordinator.kt` (pull others, then push own) | done, tested |
| Drive store | `GoogleDriveBackupClient.asSyncTransport` (appDataFolder, `appProperties.kind=sync`) | done; list parsing unit-tested |
| Passphrase | `sync/SyncPassphraseStore.kt`: AES-GCM under an Android Keystore key | done, tested |
| Trigger and status | `sync/SyncController.kt`, `AppViewModel` (start, 5 s after a change, button), `screens/SyncCard.kt` | done |

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
   device still has an unpushed change to the same row that is newer. A category, stock item or
   product created on two phones with the same natural key (`businessId+role+normalizedName`,
   `businessId+projectId+name`, `projectId+name`, case-insensitive) is merged: the incoming row is not
   inserted, `sync_meta` keeps `alias:<table>:<remoteId>` → local id, and `categoryId`, `productId` and
   `stockItemId` of incoming rows are rewritten through it, now and in later change sets. Merges,
   skipped edits and refused rows become one SYSTEM notice on the phone where they happened (written
   with capture off, so it is not sent on). Upserts are applied parents
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

## Decisions taken (DEC-024, 2026-10-22)

1. **Account.** All phones sign in with the same Google account (the business account). Sync uses
   that account's `appDataFolder`, so nothing is shared with anyone else and no broader Drive scope is
   needed.
2. **Passphrase.** Typed once per phone, at least 8 characters, the same on every phone; kept
   encrypted by a non-exportable Keystore key. Switching sync on checks it against a change set
   another phone already stored and refuses a mismatch before anything is written. Lost Keystore
   (new phone, reset) → the card asks to switch sync off and on again.
3. **Trigger.** On app start, 5 seconds after the last local change (debounced), and from
   "Sinkronkan sekarang". Automatic rounds are quiet; failures show in the card. No background work
   while the app is closed.
4. **Conflicts.** Automatic: last writer wins per row; equal names merge; a notice reports both.
5. **Capture only while on.** Schema 12 recreates the triggers with the `sync_enabled` condition and
   empties the outbox that grew on every phone before (about four rows per transaction, never read).
   Switching on sets `requires_full_snapshot`, so the first push carries every row; switching off
   clears the flag and the outbox.

Still open, with their ceilings:

- **Compaction.** Change sets accumulate in `appDataFolder`. Deleting old ones needs every phone's
  cursor, which phones do not publish yet. Ceiling: Drive storage of the owner's account; a year of a
  medium business is a few MB of compressed change sets.
- **Clock skew.** Last-writer-wins trusts phone clocks (second resolution). Fine for a few phones of
  one owner.

## Verification

- `SyncEngineTest` (instrumented): two devices exchange encrypted change sets through an
  in-memory store: inserts, deletes with cascades, no echo, last-writer-wins, wrong passphrase
  leaves the database untouched, the same category created offline on both phones is merged with its
  references, nothing is captured while sync is off.
- `SyncPassphraseStoreTest` (instrumented): Keystore round trip and clear.
- `MigrationTest.migrate11To12…`: outbox emptied, capture off until `sync_enabled`, on after.
- `MigrationTest` (instrumented): after 7→8, a write lands in `sync_outbox`; device id and
  full-snapshot flag exist.
- `GoogleDriveBackupClientTest` (unit): sync files are listed for sync and hidden from backup
  listing, so backup retention never prunes them.
