package com.mankeu.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.domain.BookMode
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.StockMovementType
import com.mankeu.sync.SyncSchema
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate

/** "Hapus semua data" (DEC-020) leaves nothing of the old data on the phone, and the app still works after. */
@RunWith(AndroidJUnit4::class)
class EraseAllDataTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository

    @Before
    fun setUp() {
        // With the sync callback, as in the app: triggers would otherwise not exist to be silenced.
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addCallback(SyncSchema.callback)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun everyTableIsEmptyAndTheDeviceGetsANewSyncIdentity() = runBlocking {
        val businessId = repository.createBusiness("Warung Uji")
        val projectId = repository.createProject(businessId, "Cabang Uji")
        val bookId = database.financeDao().getBooks(projectId).first { it.mode == BookMode.REALIZATION.name }.id
        repository.recordEvent(
            EventDraft(businessId, projectId, bookId, FinancialEventType.CASH_INCOME, 50_000, LocalDate.of(2026, 10, 1), "Jual", ""),
        )
        val item = repository.createStockItem(businessId, projectId, false, "Gula", "kg", 15_000, BigDecimal.TEN, BigDecimal.ONE)
        repository.recordStockMovement(item, StockMovementType.OUT, BigDecimal.ONE, LocalDate.of(2026, 10, 2), "")
        val oldDevice = meta(SyncSchema.DEVICE_ID)
        assertTrue(count("sync_outbox") > 0)

        repository.eraseAllData()

        AppDatabase.DATA_TABLES.forEach { table -> assertEquals(table, 0, count(table)) }
        assertEquals("deletes must not queue a sync push of the old ids", 0, count("sync_outbox"))
        assertEquals(1, count("sync_meta"))
        assertNotEquals(oldDevice, meta(SyncSchema.DEVICE_ID))
        assertEquals(null, meta(SyncSchema.APPLYING_REMOTE))

        // Change capture is back on: a new business after the wipe is recorded for sync again.
        repository.createBusiness("Usaha Baru")
        assertTrue(count("sync_outbox") > 0)
    }

    @Test
    fun theRecoveryCopyIsDeleted() {
        val file = File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "erase-test-recovery.aabk")
        file.writeBytes(byteArrayOf(1, 2, 3))
        val manager = EncryptedBackupManager(database, file)
        assertTrue(manager.hasLatestRecovery())

        manager.deleteLatestRecovery()
        manager.deleteLatestRecovery() // nothing left: not an error

        assertFalse(file.exists())
    }

    private fun count(table: String): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM `$table`").use { it.moveToFirst(); it.getInt(0) }

    private fun meta(key: String): String? =
        database.openHelper.readableDatabase.query("SELECT value FROM sync_meta WHERE `key` = ?", arrayOf(key)).use {
            if (it.moveToFirst()) it.getString(0) else null
        }
}
