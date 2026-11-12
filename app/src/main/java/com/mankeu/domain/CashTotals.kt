package com.mankeu.domain

/** Actual CASH debits and credits across operating, financing and investing roles (DEC-033). */
data class CashTotals(val incomingRupiah: Long, val outgoingRupiah: Long) {
    val deltaRupiah: Long get() = incomingRupiah - outgoingRupiah

    companion object {
        fun fromPostings(postings: List<Posting>): CashTotals {
            val cash = postings.filter { it.account == LedgerAccount.CASH }
            return CashTotals(
                cash.filter { it.side == PostingSide.DEBIT }.sumOf { it.amountRupiah },
                cash.filter { it.side == PostingSide.CREDIT }.sumOf { it.amountRupiah },
            )
        }

        fun fromEvents(events: List<MoneyEvent>): CashTotals =
            fromPostings(events.distinctBy { it.id }.flatMap { LedgerPolicy.journalFor(it).postings })
    }
}
