package com.mankeu.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.mankeu.domain.StockDao
import com.mankeu.domain.StockItemEntity
import com.mankeu.domain.StockMovementEntity
import com.mankeu.sync.SyncDao
import com.mankeu.sync.SyncMetaEntity
import com.mankeu.sync.SyncOutboxEntity
import com.mankeu.sync.SyncSchema

@Database(
    entities = [
        BusinessEntity::class,
        ProjectEntity::class,
        BookEntity::class,
        CategoryEntity::class,
        BudgetItemEntity::class,
        FinancialEventEntity::class,
        JournalEntryEntity::class,
        PostingEntity::class,
        ProductEntity::class,
        ProductCostComponentEntity::class,
        ImportBatchEntity::class,
        ImportNoticeEntity::class,
        ProvenanceEntity::class,
        StockItemEntity::class,
        StockMovementEntity::class,
        SyncOutboxEntity::class,
        SyncMetaEntity::class,
    ],
    version = 12,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
    abstract fun stockDao(): StockDao
    abstract fun syncDao(): SyncDao

    companion object {
        /**
         * Schema 4 drops the `units` master-data table and the `categories.defaultUnitId` column
         * that pointed into it (D-064). A unit is a word the owner types next to a quantity —
         * "kg", "botol", "porsi" — and keeping a per-business registry of those words bought
         * nothing: nothing read `defaultUnitId`, and the registry's only job was to populate a
         * dropdown the entry form does not use.
         *
         * Written as a real migration rather than a destructive fallback because v0.0.4 is on the
         * Play Store: from here on, a schema change must carry a user's books forward, not delete
         * them. Financial data is not something an app gets to drop because a table was retired.
         *
         * `categories` is recreated because SQLite before 3.35 cannot `DROP COLUMN`, and the
         * minimum supported API (26) ships older versions than that. Every remaining column is
         * copied across by name, and the two indices are rebuilt exactly as the entity declares.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `units`")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories_new` (
                        `id` TEXT NOT NULL,
                        `businessId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `normalizedName` TEXT NOT NULL,
                        `aliases` TEXT NOT NULL,
                        `templateOrigin` TEXT,
                        `active` INTEGER NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`businessId`) REFERENCES `businesses`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `categories_new`
                        (`id`, `businessId`, `role`, `name`, `normalizedName`, `aliases`,
                         `templateOrigin`, `active`, `createdAtEpochMillis`)
                    SELECT `id`, `businessId`, `role`, `name`, `normalizedName`, `aliases`,
                           `templateOrigin`, `active`, `createdAtEpochMillis`
                    FROM `categories`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `categories`")
                db.execSQL("ALTER TABLE `categories_new` RENAME TO `categories`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_businessId` ON `categories` (`businessId`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_businessId_role_normalizedName` " +
                        "ON `categories` (`businessId`, `role`, `normalizedName`)",
                )
            }
        }

        /**
         * Schema 5 makes `import_batches.sourceSha256` a plain index instead of a unique one.
         *
         * The unique index contradicted `hasImported`, which deliberately ignores reversed batches:
         * reversing an import and importing the same file again — the fix for landing it in the
         * wrong book — passed the duplicate check and then died on a raw
         * `UNIQUE constraint failed` inside the commit transaction (D-076).
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS `index_import_batches_sourceSha256`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_import_batches_sourceSha256` ON `import_batches` (`sourceSha256`)")
            }
        }

        /**
         * Schema 6 removes unused loan and interest columns from `scenario_assumptions`
         * in favor of explicit CVP variables (`fixedCostRupiah` and `variableCostPerUnitRupiah`),
         * and adds minimal inventory management tables (`stock_items` and `stock_movements`).
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rebuild scenario_assumptions without loanAmountRupiah and interestRateBasisPoints
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `scenario_assumptions_new` (
                        `id` TEXT NOT NULL,
                        `bookId` TEXT NOT NULL,
                        `openingCashRupiah` INTEGER NOT NULL,
                        `ownerCapitalRupiah` INTEGER NOT NULL,
                        `otherFinancingCostRupiah` INTEGER NOT NULL,
                        `outputQuantityText` TEXT,
                        `outputUnit` TEXT,
                        `outputPriceRupiah` INTEGER,
                        `fixedCostRupiah` INTEGER,
                        `variableCostPerUnitRupiah` INTEGER,
                        `updatedAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`bookId`) REFERENCES `books`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `scenario_assumptions_new`
                        (`id`, `bookId`, `openingCashRupiah`, `ownerCapitalRupiah`, `otherFinancingCostRupiah`,
                         `outputQuantityText`, `outputUnit`, `outputPriceRupiah`, `fixedCostRupiah`,
                         `variableCostPerUnitRupiah`, `updatedAtEpochMillis`)
                    SELECT `id`, `bookId`, `openingCashRupiah`, `ownerCapitalRupiah`, `otherFinancingCostRupiah`,
                           `outputQuantityText`, `outputUnit`, `outputPriceRupiah`, NULL, NULL, `updatedAtEpochMillis`
                    FROM `scenario_assumptions`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `scenario_assumptions`")
                db.execSQL("ALTER TABLE `scenario_assumptions_new` RENAME TO `scenario_assumptions`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_scenario_assumptions_bookId` " +
                        "ON `scenario_assumptions` (`bookId`)",
                )

                // Create stock tables
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stock_items` (
                        `id` TEXT NOT NULL,
                        `businessId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `unit` TEXT NOT NULL,
                        `basePriceRupiah` INTEGER NOT NULL,
                        `initialStockText` TEXT NOT NULL,
                        `currentStockText` TEXT NOT NULL,
                        `minAlertStockText` TEXT NOT NULL,
                        `archived` INTEGER NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL,
                        `updatedAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`businessId`) REFERENCES `businesses`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_items_businessId` ON `stock_items` (`businessId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stock_items_businessId_name` ON `stock_items` (`businessId`, `name`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stock_movements` (
                        `id` TEXT NOT NULL,
                        `stockItemId` TEXT NOT NULL,
                        `movementType` TEXT NOT NULL,
                        `quantityText` TEXT NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `referenceEventId` TEXT,
                        `createdAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`stockItemId`) REFERENCES `stock_items`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_stockItemId` ON `stock_movements` (`stockItemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_dateEpochDay` ON `stock_movements` (`dateEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_referenceEventId` ON `stock_movements` (`referenceEventId`)")
            }
        }

        /**
         * Schema 7 drops obligations and settlements tables entirely to enforce
         * a pure Cash-Only architecture (no debt, no interest, no credit sales/purchases).
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `settlements`")
                db.execSQL("DROP TABLE IF EXISTS `obligations`")
            }
        }

        /**
         * Schema 8, strict cash and sync groundwork (DEC-011, DEC-013):
         * - `import_notices.batchId` becomes nullable, so the app can raise notices of its own;
         * - `financial_events` loses `counterparty`, `dueDateEpochDay`, `obligationId`, and
         *   `scenario_assumptions` loses `otherFinancingCostRupiah` (all credit/loan leftovers);
         * - `stock_items` gains `importBatchId`, so reversing a template import removes its items;
         * - legacy non-cash rows are purged by [StrictCashCleanup], which leaves a notice;
         * - `sync_outbox` / `sync_meta` and their change-capture triggers are created.
         *
         * Room keeps foreign keys off during migrations, so the create-copy-drop-rename rebuilds
         * below do not cascade into `journal_entries`; Room checks the keys afterwards.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                rebuild(
                    db,
                    table = "import_notices",
                    create = "CREATE TABLE `import_notices_new` (`id` TEXT NOT NULL, `batchId` TEXT, `severity` TEXT NOT NULL, `title` TEXT NOT NULL, `explanation` TEXT NOT NULL, `amountRupiah` INTEGER, `sourceEvidence` TEXT NOT NULL, `reviewed` INTEGER NOT NULL, `action` TEXT, `createdAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`batchId`) REFERENCES `import_batches`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                    columns = "id,batchId,severity,title,explanation,amountRupiah,sourceEvidence,reviewed,action,createdAtEpochMillis",
                    indices = listOf("CREATE INDEX IF NOT EXISTS `index_import_notices_batchId` ON `import_notices` (`batchId`)"),
                )
                rebuild(
                    db,
                    table = "financial_events",
                    create = "CREATE TABLE `financial_events_new` (`id` TEXT NOT NULL, `businessId` TEXT NOT NULL, `projectId` TEXT NOT NULL, `bookId` TEXT NOT NULL, `type` TEXT NOT NULL, `amountRupiah` INTEGER NOT NULL, `dateEpochDay` INTEGER NOT NULL, `description` TEXT NOT NULL, `category` TEXT NOT NULL, `categoryId` TEXT, `reference` TEXT, `quantityText` TEXT, `unit` TEXT, `unitPriceRupiah` INTEGER, `budgetItemId` TEXT, `importBatchId` TEXT, `cashFlowRole` TEXT NOT NULL, `status` TEXT NOT NULL, `reversesEventId` TEXT, `reversedByEventId` TEXT, `createdAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`businessId`) REFERENCES `businesses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                    columns = "id,businessId,projectId,bookId,type,amountRupiah,dateEpochDay,description,category,categoryId,reference,quantityText,unit,unitPriceRupiah,budgetItemId,importBatchId,cashFlowRole,status,reversesEventId,reversedByEventId,createdAtEpochMillis",
                    indices = listOf("businessId", "projectId", "bookId", "categoryId", "budgetItemId", "importBatchId", "reversesEventId")
                        .map { "CREATE INDEX IF NOT EXISTS `index_financial_events_$it` ON `financial_events` (`$it`)" },
                )
                rebuild(
                    db,
                    table = "scenario_assumptions",
                    create = "CREATE TABLE `scenario_assumptions_new` (`id` TEXT NOT NULL, `bookId` TEXT NOT NULL, `openingCashRupiah` INTEGER NOT NULL, `ownerCapitalRupiah` INTEGER NOT NULL, `outputQuantityText` TEXT, `outputUnit` TEXT, `outputPriceRupiah` INTEGER, `fixedCostRupiah` INTEGER, `variableCostPerUnitRupiah` INTEGER, `updatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                    columns = "id,bookId,openingCashRupiah,ownerCapitalRupiah,outputQuantityText,outputUnit,outputPriceRupiah,fixedCostRupiah,variableCostPerUnitRupiah,updatedAtEpochMillis",
                    indices = listOf("CREATE UNIQUE INDEX IF NOT EXISTS `index_scenario_assumptions_bookId` ON `scenario_assumptions` (`bookId`)"),
                )

                db.execSQL("ALTER TABLE `stock_items` ADD COLUMN `importBatchId` TEXT")

                StrictCashCleanup.run(db, source = "Pembaruan basis data ke skema 8")

                db.execSQL("CREATE TABLE IF NOT EXISTS `sync_outbox` (`seq` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tableName` TEXT NOT NULL, `rowId` TEXT NOT NULL, `op` TEXT NOT NULL, `changedAtEpochMillis` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_outbox_tableName_rowId` ON `sync_outbox` (`tableName`, `rowId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `sync_meta` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))")
                SyncSchema.createTriggers(db)
                SyncSchema.ensureDeviceId(db)
                // Everything already on the device predates change capture.
                db.execSQL("INSERT OR REPLACE INTO sync_meta(`key`, value) VALUES ('${SyncSchema.REQUIRES_FULL_SNAPSHOT}', '1')")
            }
        }

        /**
         * Schema 9, business plan and itemised BEP (DEC-014):
         * - cost nature on categories, plan lines and transactions; recurrence and asset life on
         *   plan lines; product and asset life on transactions (all nullable, no table rebuild);
         * - `products` and `product_cost_components`;
         * - the old `scenario_assumptions` calculator converted by [LegacyPlanConversion], then dropped.
         * New tables get change-capture triggers; ALTER TABLE ADD COLUMN keeps the existing ones.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `costNature` TEXT")
                db.execSQL("ALTER TABLE `budget_items` ADD COLUMN `costNature` TEXT")
                db.execSQL("ALTER TABLE `budget_items` ADD COLUMN `recurrence` TEXT")
                db.execSQL("ALTER TABLE `budget_items` ADD COLUMN `usefulLifeMonths` INTEGER")
                db.execSQL("ALTER TABLE `financial_events` ADD COLUMN `costNature` TEXT")
                db.execSQL("ALTER TABLE `financial_events` ADD COLUMN `productId` TEXT")
                db.execSQL("ALTER TABLE `financial_events` ADD COLUMN `usefulLifeMonths` INTEGER")
                db.execSQL("CREATE TABLE IF NOT EXISTS `products` (`id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `name` TEXT NOT NULL, `unit` TEXT NOT NULL, `priceRupiah` INTEGER NOT NULL, `targetPerMonthText` TEXT, `sortOrder` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, `importBatchId` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_projectId` ON `products` (`projectId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `product_cost_components` (`id` TEXT NOT NULL, `productId` TEXT NOT NULL, `name` TEXT NOT NULL, `amountRupiah` INTEGER NOT NULL, `yieldText` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_cost_components_productId` ON `product_cost_components` (`productId`)")
                // Before the conversions below, so the rows they write reach the sync outbox too.
                SyncSchema.createTriggers(db)

                LegacyPlanConversion.applyDefaultCostNatures(db)
                val rows = db.query("SELECT * FROM `scenario_assumptions`").use { c ->
                    buildList {
                        while (c.moveToNext()) {
                            add((0 until c.columnCount).associate { i ->
                                c.getColumnName(i) to when (c.getType(i)) {
                                    android.database.Cursor.FIELD_TYPE_NULL -> null
                                    android.database.Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                                    else -> c.getString(i)
                                }
                            })
                        }
                    }
                }
                LegacyPlanConversion.convertAssumptions(db, rows, source = "Pembaruan basis data ke skema 9")
                db.execSQL("DROP TABLE `scenario_assumptions`")
            }
        }

        /**
         * Schema 10: products may start selling later in the plan (seasonal), an import batch
         * remembers the plan period it filled in and its money in, and stock can belong to one unit. Existing stock
         * stays shared (projectId NULL): which branch holds it is not something to guess.
         * Columns are added in place, so sync triggers (which only read `id`) stay valid.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `products` ADD COLUMN `sellFromMonth` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `import_batches` ADD COLUMN `appliedStartEpochDay` INTEGER")
                db.execSQL("ALTER TABLE `import_batches` ADD COLUMN `appliedEndEpochDay` INTEGER")
                db.execSQL("ALTER TABLE `import_batches` ADD COLUMN `incomingRupiah` INTEGER")
                db.execSQL("ALTER TABLE `stock_items` ADD COLUMN `projectId` TEXT")
                db.execSQL("DROP INDEX IF EXISTS `index_stock_items_businessId_name`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stock_items_businessId_projectId_name` ON `stock_items` (`businessId`, `projectId`, `name`)")
            }
        }

        /**
         * Schema 11, Ubah and Hapus replace "Balik Transaksi" (DEC-019): [ReversalCleanup] deletes
         * every reversal and the original it undid (each pair nets to zero), then `financial_events`
         * loses `status`/`reversesEventId`/`reversedByEventId` and `journal_entries` loses
         * `reversalOfJournalId`. The rebuilds drop the change-capture triggers, so they are recreated.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ReversalCleanup.run(db, source = "Pembaruan basis data ke skema 11")
                rebuild(
                    db,
                    table = "financial_events",
                    create = "CREATE TABLE `financial_events_new` (`id` TEXT NOT NULL, `businessId` TEXT NOT NULL, `projectId` TEXT NOT NULL, `bookId` TEXT NOT NULL, `type` TEXT NOT NULL, `amountRupiah` INTEGER NOT NULL, `dateEpochDay` INTEGER NOT NULL, `description` TEXT NOT NULL, `category` TEXT NOT NULL, `categoryId` TEXT, `reference` TEXT, `quantityText` TEXT, `unit` TEXT, `unitPriceRupiah` INTEGER, `budgetItemId` TEXT, `importBatchId` TEXT, `cashFlowRole` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `costNature` TEXT, `productId` TEXT, `usefulLifeMonths` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`businessId`) REFERENCES `businesses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                    columns = "id,businessId,projectId,bookId,type,amountRupiah,dateEpochDay,description,category,categoryId,reference,quantityText,unit,unitPriceRupiah,budgetItemId,importBatchId,cashFlowRole,createdAtEpochMillis,costNature,productId,usefulLifeMonths",
                    indices = listOf("businessId", "projectId", "bookId", "categoryId", "budgetItemId", "importBatchId")
                        .map { "CREATE INDEX IF NOT EXISTS `index_financial_events_$it` ON `financial_events` (`$it`)" },
                )
                rebuild(
                    db,
                    table = "journal_entries",
                    create = "CREATE TABLE `journal_entries_new` (`id` TEXT NOT NULL, `sourceEventId` TEXT NOT NULL, `dateEpochDay` INTEGER NOT NULL, `description` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`sourceEventId`) REFERENCES `financial_events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                    columns = "id,sourceEventId,dateEpochDay,description",
                    indices = listOf("CREATE UNIQUE INDEX IF NOT EXISTS `index_journal_entries_sourceEventId` ON `journal_entries` (`sourceEventId`)"),
                )
                SyncSchema.createTriggers(db)
            }
        }

        /**
         * Schema 12, sync becomes a release feature (DEC-024): change capture runs only while sync is
         * switched on, so the triggers are recreated with that condition and the outbox written so
         * far, which nothing ever read, is emptied. No table changes.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                SyncSchema.createTriggers(db)
                db.execSQL("DELETE FROM sync_outbox")
            }
        }

        private fun rebuild(db: SupportSQLiteDatabase, table: String, create: String, columns: String, indices: List<String>) {
            db.execSQL(create)
            db.execSQL("INSERT INTO `${table}_new` ($columns) SELECT $columns FROM `$table`")
            db.execSQL("DROP TABLE `$table`")
            db.execSQL("ALTER TABLE `${table}_new` RENAME TO `$table`")
            indices.forEach(db::execSQL)
        }

        /** Every table holding user data, parents before children. Backup and sync both walk it. */
        val DATA_TABLES = listOf(
            "businesses",
            "projects",
            "books",
            "categories",
            "import_batches",
            "budget_items",
            "financial_events",
            "journal_entries",
            "postings",
            "import_notices",
            "provenance_records",
            "stock_items",
            "stock_movements",
            "products",
            "product_cost_components",
        )

        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "mankeu.db",
        )
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
            .addCallback(SyncSchema.callback)
            .build()
    }
}
