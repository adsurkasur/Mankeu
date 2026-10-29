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

Since phase 0, `fallbackToDestructiveMigrationFrom(1..11)` is configured for schemas 1 to 11 only (operator-reported: the app never shipped them to real users beyond testers; the removed KDoc said v0.0.4 was on the Play Store, so such installs are wiped without an on-screen notice). From schema 12 on there is no fallback: an upgrade without a matching migration crashes on open instead of deleting a user's books, and that crash is caught in development by installing over existing data.

## 12 → 13 (Fase 1)

`Migration12To13` (`app/src/main/java/com/mankeu/data/Migration12To13.kt`), DEC-028:

- **New tables.** `product_units` (sale units: `name`, `contentText` base units per one, `priceRupiah`, `sortOrder`; the first is the main unit), `product_recipe_lines` (per-batch recipe: a material `stockItemId` + `quantityText`, or a free `amountRupiah`; no foreign key to `stock_items`, so a removed material leaves its line and the HPP shows as incomplete) and `product_movements` (production and stock counts in base units; sales are read from the sales themselves). All three cascade from `products`.
- **`products` rebuilt.** `priceRupiah` moves to the first sale unit; `batchYieldText` (default `'1'`) is added; `unit` is now the base unit. The rebuild drops the table's sync triggers, so the migration ends with `SyncSchema.createTriggers` (which also covers the three new tables).
- **`financial_events.stockItemId`** (nullable, indexed): the material an expense bought (spec #20). Added with `ALTER TABLE`.
- **Old products** go through `LegacyProductShape` (`app/src/main/java/com/mankeu/data/LegacyProductShape.kt`): a batch of 1 base unit; the old unit and price become sale unit `<productId>-u0` with content 1; each component "Rp X covers N units" becomes a free recipe line of `round(X ÷ N)`, half up, keeping the component's id and order. The rounding moves a product's HPP by at most Rp0,5 per component per unit (18.000 for 6 → 3.000; 10.000 for 3 → 3.333). Ids come from the data, so two phones that migrate on their own produce the same rows. Only the migration and `BackupUpgrade` use this adapter now; template v4 files are rejected.
- **Backups.** `OLDEST_RESTORABLE_SCHEMA` stays 12. `EncryptedBackupManager.prepareRestore` passes every snapshot through `BackupUpgrade.toCurrent`, which reshapes a schema-12 snapshot exactly as the migration does (`product_cost_components` removed; `product_units`, `product_recipe_lines` and an empty `product_movements` added). The products rows keep their `priceRupiah` column; the restore drops columns the live table no longer has, and `batchYieldText` takes its SQL default. Verified by `BackupUpgradeTest`.
- **Sync between versions (DEC-028 §7).** A phone accepts a change set only from the same or an older schema (`schemaVersion <= own`, existing behaviour): a schema-12 phone refuses every schema-13 change set until it is updated, and a schema-13 phone accepts schema-12 change sets but drops their `product_cost_components` rows and the products' `priceRupiah`, so product edits made on an old phone during the transition lose their sale units and recipe on the new one. Release note: update every phone. Migrated rows are not put in the outbox (the triggers are created after the inserts); both phones derive the same rows anyway.

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

The current build (schema 13; a schema-12 backup is upgraded by `BackupUpgrade`) refuses every backup older than schema 12, so the rows above and the notes below only describe the old behaviour. Previously it restored all three rows above, plus schema 6 to 11 backups. A backup older than schema 10 restores with the new columns at their SQL defaults: products sell from month 1, stock is shared, batches have no period to undo.

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
