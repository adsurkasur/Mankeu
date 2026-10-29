package com.mankeu.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
        ProductUnitEntity::class,
        ProductRecipeLineEntity::class,
        ProductMovementEntity::class,
        ContactEntity::class,
        SaleEntity::class,
        SaleLineEntity::class,
        ConsignmentEntity::class,
        ConsignmentLineEntity::class,
        ImportBatchEntity::class,
        ImportNoticeEntity::class,
        ProvenanceEntity::class,
        StockItemEntity::class,
        StockMovementEntity::class,
        SyncOutboxEntity::class,
        SyncMetaEntity::class,
    ],
    version = 14,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
    abstract fun stockDao(): StockDao
    abstract fun syncDao(): SyncDao

    companion object {
        /**
         * Every table holding user data, parents before children. Backup restore and sync apply walk
         * it forwards, erase and sync deletes walk it backwards.
         *
         * Stock items and products (with their children) come before `financial_events` although no
         * foreign key asks for it: sync merges twin rows (same name created on two phones) by
         * recording an alias when the twin is upserted, and rewrites `categoryId`, `productId` and
         * `stockItemId` of rows applied *after* that. An event applied before its material's alias
         * exists keeps the other phone's id and never prices the local material (final review I-5).
         * Contacts follow businesses for the same reason (`contactId` is rewritten through aliases);
         * notes and consignments come after products and before `financial_events`, parents first.
         */
        val DATA_TABLES = listOf(
            "businesses",
            "contacts",
            "projects",
            "books",
            "categories",
            "import_batches",
            "stock_items",
            "products",
            "product_units",
            "product_recipe_lines",
            "product_movements",
            "consignments",
            "consignment_lines",
            "sales",
            "sale_lines",
            "budget_items",
            "financial_events",
            "journal_entries",
            "postings",
            "import_notices",
            "provenance_records",
            "stock_movements",
        )

        fun create(context: Context, name: String = "mankeu.db"): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            name,
        )
            // DEC-026 / spec #1: pre-0.1.0 installs start clean; schema 12 onward is carried forward.
            // The destructive path runs Callback.onDestructiveMigration, not onCreate (see SyncSchema.callback).
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
            .addMigrations(Migration12To13, Migration13To14)
            .addCallback(SyncSchema.callback)
            .build()
    }
}
