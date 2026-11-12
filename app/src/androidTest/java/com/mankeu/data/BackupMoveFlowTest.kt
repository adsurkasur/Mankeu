package com.mankeu.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.domain.BookMode
import com.mankeu.domain.MonthlySummary
import com.mankeu.domain.SaleUnit
import com.mankeu.sync.SyncSchema
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class BackupMoveFlowTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var backupFile: File

    @Before fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(SyncSchema.callback)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(database)
        backupFile = File(context.cacheDir, "backup-move-flow-test.aabk")
        val (businessId, projectId) = repository.createBusinessWithUnit("Warung Uji", "Umum")
        val buyerId = repository.saveContact(businessId, null, ContactDraft("Bu Ani"))
        val bookId = repository.books(projectId).first().single { it.mode == BookMode.REALIZATION.name }.id
        val productId = repository.saveProduct(projectId, null, ProductDraft(
            name = "Kopi",
            baseUnit = "gelas",
            batchYieldText = "1",
            units = listOf(SaleUnitDraft(name = "gelas", contentText = "1", priceRupiah = 20_000)),
            recipe = listOf(RecipeLineDraft("Kopi bubuk", amountRupiah = 5_000)),
        ))
        repository.saveSale(projectId, null, SaleDraft(
            contactId = buyerId,
            date = LocalDate.of(2026, 10, 12),
            dueDate = LocalDate.of(2026, 10, 19),
            discountRupiah = 0,
            notes = "Antar sore",
            lines = listOf(SaleLineDraft(productId, "gelas", "1", 20_000)),
            paidNowRupiah = 10_000,
        ))
        // Keep the expected canonical summary and buyer list alongside the fixture for restoration.
        expectedSummary = repository.monthlySummary(bookId, YearMonth.of(2026, 10)).first()
        expectedBuyers = repository.contacts(businessId).first()
    }

    @After fun tearDown() {
        database.close()
        backupFile.delete()
    }

    private lateinit var expectedSummary: MonthlySummary
    private lateinit var expectedBuyers: List<ContactEntity>

    @Test fun savedFileRestoresTheSameMonthlySummaryAndBuyerListAfterErase() = runBlocking {
        val password = "pindah-hp-uji".toCharArray()
        val manager = EncryptedBackupManager(database, backupFile)
        backupFile.writeBytes(manager.create(password))
        assertTrue(backupFile.length() > 0)

        repository.eraseAllData()
        assertTrue(database.financeDao().getBusinesses().isEmpty())

        val prepared = manager.prepareRestore(backupFile.readBytes(), password)
        manager.restore(prepared, password)

        val businessId = database.financeDao().getBusinesses().single().id
        val projectId = database.financeDao().getProjects(businessId).single().id
        val bookId = repository.books(projectId).first().single { it.mode == BookMode.REALIZATION.name }.id
        assertEquals(expectedSummary, repository.monthlySummary(bookId, YearMonth.of(2026, 10)).first())
        assertEquals(expectedBuyers, repository.contacts(businessId).first())
    }
}
