package com.mankeu.presentation

import com.mankeu.data.ProductRecord
import com.mankeu.data.SaleRecord
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.CashTotals
import com.mankeu.domain.MoneyEvent
import com.mankeu.domain.SaleStatus
import java.time.LocalDate

enum class CatatanFilter {
    ALL,
    UNPAID,
    OVERDUE,
    INCOME,
    EXPENSE,
}

sealed interface UnifiedLedgerItem {
    val id: String
    val date: LocalDate
    val cashDeltaRupiah: Long

    data class Sale(
        val record: SaleRecord,
        val productSummary: String,
        val paidOnDateRupiah: Long,
        val representedPayments: List<MoneyEvent> = emptyList(),
    ) : UnifiedLedgerItem {
        override val id: String get() = "sale_" + record.sale.id
        override val date: LocalDate get() = LocalDate.ofEpochDay(record.sale.dateEpochDay)
        override val cashDeltaRupiah: Long get() = paidOnDateRupiah
        val totalRupiah: Long get() = record.total
        val outstandingRupiah: Long get() = record.outstanding
        val status: SaleStatus get() = record.status
        val customerName: String? get() = record.contact?.name
    }

    data class SalePayment(
        val event: MoneyEvent,
        val customerName: String,
        val saleRecord: SaleRecord?,
    ) : UnifiedLedgerItem {
        override val id: String get() = "payment_" + event.id
        override val date: LocalDate get() = event.date
        override val cashDeltaRupiah: Long get() = event.amountRupiah
        val amountRupiah: Long get() = event.amountRupiah
    }

    data class Money(
        val event: MoneyEvent,
    ) : UnifiedLedgerItem {
        override val id: String get() = "event_" + event.id
        override val date: LocalDate get() = event.date
        val isCreditPurchase: Boolean get() = event.type == FinancialEventType.CREDIT_PURCHASE
        val isIncoming: Boolean get() = BusinessWords.isIncoming(event.type)
        override val cashDeltaRupiah: Long get() = CashTotals.fromEvents(listOf(event)).deltaRupiah
        val amountRupiah: Long get() = event.amountRupiah
        val description: String get() = event.description
        val category: String get() = event.category
    }
}

data class LedgerDayGroup(
    val date: LocalDate,
    val items: List<UnifiedLedgerItem>,
    val totalInRupiah: Long,
    val totalOutRupiah: Long,
)

object UnifiedLedger {

    fun hasAnyEntries(sales: List<SaleRecord>, events: List<MoneyEvent>): Boolean {
        return sales.isNotEmpty() || events.isNotEmpty()
    }

