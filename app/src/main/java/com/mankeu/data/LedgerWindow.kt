package com.mankeu.data

import com.mankeu.domain.MoneyEvent
import java.time.LocalDate

/** One transaction-coherent Catatan read. All shown days are complete; note details retain history. */
data class LedgerWindow(
    val projectId: String,
    val bookId: String,
    val oldestDate: LocalDate?,
    val events: List<MoneyEvent>,
    val sales: List<SaleRecord>,
    val hasEarlier: Boolean,
    /** Complete linked-payment history for status; these amounts never become visible cash rows. */
    val supplierPaidRupiah: Map<String, Long> = emptyMap(),
) {
    val loadedRows: Int get() = events.size + sales.count { oldestDate == null || it.sale.dateEpochDay >= oldestDate.toEpochDay() }
}
