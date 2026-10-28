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
            // DEC-025 / spec #1: pre-0.1.0 installs start clean; only schema 12 is carried forward.
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
            .addCallback(SyncSchema.callback)
            .build()
    }
}