    fun build(
        sales: List<SaleRecord>,
        events: List<MoneyEvent>,
        products: List<ProductRecord>,
        filter: CatatanFilter,
        search: String = "",
        today: LocalDate = LocalDate.now(),
        oldestDate: LocalDate? = null,
        /** Repository callers provide full status history; null is for complete standalone inputs. */
        supplierPaymentsRupiah: Map<String, Long>? = null,
    ): List<LedgerDayGroup> {
        val productMap = products.associateBy { it.product.id }
        val saleMap = sales.associateBy { it.sale.id }
        // Canonical event identity: a payment occurs once even when present in both inputs.
        // Repository callers supply both inputs from the same database transaction.
        val canonicalEvents = (sales.flatMap { it.payments } + events).associateBy { it.id }.values
            .filter { oldestDate == null || !it.date.isBefore(oldestDate) }
        val paymentsBySale = canonicalEvents.filter { it.saleId != null }.groupBy { it.saleId }
        val supplierPaid = supplierPaymentsRupiah ?: canonicalEvents.filter { it.settlesEventId != null }.groupBy { it.settlesEventId }
            .mapValues { (_, payments) -> payments.sumOf { it.amountRupiah } }

        val absorbedEventIds = mutableSetOf<String>()

        val saleItems = sales.filter { oldestDate == null || it.sale.dateEpochDay >= oldestDate.toEpochDay() }.map { saleRecord ->
            val saleDate = LocalDate.ofEpochDay(saleRecord.sale.dateEpochDay)
            val paymentsOnSaleDate = paymentsBySale[saleRecord.sale.id].orEmpty().filter { it.date == saleDate }
            paymentsOnSaleDate.forEach { absorbedEventIds.add(it.id) }
            val paidOnDate = CashTotals.fromEvents(paymentsOnSaleDate).incomingRupiah

            val productSummary = saleRecord.lines.sortedBy { it.sortOrder }.joinToString(", ") { line ->
                val pName = productMap[line.productId]?.product?.name ?: "—"
                "${line.quantityText} ${line.unitName} $pName"
            }

            UnifiedLedgerItem.Sale(
                record = saleRecord,
                productSummary = productSummary,
                paidOnDateRupiah = paidOnDate,
                representedPayments = paymentsOnSaleDate,
            )
        }

        val nonAbsorbedEvents = canonicalEvents.filterNot { it.id in absorbedEventIds }

        val moneyItems = nonAbsorbedEvents.map { event ->
            if (event.saleId != null) {
                val linkedSale = saleMap[event.saleId]
                val custName = linkedSale?.contact?.name ?: event.description.removePrefix("Bayar nota ").ifBlank { "Umum" }
                UnifiedLedgerItem.SalePayment(
                    event = event,
                    customerName = custName,
                    saleRecord = linkedSale,
                )
            } else {
                UnifiedLedgerItem.Money(event)
            }
        }

        val allItems: List<UnifiedLedgerItem> = saleItems + moneyItems

        // Filter by CatatanFilter
        val filteredByKind = allItems.filter { item ->
            when (filter) {
                CatatanFilter.ALL -> true
                CatatanFilter.UNPAID -> when (item) {
                    is UnifiedLedgerItem.Sale -> item.outstandingRupiah > 0
                    is UnifiedLedgerItem.Money -> item.isCreditPurchase &&
                        item.amountRupiah > (supplierPaid[item.event.id] ?: 0L)
                    is UnifiedLedgerItem.SalePayment -> false
                }
                CatatanFilter.OVERDUE -> item is UnifiedLedgerItem.Sale && item.record.overdue(today)
                CatatanFilter.INCOME -> when (item) {
                    is UnifiedLedgerItem.Sale -> item.cashDeltaRupiah > 0
                    is UnifiedLedgerItem.SalePayment -> true
                    is UnifiedLedgerItem.Money -> item.isIncoming
                }
                CatatanFilter.EXPENSE -> when (item) {
                    is UnifiedLedgerItem.Sale -> false
                    is UnifiedLedgerItem.SalePayment -> false
                    is UnifiedLedgerItem.Money -> !item.isIncoming && !item.isCreditPurchase
                }
            }
        }

        // Filter by search query
        val trimmedSearch = search.trim()
        val filtered = if (trimmedSearch.isBlank()) {
            filteredByKind
        } else {
            filteredByKind.filter { item ->
                when (item) {
                    is UnifiedLedgerItem.Sale -> {
                        item.customerName?.contains(trimmedSearch, ignoreCase = true) == true ||
                            item.productSummary.contains(trimmedSearch, ignoreCase = true) ||
                            item.record.sale.notes.contains(trimmedSearch, ignoreCase = true) ||
                            item.representedPayments.any { it.matches(trimmedSearch) }
                    }
                    is UnifiedLedgerItem.SalePayment -> {
                        item.customerName.contains(trimmedSearch, ignoreCase = true) ||
                            item.event.matches(trimmedSearch)
                    }
                    is UnifiedLedgerItem.Money -> {
                        item.description.contains(trimmedSearch, ignoreCase = true) ||
                            item.category.contains(trimmedSearch, ignoreCase = true) ||
                            item.event.reference?.contains(trimmedSearch, ignoreCase = true) == true
                    }
                }
            }
        }

        // Group by date descending
        return filtered.groupBy { it.date }
            .entries
            .sortedByDescending { it.key }
            .map { (date, items) ->
                val cash = CashTotals.fromEvents(items.flatMap {
                    when (it) {
                        is UnifiedLedgerItem.Sale -> it.representedPayments
                        is UnifiedLedgerItem.SalePayment -> listOf(it.event)
                        is UnifiedLedgerItem.Money -> listOf(it.event)
                    }
                })
                LedgerDayGroup(
                    date = date,
                    items = items,
                    totalInRupiah = cash.incomingRupiah,
                    totalOutRupiah = cash.outgoingRupiah,
                )
            }
    }

    private fun MoneyEvent.matches(query: String) = description.contains(query, ignoreCase = true) ||
        category.contains(query, ignoreCase = true) || reference?.contains(query, ignoreCase = true) == true
}
