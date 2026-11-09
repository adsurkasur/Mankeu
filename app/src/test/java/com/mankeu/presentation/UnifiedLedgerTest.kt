package com.mankeu.presentation

import com.mankeu.data.ContactEntity
import com.mankeu.data.ProductEntity
import com.mankeu.data.ProductRecord
import com.mankeu.data.SaleEntity
import com.mankeu.data.SaleLineEntity
import com.mankeu.data.SaleRecord
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.MoneyEvent
import com.mankeu.domain.SaleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UnifiedLedgerTest {
    private val today = LocalDate.of(2026, 11, 10)
    private val yesterday = today.minusDays(1)
    private val lastWeek = today.minusDays(7)

    private val customerA = ContactEntity("c1", "b1", "Toko A", "toko a", createdAtEpochMillis = 0)
    private val customerB = ContactEntity("c2", "b1", "Toko B", "toko b", createdAtEpochMillis = 0)

    private val productKeripik = ProductRecord(
        product = ProductEntity("p1", "b1", "Keripik", "bungkus", createdAtEpochMillis = 0),
        units = emptyList(),
        recipe = emptyList(),
    )

    private fun note(
        id: String,
        date: LocalDate,
        contact: ContactEntity?,
        total: Long,
        paid: Long = total,
    ) = SaleRecord(
        sale = SaleEntity(id, "b1", "prj1", contact?.id, date.toEpochDay(), createdAtEpochMillis = 0),
        lines = listOf(SaleLineEntity("$id-line", id, "p1", "dus", "40", "2", total, total, 0)),
        payments = if (paid > 0) listOf(event("$id-pay", date, FinancialEventType.CASH_INCOME, paid, "Bayar", "Penjualan", id)) else emptyList(),
        contact = contact,
    )

    private fun event(
        id: String,
        date: LocalDate,
        type: FinancialEventType,
        amount: Long,
        description: String,
        category: String,
        saleId: String? = null,
    ) = MoneyEvent(
        id = id,
        businessId = "b1",
        projectId = "prj1",
        type = type,
        amountRupiah = amount,
        date = date,
        description = description,
        category = category,
        saleId = saleId,
    )

    @Test
    fun ordersAndGroupsEntriesByDateDescending() {
        val saleToday = note("s1", today, customerA, 600_000)
        val expenseYesterday = event("e1", yesterday, FinancialEventType.CASH_EXPENSE, 260_000, "Minyak 10 liter", "Bahan Baku")
        val incomeLastWeek = event("e2", lastWeek, FinancialEventType.CASH_INCOME, 50_000, "Jual kardus", "Pendapatan Lain")

        val groups = UnifiedLedger.build(
            sales = listOf(saleToday),
            events = listOf(expenseYesterday, incomeLastWeek),
            products = listOf(productKeripik),
            filter = CatatanFilter.ALL,
            today = today,
        )

        assertEquals(3, groups.size)
        assertEquals(today, groups[0].date)
        assertEquals(yesterday, groups[1].date)
        assertEquals(lastWeek, groups[2].date)
    }

    @Test
    fun absorbsInitialNotePaymentOnSameDayWithoutDuplicateRowOrDoubleCounting() {
        val saleToday = note("s1", today, customerA, 600_000, paid = 600_000)
        val paymentEvent = event("pay1", today, FinancialEventType.CASH_INCOME, 600_000, "Bayar nota Toko A", "Penjualan", saleId = "s1")

        val groups = UnifiedLedger.build(
            sales = listOf(saleToday),
            events = listOf(paymentEvent),
            products = listOf(productKeripik),
            filter = CatatanFilter.ALL,
            today = today,
        )

        assertEquals(1, groups.size)
        val day = groups[0]
        // Only one item: the Sale note. The payment event on the same day is absorbed.
        assertEquals(1, day.items.size)
        val item = day.items[0]
        assertTrue(item is UnifiedLedgerItem.Sale)
        val saleItem = item as UnifiedLedgerItem.Sale
        assertEquals(600_000L, saleItem.totalRupiah)
        assertEquals(SaleStatus.PAID, saleItem.status)
        assertEquals(600_000L, saleItem.cashDeltaRupiah)
        // Daily total: exact cash in of 600.000, NOT 1.200.000
        assertEquals(600_000L, day.totalInRupiah)
        assertEquals(0L, day.totalOutRupiah)
    }

    @Test
    fun unpaidNoteShowsZeroCashAndLaterPaymentAppearsOnPaymentDate() {
        // Sale on yesterday: 600.000, unpaid (0 cash in yesterday)
        val unpaidSale = note("s1", yesterday, customerA, 600_000, paid = 0)
        // Payment on today: 400.000 paid today
        val paymentToday = event("pay1", today, FinancialEventType.CASH_INCOME, 400_000, "Bayar nota Toko A", "Penjualan", saleId = "s1")

        val groups = UnifiedLedger.build(
            sales = listOf(unpaidSale),
            events = listOf(paymentToday),
            products = listOf(productKeripik),
            filter = CatatanFilter.ALL,
            today = today,
        )

        assertEquals(2, groups.size)
        // Today group: has the payment
        val todayGroup = groups[0]
        assertEquals(today, todayGroup.date)
        assertEquals(1, todayGroup.items.size)
        assertTrue(todayGroup.items[0] is UnifiedLedgerItem.SalePayment)
        assertEquals(400_000L, todayGroup.totalInRupiah)

        // Yesterday group: has the unpaid note with 0 cash in
        val yesterdayGroup = groups[1]
        assertEquals(yesterday, yesterdayGroup.date)
        assertEquals(1, yesterdayGroup.items.size)
        assertTrue(yesterdayGroup.items[0] is UnifiedLedgerItem.Sale)
        val saleItem = yesterdayGroup.items[0] as UnifiedLedgerItem.Sale
        assertEquals(0L, saleItem.cashDeltaRupiah)
        assertEquals(0L, yesterdayGroup.totalInRupiah)
    }

    @Test
    fun filtersByUnpaidShowsOnlyOutstandingNotesAndCreditPurchases() {
        val paidSale = note("s1", today, customerA, 600_000, paid = 600_000)
        val unpaidSale = note("s2", today, customerB, 500_000, paid = 0)
        val creditPurchase = event("cp1", yesterday, FinancialEventType.CREDIT_PURCHASE, 300_000, "Beli plastik tempo", "Bahan")
        val cashExpense = event("e1", yesterday, FinancialEventType.CASH_EXPENSE, 100_000, "Listrik", "Operasional")

        val groups = UnifiedLedger.build(
            sales = listOf(paidSale, unpaidSale),
            events = listOf(creditPurchase, cashExpense),
            products = listOf(productKeripik),
            filter = CatatanFilter.UNPAID,
            today = today,
        )

        val allItems = groups.flatMap { it.items }
        assertEquals(2, allItems.size)
        assertTrue(allItems.any { it is UnifiedLedgerItem.Sale && it.record.sale.id == "s2" })
        assertTrue(allItems.any { it is UnifiedLedgerItem.Money && it.event.id == "cp1" })
        assertFalse(allItems.any { it is UnifiedLedgerItem.Sale && it.record.sale.id == "s1" })
        assertFalse(allItems.any { it is UnifiedLedgerItem.Money && it.event.id == "e1" })
    }

    @Test
    fun filtersByIncomeShowsCashNotesAndIncomesOnly() {
        val cashSale = note("s1", today, customerA, 600_000, paid = 600_000)
        val unpaidSale = note("s2", today, customerB, 500_000, paid = 0)
        val otherIncome = event("e1", yesterday, FinancialEventType.CASH_INCOME, 150_000, "Jual karung", "Lain-lain")
        val expense = event("e2", yesterday, FinancialEventType.CASH_EXPENSE, 200_000, "Bensin", "Operasional")

        val groups = UnifiedLedger.build(
            sales = listOf(cashSale, unpaidSale),
            events = listOf(otherIncome, expense),
            products = listOf(productKeripik),
            filter = CatatanFilter.INCOME,
            today = today,
        )

        val allItems = groups.flatMap { it.items }
        assertEquals(2, allItems.size)
        assertTrue(allItems.any { it is UnifiedLedgerItem.Sale && it.record.sale.id == "s1" })
        assertTrue(allItems.any { it is UnifiedLedgerItem.Money && it.event.id == "e1" })
        assertFalse(allItems.any { it is UnifiedLedgerItem.Sale && it.record.sale.id == "s2" })
        assertFalse(allItems.any { it is UnifiedLedgerItem.Money && it.event.id == "e2" })
    }

    @Test
    fun filtersByExpenseShowsCashPurchasesAndExpensesOnly() {
        val cashSale = note("s1", today, customerA, 600_000, paid = 600_000)
        val expense = event("e1", today, FinancialEventType.CASH_EXPENSE, 200_000, "Bensin", "Operasional")
        val creditPurchase = event("cp1", yesterday, FinancialEventType.CREDIT_PURCHASE, 300_000, "Beli plastik tempo", "Bahan")

        val groups = UnifiedLedger.build(
            sales = listOf(cashSale),
            events = listOf(expense, creditPurchase),
            products = listOf(productKeripik),
            filter = CatatanFilter.EXPENSE,
            today = today,
        )

        val allItems = groups.flatMap { it.items }
        assertEquals(1, allItems.size)
        assertTrue(allItems.any { it is UnifiedLedgerItem.Money && it.event.id == "e1" })
    }

    @Test
    fun searchesByCustomerNameAndDescription() {
        val saleA = note("s1", today, customerA, 600_000)
        val saleB = note("s2", today, customerB, 500_000)
        val expense = event("e1", today, FinancialEventType.CASH_EXPENSE, 100_000, "Minyak goreng", "Bahan")

        val groupsA = UnifiedLedger.build(
            sales = listOf(saleA, saleB),
            events = listOf(expense),
            products = listOf(productKeripik),
            filter = CatatanFilter.ALL,
            search = "Toko A",
            today = today,
        )
        assertEquals(1, groupsA.flatMap { it.items }.size)
        assertEquals("s1", (groupsA[0].items[0] as UnifiedLedgerItem.Sale).record.sale.id)

        val groupsMinyak = UnifiedLedger.build(
            sales = listOf(saleA, saleB),
            events = listOf(expense),
            products = listOf(productKeripik),
            filter = CatatanFilter.ALL,
            search = "minyak",
            today = today,
        )
        assertEquals(1, groupsMinyak.flatMap { it.items }.size)
        assertEquals("e1", (groupsMinyak[0].items[0] as UnifiedLedgerItem.Money).event.id)
    }

    @Test
    fun hasAnyEntriesReturnsTrueWhenOnlySalesExistPreventingL14AuditBug() {
        val sales = listOf(note("s1", today, customerA, 600_000))
        val events = emptyList<MoneyEvent>()

        assertTrue(UnifiedLedger.hasAnyEntries(sales, events))
        assertFalse(UnifiedLedger.hasAnyEntries(emptyList(), emptyList()))
    }

    @Test
    fun buildsScaleLedgerUnderFiftyMilliseconds() {
        val largeSales = (1..500).map { i ->
            note("s_$i", today.minusDays((i % 60).toLong()), customerA, 500_000, paid = 250_000)
        }
        val largeEvents = (1..500).map { i ->
            event("e_$i", today.minusDays((i % 60).toLong()), FinancialEventType.CASH_EXPENSE, 100_000, "Pengeluaran $i", "Operasional")
        }

        val start = System.currentTimeMillis()
        val groups = UnifiedLedger.build(
            sales = largeSales,
            events = largeEvents,
            products = listOf(productKeripik),
            filter = CatatanFilter.ALL,
            today = today,
        )
        val elapsed = System.currentTimeMillis() - start

        assertTrue("Expected build to take less than 500ms, took ${elapsed}ms", elapsed < 500)
        assertTrue(groups.isNotEmpty())
    }
}
