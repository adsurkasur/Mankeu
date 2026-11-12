package com.mankeu.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.domain.BookMode
import com.mankeu.domain.FinancialEventType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.channels.Channel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import com.mankeu.presentation.CatatanFilter
import com.mankeu.presentation.UnifiedLedger
import com.mankeu.presentation.UnifiedLedgerItem
import java.time.YearMonth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class LedgerWindowTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var business: String
    private lateinit var project: String
    private lateinit var book: String
    private val date = LocalDate.of(2026, 11, 10)

    @Before fun setup() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = FinanceRepository(database)
        val scope = repository.createBusinessWithUnit("Uji jendela", "Utama")
        business = scope.first
        project = scope.second
        book = repository.books(project).first().first { it.mode == BookMode.REALIZATION.name }.id
    }
    @After fun close() { database.close() }

    private suspend fun expense(day: LocalDate, description: String = "Belanja") = repository.recordEvent(
        EventDraft(business, project, book, FinancialEventType.CASH_EXPENSE, 1_000, day, description, "Biaya"),
    )

    @Test fun recentRowsIncludeTheEntireBoundaryDay() = runBlocking {
        repository.inTransaction { repeat(201) { expense(date) } }
        val events = repository.eventsForBook(book).first()
        assertEquals(201, events.size)
        assertEquals(201_000L, events.sumOf { it.amountRupiah })
        val window = repository.ledgerWindow(project).first()
        assertEquals(201, window.events.size)
        assertFalse(window.hasEarlier)
        val days = UnifiedLedger.build(window.sales, window.events, emptyList(), CatatanFilter.ALL, oldestDate = window.oldestDate)
        assertEquals(201_000L, days.single().totalOutRupiah)
        assertEquals(201_000L, repository.monthlySummary(book, YearMonth.from(date)).first().totalCashOutRupiah)
    }

    private suspend fun sale(day: LocalDate, paid: Long = 0): Pair<String, SaleDraft> {
        val product = repository.saveProduct(project, null, ProductDraft("Uji produk", "unit", "1",
            listOf(SaleUnitDraft(name = "unit", contentText = "1", priceRupiah = 600_000)), emptyList()))
        val customer = repository.saveContact(business, null, ContactDraft("Pelanggan uji"))
        val draft = SaleDraft(customer, day, day.plusDays(7), 0, "Nota uji",
            listOf(SaleLineDraft(product, "unit", "1", 600_000)), paid)
        return repository.saveSale(project, null, draft) to draft
    }

    @Test fun rowBudgetIncludesNoteOnlyDaysAndExtendsWholeBoundaryDate() = runBlocking {
        sale(date.minusDays(2))
        repository.inTransaction {
            repeat(199) { expense(date) }
            repeat(3) { expense(date.minusDays(1)) }
        }
        val first = repository.ledgerWindow(project).first()
        assertEquals(date.minusDays(1), first.oldestDate)
        assertEquals(202, first.events.size)
        assertTrue(first.hasEarlier)
        val older = repository.ledgerWindow(project, first.loadedRows + 200).first()
        assertEquals(date.minusDays(2), older.oldestDate)
        assertFalse(older.hasEarlier)
        assertEquals(3, UnifiedLedger.build(older.sales, older.events, emptyList(), CatatanFilter.ALL, oldestDate = older.oldestDate).size)
    }

    @Test fun oldLaterPaymentLoadsWithItsOriginalIdAndCanBeEdited() = runBlocking {
        val (id, _) = sale(date.minusDays(10))
        val payment = repository.recordSalePayment(id, 400_000, date.minusDays(9))
        val oldExpense = expense(date.minusDays(9), "Biaya lama")
        repository.inTransaction { repeat(200) { expense(date) } }
        val recent = repository.ledgerWindow(project).first()
        assertEquals(200, recent.events.size)
        assertTrue(recent.hasEarlier)
        val older = repository.ledgerWindow(project, recent.loadedRows + 200).first()
        val days = UnifiedLedger.build(older.sales, older.events, emptyList(), CatatanFilter.ALL, oldestDate = older.oldestDate)
        val paymentRow = days.flatMap { it.items }.filterIsInstance<UnifiedLedgerItem.SalePayment>().single()
        assertEquals(payment, paymentRow.event.id)
        assertEquals(id, paymentRow.saleRecord!!.sale.id)
        assertEquals(400_000L, days.sumOf { it.totalInRupiah })
        assertEquals(older.events.size, older.events.distinctBy { it.id }.size)
        repository.updateEvent(oldExpense, EventDraft(business, project, book, FinancialEventType.CASH_EXPENSE,
            2_000, date.minusDays(9), "Biaya lama diubah", "Biaya"))
        val edited = repository.ledgerWindow(project, older.loadedRows + 200).first()
        assertEquals(2_000L, edited.events.single { it.id == oldExpense }.amountRupiah)
        assertEquals(202_000L, repository.monthlySummary(book, YearMonth.from(date)).first().totalCashOutRupiah)
    }

    @Test fun newerPaymentLinksToNoteOutsideTheLoadedDateRange() = runBlocking {
        val (id, _) = sale(date.minusDays(10))
        val payment = repository.recordSalePayment(id, 600_000, date)
        repository.inTransaction { repeat(200) { expense(date) } }
        val window = repository.ledgerWindow(project).first()
        assertEquals(date, window.oldestDate)
        val day = UnifiedLedger.build(window.sales, window.events, emptyList(), CatatanFilter.ALL, oldestDate = window.oldestDate).single()
        assertEquals(600_000L, day.totalInRupiah)
        assertTrue(day.items.none { it is UnifiedLedgerItem.Sale })
        assertEquals(id, day.items.filterIsInstance<UnifiedLedgerItem.SalePayment>().single().saleRecord!!.sale.id)
        assertEquals(payment, day.items.filterIsInstance<UnifiedLedgerItem.SalePayment>().single().event.id)
    }

    @Test fun fullyPaidSameDayNoteAbsorbsTheCanonicalPaymentExactlyOnce() = runBlocking {
        val (id, _) = sale(date, 600_000)
        val window = repository.ledgerWindow(project).first()
        val day = UnifiedLedger.build(window.sales, window.events, emptyList(), CatatanFilter.ALL, oldestDate = window.oldestDate).single()
        assertEquals(1, day.items.size)
        assertEquals(id, (day.items.single() as UnifiedLedgerItem.Sale).record.sale.id)
        assertEquals(window.events.single().id, (day.items.single() as UnifiedLedgerItem.Sale).representedPayments.single().id)
        assertEquals(600_000L, day.totalInRupiah)
        assertEquals(600_000L, repository.monthlySummary(book, YearMonth.from(date)).first().totalCashInRupiah)
    }

    @Test fun paymentsRemainOnTheirDatesAfterNoteEditThenDisappearOnDelete() = runBlocking {
        val (id, draft) = sale(date, 200_000)
        repository.recordSalePayment(id, 200_000, date)
        repository.recordSalePayment(id, 200_000, date.plusDays(1))
        fun days(window: LedgerWindow) = UnifiedLedger.build(window.sales, window.events, emptyList(), CatatanFilter.ALL, oldestDate = window.oldestDate)
        assertEquals(listOf(200_000L, 400_000L), days(repository.ledgerWindow(project).first()).map { it.totalInRupiah })
        repository.saveSale(project, id, draft.copy(date = date.minusDays(1)))
        assertEquals(listOf(200_000L, 400_000L, 0L), days(repository.ledgerWindow(project).first()).map { it.totalInRupiah })
        val rejected = runCatching { repository.saveSale(project, id, draft.copy(date = date.plusDays(2))) }
        assertTrue(rejected.isFailure)
        assertEquals(date.minusDays(1).toEpochDay(), repository.ledgerWindow(project).first().sales.single().sale.dateEpochDay)
        assertEquals(3, repository.deleteSale(id))
        val empty = repository.ledgerWindow(project).first()
        assertEquals(emptyList<Any>(), days(empty))
        assertEquals(0L, repository.monthlySummary(book, YearMonth.from(date)).first().totalCashInRupiah)
    }

    @Test fun missingUnitIsAnEmptyWindowInsteadOfADeadFlow() = runBlocking {
        val empty = repository.ledgerWindow("removed-unit").first()
        assertTrue(empty.events.isEmpty())
        assertTrue(empty.sales.isEmpty())
        assertFalse(empty.hasEarlier)
    }

    @Test fun contactRenameRefreshesTheTransactionCoherentNoteSnapshot() = runBlocking {
        sale(date)
        val windows = Channel<LedgerWindow>(Channel.UNLIMITED)
        val collector = launch(Dispatchers.Default) { repository.ledgerWindow(project).collect { windows.send(it) } }
        try {
            val first = withTimeout(5_000) { windows.receive() }
            val contact = first.sales.single().contact!!
            repository.saveContact(business, contact.id, ContactDraft("Nama diperbarui"))
            val updated = withTimeout(5_000) { windows.receive() }
            assertEquals("Nama diperbarui", updated.sales.single().contact!!.name)
            assertEquals(first.events, updated.events)
        } finally { collector.cancelAndJoin() }
    }

    @Test fun editedPaidPurchaseKeepsItsStatusBeforeAndAfterLoadingOlderPayment() = runBlocking {
        val contact = repository.saveContact(business, null, ContactDraft("Pemasok uji", isCustomer = false, isSupplier = true))
        val draft = EventDraft(business, project, book, FinancialEventType.CREDIT_PURCHASE, 50_000,
            date, "Belanja tempo dipindah tanggal", "Biaya", contactId = contact)
        val purchase = repository.recordEvent(draft)
        repository.recordSupplierPayment(purchase, 50_000, date)
        repository.updateEvent(purchase, draft.copy(date = date.plusDays(2)))
        repository.inTransaction { repeat(199) { expense(date.plusDays(2)) } }
        val initial = repository.ledgerWindow(project).first()
        assertEquals(date.plusDays(2), initial.oldestDate)
        assertEquals(200, initial.events.size)
        val initialUnpaid = UnifiedLedger.build(initial.sales, initial.events, emptyList(), CatatanFilter.UNPAID,
            oldestDate = initial.oldestDate, supplierPaymentsRupiah = initial.supplierPaidRupiah)
        assertTrue("Already paid remains paid even when its old payment is outside the loaded dates", initialUnpaid.isEmpty())
        val initialCash = UnifiedLedger.build(initial.sales, initial.events, emptyList(), CatatanFilter.ALL,
            oldestDate = initial.oldestDate, supplierPaymentsRupiah = initial.supplierPaidRupiah)
        assertEquals(199_000L, initialCash.sumOf { it.totalOutRupiah })
        val history = repository.ledgerWindow(project, initial.loadedRows + 200).first()
        val historyUnpaid = UnifiedLedger.build(history.sales, history.events, emptyList(), CatatanFilter.UNPAID,
            oldestDate = history.oldestDate, supplierPaymentsRupiah = history.supplierPaidRupiah)
        assertEquals(initialUnpaid, historyUnpaid)
        val historyCash = UnifiedLedger.build(history.sales, history.events, emptyList(), CatatanFilter.ALL,
            oldestDate = history.oldestDate, supplierPaymentsRupiah = history.supplierPaidRupiah)
        assertEquals(249_000L, historyCash.sumOf { it.totalOutRupiah })
        assertEquals(50_000L, repository.monthlySummary(book, YearMonth.from(date)).first().totalCashOutRupiah - 199_000L)
    }
}
