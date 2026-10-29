# Database Migrations and Backup Compatibility

The Room database is `mankeu.db`, class `com.mankeu.data.AppDatabase`. Exported schemas live in `app/schemas/com.mankeu.data.AppDatabase/<version>.json` and must be committed with every schema change.

> **Schema 12 is the floor (DEC-026, DEC-027).** The migrations 3→11 and their cleanups (`StrictCashCleanup`, `ReversalCleanup`, `LegacyPlanConversion`) were removed. Installs older than schema 12 are recreated empty (`fallbackToDestructiveMigrationFrom(1..11)`), and backups older than schema 12 are refused (`OLDEST_RESTORABLE_SCHEMA = 12`). The version history and the compatibility notes below are history of how those schemas behaved, not current behaviour.

## Version history

| Schema | Shipped in | Change | Migration |
| :-: | :-- | :-- | :-- |
| 1–2 | pre-release | Initial ledger, then books, budgets and imports | Destructive (no user data existed yet) |
| 3 | v0.0.4 | Baseline of the first Play Store build (internal testing track) | — |
| 4 | v0.0.5 | `units` table and `categories.defaultUnitId` removed (D-064) | `MIGRATION_3_4`: drop `units`, rebuild `categories` without the column, recreate both indices |
| 5 | v0.0.6 | `import_batches.sourceSha256` index is no longer unique (D-076) | `MIGRATION_4_5`: drop the unique index, create a plain index of the same name |
| 6 | unreleased | Loan/interest columns removed from `scenario_assumptions`; nullable CVP inputs `fixedCostRupiah` and `variableCostPerUnitRupiah` added; minimal inventory tables `stock_items` and `stock_movements` added (DEC-007, DEC-009) | `MIGRATION_5_6`: rebuild `scenario_assumptions` copying every surviving column (CVP inputs start `NULL`), create both stock tables and their indices |
| 7 | unreleased | Pure-cash model: `obligations` and `settlements` dropped (DEC-006) | `MIGRATION_6_7`: `DROP TABLE IF EXISTS` for both. Existing `financial_events` rows are kept unchanged |
| 8 | unreleased | Strict cash + sync groundwork (DEC-011, DEC-013): non-cash rows purged; `financial_events.counterparty/dueDateEpochDay/obligationId` and `scenario_assumptions.otherFinancingCostRupiah` dropped; `import_notices.batchId` nullable; `stock_items.importBatchId` added; category roles `EQUITY`→`CAPITAL_IN`, unused loan/interest categories archived; `sync_outbox`, `sync_meta` and change-capture triggers added | `MIGRATION_7_8`: rebuild the three tables, `ALTER TABLE ADD COLUMN`, `StrictCashCleanup.run` (leaves a notice), create sync tables + `SyncSchema.createTriggers`. Verified by `MigrationTest` and on the emulator's real schema-7 data |
| 9 | unreleased | Business plan (DEC-014): `products` and `product_cost_components` added; `costNature` on categories, budget items and events; `recurrence` and `usefulLifeMonths` on budget items; `productId` and `usefulLifeMonths` on events; `scenario_assumptions` dropped after its rows are converted | `MIGRATION_8_9`: `ALTER TABLE ADD COLUMN`, create both tables, `SyncSchema.createTriggers` **before** the conversion so converted rows reach the outbox, `LegacyPlanConversion` (default cost natures by category name; a Proyeksi calculator row becomes product "Produk utama" + recipe line, a monthly FIXED plan line and a capital line; opening cash is only named in a SYSTEM notice), then drop the table. Verified by `MigrationTest.migrate8To9…` and on the emulator's real schema-8 data |
| 10 | unreleased | Seasonal sales, period undo, per-unit stock (DEC-017): `products.sellFromMonth` (default 1); `import_batches.appliedStartEpochDay/appliedEndEpochDay` (the plan period an import filled in) and `incomingRupiah` (money in, NULL on older batches); `stock_items.projectId` (NULL = shared by every unit), unique index `(businessId, projectId, name)` replaces `(businessId, name)` | `MIGRATION_9_10`: `ALTER TABLE ADD COLUMN` ×5, drop and recreate the stock name index. Existing stock stays shared: which branch holds it cannot be guessed. Sync triggers read only `id`, so they stay valid. Verified by `MigrationTest.migrate9To10…` and by a real 7→10 upgrade on the emulator (schema-7 data from v0.0.6) |
| 11 | unreleased | Ubah and Hapus replace "Balik Transaksi" (DEC-019): every reversal pair is deleted; `financial_events.status/reversesEventId/reversedByEventId` and `journal_entries.reversalOfJournalId` (with their indices) dropped | `MIGRATION_10_11`: `ReversalCleanup.run` deletes each `REVERSAL` row and the original it undid, with journals, postings and provenance (each pair nets to zero, so cash and reports do not change) and leaves a SYSTEM notice; rebuild both tables, then `SyncSchema.createTriggers`. Verified by `MigrationTest.migrate10To11…` |
| 12 | unreleased | Sync becomes a release feature (DEC-024): change capture only while `sync_meta.sync_enabled` is set | `MIGRATION_11_12`: `SyncSchema.createTriggers` with the new condition, `DELETE FROM sync_outbox`. No table changes; backups of any earlier schema restore as before. Verified by `MigrationTest.migrate11To12…` |
| 13 | unreleased (Fase 1) | Product model (DEC-028, spec #5 #6 #9 #20): `product_cost_components` replaced by `product_units`, `product_recipe_lines`, `product_movements`; `products.priceRupiah` dropped, `products.batchYieldText` added; `financial_events.stockItemId` added | `Migration12To13`, see "12 → 13 (Fase 1)" below. Verified by `Migration12To13Test` |
| 14 | unreleased (Fase 2) | Notes, receivables, supplier credit, consignment (DEC-029): `contacts`, `sales`, `sale_lines`, `consignments`, `consignment_lines` added; `financial_events.saleId/contactId/dueDateEpochDay/settlesEventId` added (indexed); `product_movements.consignmentId` added; old product cash sales become paid notes | `Migration13To14`, see "13 → 14 (Fase 2)" below. Verified by `Migration13To14Test` and `BackupUpgradeTest` |

Since phase 0, `fallbackToDestructiveMigrationFrom(1..11)` is configured for schemas 1 to 11 only (operator-reported: the app never shipped them to real users beyond testers; the removed KDoc said v0.0.4 was on the Play Store, so such installs are wiped without an on-screen notice). From schema 12 on there is no fallback: an upgrade without a matching migration crashes on open instead of deleting a user's books, and that crash is caught in development by installing over existing data.

## 12 → 13 (Fase 1)

`Migration12To13` (`app/src/main/java/com/mankeu/data/Migration12To13.kt`), DEC-028:

- **New tables.** `product_units` (sale units: `name`, `contentText` base units per one, `priceRupiah`, `sortOrder`; the first is the main unit), `product_recipe_lines` (per-batch recipe: a material `stockItemId` + `quantityText`, or a free `amountRupiah`; no foreign key to `stock_items`, so a removed material leaves its line and the HPP shows as incomplete) and `product_movements` (production and stock counts in base units; sales are read from the sales themselves). All three cascade from `products`.
- **`products` rebuilt.** `priceRupiah` moves to the first sale unit; `batchYieldText` (default `'1'`) is added; `unit` is now the base unit. The rebuild drops the table's sync triggers, so the migration ends with `SyncSchema.createTriggers` (which also covers the three new tables).
- **`financial_events.stockItemId`** (nullable, indexed): the material an expense bought (spec #20). Added with `ALTER TABLE`.
- **Old products** go through `LegacyProductShape` (`app/src/main/java/com/mankeu/data/LegacyProductShape.kt`): a batch of 1 base unit; the old unit and price become sale unit `<productId>-u0` with content 1; each component "Rp X covers N units" becomes a free recipe line of `round(X ÷ N)`, half up, keeping the component's id and order. The rounding moves a product's HPP by at most Rp0,5 per component per unit (18.000 for 6 → 3.000; 10.000 for 3 → 3.333). Ids come from the data, so two phones that migrate on their own produce the same rows. Only the migration and `BackupUpgrade` use this adapter now; template v4 files are rejected.
- **Backups.** `OLDEST_RESTORABLE_SCHEMA` stays 12. `EncryptedBackupManager.prepareRestore` passes every snapshot through `BackupUpgrade.toCurrent`, which reshapes a schema-12 snapshot exactly as the migration does (`product_cost_components` removed; `product_units`, `product_recipe_lines` and an empty `product_movements` added). The products rows keep their `priceRupiah` column; the restore drops columns the live table no longer has, and `batchYieldText` takes its SQL default. Verified by `BackupUpgradeTest`.
- **Sync between versions (DEC-028 §7).** A phone accepts a change set only from the same or an older schema (`schemaVersion <= own`, existing behaviour): a schema-12 phone refuses every schema-13 change set until it is updated, and a schema-13 phone accepts schema-12 change sets but drops their `product_cost_components` rows and the products' `priceRupiah`, so product edits made on an old phone during the transition lose their sale units and recipe on the new one. Release note: update every phone. Migrated rows are not put in the outbox (the triggers are created after the inserts); both phones derive the same rows anyway.

## 13 → 14 (Fase 2)

`Migration13To14` (`app/src/main/java/com/mankeu/data/Migration13To14.kt`), DEC-029:

- **New tables.** `contacts` (per business, unique `(businessId, normalizedName)`), `sales` and `sale_lines` (a note and its lines with unit, content, price, total and an HPP snapshot per line), `consignments` and `consignment_lines` (goods left at a store, priced per sale unit). Notes and consignments cascade from `projects`, lines from their parent. Created with the exact `createSql` of `14.json`.
- **New columns** (`ALTER TABLE`, all nullable): `financial_events.saleId` (payment of a note), `contactId`, `dueDateEpochDay`, `settlesEventId` (payment of a credit purchase), each with an index; `product_movements.consignmentId` (the consignment a `CONSIGN_RETURN` came back from).
- **Old product sales (F2-4)** go through `LegacySaleShape` (`app/src/main/java/com/mankeu/data/LegacySaleShape.kt`): every Realisasi `CASH_INCOME` with a `productId` gets note `<eventId>-s` (date and timestamps of the event, `hppEstimated = 1`, no customer) and one line `<eventId>-l` (event unit or the main sale unit, its content, event quantity or 1, event unit price or amount ÷ quantity, total = amount, HPP = recipe cost now over content × quantity, material prices derived as in `FinanceRepository`). The event gets `saleId`, and its postings are rebuilt by `LedgerPolicy` as Kas / Piutang. Proyeksi sales, non-product income and purchases are untouched. The conversion runs inside `SyncSchema.quietly` (no outbox rows; both phones derive the same rows), then `SyncSchema.createTriggers` adds the five new tables' triggers.
- **Backups.** `BackupUpgrade.toCurrent` chains 12 → 13 → 14; the 13 → 14 step does the same conversion on the snapshot (adds `saleId` to the event rows, replaces the postings of converted journals, adds `sales` and `sale_lines`, empty `contacts`, `consignments`, `consignment_lines`). Verified by `BackupUpgradeTest` (13 → 14 and 12 → 14).
- **Sync between versions.** A schema-13 phone refuses schema-14 change sets until updated (existing rule), and a schema-14 phone refuses change sets older than schema 14 (`SyncEngine.MIN_SYNC_SCHEMA`, DEC-029): an old phone's product sales have no note and its edits would put Kas / Pendapatan back on migrated payments. Release note: **sync every phone, then update every phone before syncing again.** A change set below schema 14 is skipped, not a wall: `SyncCoordinator` consumes it unapplied (cursor moves past it), raises one SYSTEM notice and the message "Perubahan dari HP yang belum diperbarui dilewati; data lengkapnya akan datang setelah HP itu diperbarui.", and applies later change sets. A change set newer than the phone's own schema still stops the round (that phone must update). `Migration13To14` sets `requires_full_snapshot`, so every upgraded phone republishes its whole data, converted, which brings back what its skipped old change sets carried (deletions in them are not replayed). Restore already sets the same flag.
- **Undo import.** A migrated note keeps its sale event's `importBatchId`; `undoImportBatch` deletes the batch's notes.
- **Backup capture** reads every table `ORDER BY rowid`, so the 13 → 14 upgrade of a snapshot breaks purchase ties like the migration.
- **Interim (until Fase 2 Task 4):** profit on the Ringkasan screen still comes from postings, where migrated sales now credit Piutang instead of revenue.

## Re-import and upgrade notes

- **Re-importing a file.** A batch keeps its `import_batches` row after it is undone (`undoImportBatch` since schema 11; the stored state is still spelled `REVERSED`) as audit history. Duplicate protection is `FinanceDao.hasImported`, which only counts `COMMITTED` batches. The same workbook can therefore be imported again once its earlier batch is undone, and several rows may share a `sourceSha256`.
- **Upgrade verified on device (schema 5).** Installing v0.0.6 over a live schema-4 book gave `PRAGMA user_version` 5 with the index recreated non-unique. Transactions (59), cash (Rp29.050.000) and unbalanced journals (0) matched before and after.

## Backup compatibility

`EncryptedBackupManager` restores a backup whose schema is between `OLDEST_RESTORABLE_SCHEMA` (12; was 3 before phase 0) and the app's current schema, inclusive.

| Backup made on | Restores on v0.0.4 | v0.0.5 | v0.0.6 |
| :-- | :-: | :-: | :-: |
| v0.0.4 (schema 3) | yes | **no** | yes |
| v0.0.5 (schema 4) | no | yes | yes |
| v0.0.6 (schema 5) | no | no | yes |

The current build (schema 14; schema-12 and schema-13 backups are upgraded by `BackupUpgrade`) refuses every backup older than schema 12, so the rows above and the notes below only describe the old behaviour. Previously it restored all three rows above, plus schema 6 to 11 backups. A backup older than schema 10 restores with the new columns at their SQL defaults: products sell from month 1, stock is shared, batches have no period to undo.

- An older snapshot is restored by inserting only the columns the live table still has. Tables a later schema retired (`units`, `obligations`, `settlements`) are ignored.
- A snapshot older than schema 11 may hold "Balik Transaksi" pairs. Their columns no longer exist, so `EncryptedBackupManager` finds the pairs in the snapshot itself and passes their ids to `ReversalCleanup.delete` after restore (same deletion and notice as `MIGRATION_10_11`).
- A snapshot older than schema 9 gets the same `LegacyPlanConversion` as `MIGRATION_8_9` after restore: default cost natures, and its `scenario_assumptions` rows become plan lines and a product. `products` and `product_cost_components` are optional before schema 9.
- A snapshot older than schema 8 is passed through `StrictCashCleanup` after restore, so an old backup cannot bring back credit/loan rows. Every restore clears `sync_outbox` and sets `requires_full_snapshot`.
- Tables a later schema added (`stock_items`, `stock_movements`, schema 6) are optional in a backup older than schema 6 and restore empty. From schema 6 on they are required.
- A newer snapshot is refused: an older app cannot know what its extra columns mean.
- v0.0.5 refused schema-3 backups. That was the bug fixed in D-080, so users holding a v0.0.4 backup should restore it on v0.0.6.
- Every restore first writes an encrypted recovery copy of the current data, then replaces all tables in one transaction.

## Adding a migration

> Since schema 8: if the migration rebuilds a data table, call `SyncSchema.createTriggers(db)` afterwards, or that table stops recording changes for sync. A migration that adds a data table must call it too: `createTriggers` only covers the `DATA_TABLES` that already exist, because an older migration (7→8) runs against the schema of its own time. See `docs/SYNC_ARCHITECTURE.md`.

1. Bump `version` in `@Database` and add `MIGRATION_N_N+1` to `addMigrations`.
2. Build once so Room exports `N+1.json`, and commit it.
3. If the migration **reshapes** a table, a restore of an older backup can no longer just drop columns. Teach `BackupUpgrade` the same reshaping (as for 12 → 13), or raise `OLDEST_RESTORABLE_SCHEMA`.
4. Verify by installing the new build over a device that holds data at the previous schema. Compare row counts, cash and unbalanced journals before and after.
5. Record the change in this file, `docs/DECISION_LOG.md`, `docs/DOMAIN_MODEL.md` and `docs/diagrams/01_entity_relationship_diagram.md`.
