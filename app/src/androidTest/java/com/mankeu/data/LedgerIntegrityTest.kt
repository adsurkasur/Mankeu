package com.mankeu.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.backup.BackupTable
import com.mankeu.backup.BackupValue
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.PasswordBackupCipher
import com.mankeu.backup.PortableBackupCodec
import com.mankeu.domain.BookMode
import com.mankeu.domain.CashFlowRole
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.FinancialEventType.CASH_EXPENSE
import com.mankeu.domain.FinancialEventType.CASH_INCOME
import com.mankeu.domain.LedgerAccount
import com.mankeu.domain.PostedRecord
import com.mankeu.domain.PostingReportCalculator
import com.mankeu.domain.PostingSide
import com.mankeu.importer.ImportTransaction
import com.mankeu.importer.WorkbookImportPreview
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

/**
 * One regression per ledger-integrity defect fixed in D-076..D-079. Each asserts the balances a
 * report would show afterwards, not just that a call returned: every one of these bugs "succeeded"
 * and left the books wrong.
 */
@RunWith(AndroidJUnit4::class)
class LedgerIntegrityTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var businessId: String
    private lateinit var projectId: String
    private lateinit var bookId: String
    private val dao get() = database.financeDao()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = FinanceRepository(database)
        runBlocking {
            businessId = repository.createBusiness("Warung Uji")
            projectId = repository.createProject(businessId, "Siklus Uji")
            bookId = dao.getBooks(projectId).first { it.mode == BookMode.REALIZATION.name }.id
        }
    }

    @After
    fun tearDown() = database.close()

    /** DEC-019: Ubah replaces the record in place; its identity stays and the journal is rebuilt. */
    @Test
    fun editingKeepsTheRecordIdentityAndRebuildsItsJournal() = runBlocking {
        val sale = record(CASH_INCOME, 100_000)
        val before = dao.getEvent(sale)!!

        repository.updateEvent(sale, draft(CASH_EXPENSE, 250_000).copy(description = "Listrik"))

        val after = dao.getEvent(sale)!!
        assertEquals(listOf(sale), dao.getEventsForBook(bookId).map { it.id })
        assertEquals(before.createdAtEpochMillis to "Listrik", after.createdAtEpochMillis to after.description)
        assertEquals(CASH_EXPENSE.name to 250_000L, after.type to after.amountRupiah)
        assertEquals(-250_000, summary().cashBalanceRupiah)
        assertEquals(-250_000, summary().profitRupiah)
    }

    /** DEC-022: a blank description takes the category name, on record and on edit. */
    @Test
    fun aBlankDescriptionBecomesTheCategoryName() = runBlocking {
        val categoryId = repository.createCategory(businessId, "Jualan Gorengan", CategoryRole.REVENUE)
        val sale = repository.recordEvent(draft(CASH_INCOME, 20_000).copy(description = "  ", category = "Jualan Gorengan", categoryId = categoryId))
        assertEquals("Jualan Gorengan", dao.getEvent(sale)!!.description)

        repository.updateEvent(sale, draft(CASH_INCOME, 25_000).copy(description = "", category = "Jualan Gorengan", categoryId = categoryId))
        assertEquals("Jualan Gorengan", dao.getEvent(sale)!!.description)
    }

    /** A business never exists without its first unit (review 2026-10-22): both or neither. */
    @Test
    fun aBusinessAndItsFirstUnitAreCreatedTogetherOrNotAtAll() = runBlocking {
        val before = dao.getBusinesses().size

        assertRefused { repository.createBusinessWithUnit("Toko Baru", "   ") }
        assertEquals(before, dao.getBusinesses().size)

        val (business, unit) = repository.createBusinessWithUnit("Toko Baru", "Utama")
        assertEquals(business, dao.getProject(unit)!!.businessId)
    }

    @Test
    fun editingIsValidatedLikeRecording() = runBlocking {
        val sale = record(CASH_INCOME, 100_000)

        assertRefused { repository.updateEvent(sale, draft(CASH_INCOME, 0)) }
        assertEquals(100_000, summary().cashBalanceRupiah)
    }

    @Test
    fun deletingRemovesTheRecordAndItsJournal() = runBlocking {
        val sale = record(CASH_INCOME, 500_000)
        record(CASH_EXPENSE, 100_000)

        repository.deleteEvent(sale)

        assertEquals(1, dao.getEventsForBook(bookId).size)
        assertEquals(-100_000, summary().cashBalanceRupiah)
        assertEquals(0, summary().operatingIncomeRupiah)
    }

    @Test
    fun archivedCategoryNameCanBeAddedAgain() = runBlocking {
        val first = repository.createCategory(businessId, "Bensin Motor", CategoryRole.EXPENSE)
        repository.archiveCategory(first)

        assertEquals(first, repository.createCategory(businessId, "bensin motor", CategoryRole.EXPENSE))
        assertTrue(dao.getCategory(first)!!.active)
        assertRefused { repository.createCategory(businessId, "BENSIN MOTOR", CategoryRole.EXPENSE) }
    }

    @Test
    fun undoingAnImportDeletesEveryImportedCashRowEvenAfterAnEdit() = runBlocking {
        repository.commitImport(
            businessId,
            projectId,
            WorkbookImportPreview(
                "uji.xlsx",
                "sha-cash",
                transactions = listOf(
                    ImportTransaction("Transaksi!A2", DAY, "Jual", "Penjualan & Jasa", incoming = true, amountRupiah = 400_000),
                    ImportTransaction("Transaksi!A3", DAY, "Beli bahan", "Bahan Baku & Stok", incoming = false, amountRupiah = 150_000),
                ),
            ),
        )
        assertEquals(250_000, summary().cashBalanceRupiah)
        val batch = dao.getImportBatches(projectId).single()
        val bought = dao.getEventsForBook(bookId).single { it.description == "Beli bahan" }
        repository.updateEvent(bought.id, draft(CASH_EXPENSE, 175_000))
        assertEquals(batch.id, dao.getEvent(bought.id)!!.importBatchId)

        assertEquals(2, repository.undoImportBatch(batch.id).records)

        assertTrue(dao.getEventsForBook(bookId).isEmpty())
        assertEquals(0, summary().cashBalanceRupiah)
        assertEquals("REVERSED", dao.getImportBatch(batch.id)!!.state)
    }

    @Test
    fun backupTakenOnSchemaThreeRestoresIntoCurrentSchema() = runBlocking {
        record(CASH_INCOME, 750_000)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = EncryptedBackupManager(database, File(context.cacheDir, "ledger-integrity-recovery.aabk"))
        val password = "sandi-uji-123".toCharArray()
        val current = PortableBackupCodec.decode(PasswordBackupCipher.decrypt(manager.create(password), password))
        // Shaped the way v0.0.4 wrote it: schema 3, a `units` table, `categories.defaultUnitId`,
        // and no `stock_*` tables (added in schema 6).
        // It also carries a credit sale, which strict cash (DEC-011) must purge on restore.
        val schemaThree = current.copy(
            schemaVersion = 3,
            tables = current.tables.filterNot { it.name.startsWith("stock_") }.map { table ->
                when (table.name) {
                    "categories" -> table.copy(columns = table.columns + "defaultUnitId", rows = table.rows.map { it + BackupValue.Null })
                    "financial_events" -> table.copy(rows = table.rows + listOf(legacyCreditSale(table)))
                    else -> table
                }
            } + BackupTable("units", listOf("id", "businessId", "name"), emptyList()),
        )
        record(CASH_EXPENSE, 50_000)

        val prepared = manager.prepareRestore(
            PasswordBackupCipher.encrypt(PortableBackupCodec.encode(schemaThree), password),
            password,
        )
        manager.restore(prepared, password)

        assertEquals(750_000, summary().cashBalanceRupiah)
        assertTrue(dao.getEventsForBook(bookId).none { it.type == "CREDIT_SALE" })
        assertTrue(dao.observeNotices().first().any { it.batchId == null && "non-kas" in it.title })
    }

    /** A schema-10 backup still holds pairs from "Balik Transaksi"; restoring it drops each pair (DEC-019). */
    @Test
    fun backupTakenOnSchemaTenRestoresWithoutReversalPairs() = runBlocking {
        record(CASH_INCOME, 750_000)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = EncryptedBackupManager(database, File(context.cacheDir, "ledger-integrity-recovery.aabk"))
        val password = "sandi-uji-123".toCharArray()
        val current = PortableBackupCodec.decode(PasswordBackupCipher.decrypt(manager.create(password), password))
        val reversalColumns = listOf(BackupValue.Text("POSTED"), BackupValue.Null)
        fun pairRow(table: BackupTable, id: String, type: String, reversedBy: BackupValue) = table.rows.first().mapIndexed { index, value ->
            when (table.columns[index]) {
                "id" -> BackupValue.Text(id)
                "type" -> BackupValue.Text(type)
                else -> value
            }
        } + reversalColumns + reversedBy
        val schemaTen = current.copy(
            schemaVersion = 10,
            tables = current.tables.map { table ->
                if (table.name != "financial_events") return@map table
                table.copy(
                    columns = table.columns + listOf("status", "reversesEventId", "reversedByEventId"),
                    rows = table.rows.map { it + reversalColumns + BackupValue.Null } +
                        listOf(
                            pairRow(table, "typo", "CASH_EXPENSE", BackupValue.Text("typo-undo")),
                            pairRow(table, "typo-undo", "REVERSAL", BackupValue.Null),
                        ),
                )
            },
        )
        record(CASH_EXPENSE, 50_000)

        manager.restore(manager.prepareRestore(PasswordBackupCipher.encrypt(PortableBackupCodec.encode(schemaTen), password), password), password)

        assertEquals(1, dao.getEventsForBook(bookId).size)
        assertEquals(750_000, summary().cashBalanceRupiah)
        assertTrue(dao.observeNotices().first().any { it.batchId == null && it.title == "2 catatan yang dibalik dihapus" })
    }

    /** A copy of the table's first row turned into a pre-schema-8 credit sale. */
    private fun legacyCreditSale(table: BackupTable): List<BackupValue> = table.rows.first().mapIndexed { index, value ->
        when (table.columns[index]) {
            "id" -> BackupValue.Text("legacy-credit-sale")
            "type" -> BackupValue.Text("CREDIT_SALE")
            else -> value
        }
    }

    private suspend fun record(type: FinancialEventType, amount: Long): String = repository.recordEvent(draft(type, amount))

    private fun draft(type: FinancialEventType, amount: Long) = EventDraft(
        businessId = businessId,
        projectId = projectId,
        bookId = bookId,
        type = type,
        amountRupiah = amount,
        date = DAY,
        description = "Uji $type",
        category = "",
    )

    private suspend fun summary() = PostingReportCalculator.calculate(
        dao.getPostingRows(bookId).map {
            PostedRecord(
                it.eventId,
                CashFlowRole.valueOf(it.cashFlowRole),
                LedgerAccount.valueOf(it.account),
                PostingSide.valueOf(it.side),
                it.amountRupiah,
            )
        },
    )

    /** Business rules reject with `require`, which rolls the whole transaction back. */
    private suspend fun assertRefused(block: suspend () -> Unit) {
        val failure = runCatching { block() }.exceptionOrNull()
        assertTrue("Expected a rule refusal, got $failure", failure is IllegalArgumentException)
    }

    private companion object {
        val DAY: LocalDate = LocalDate.of(2026, 9, 1)
    }
}
