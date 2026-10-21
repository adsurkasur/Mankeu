package com.mankeu.domain

object LedgerPolicy {
    fun journalFor(event: MoneyEvent): JournalEntry {
        val amount = event.amountRupiah
        val postings = when (event.type) {
            FinancialEventType.CASH_INCOME -> pair(
                LedgerAccount.CASH to PostingSide.DEBIT,
                LedgerAccount.REVENUE to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.CASH_EXPENSE -> pair(
                LedgerAccount.OPERATING_EXPENSE to PostingSide.DEBIT,
                LedgerAccount.CASH to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.OWNER_CAPITAL -> pair(
                LedgerAccount.CASH to PostingSide.DEBIT,
                LedgerAccount.OWNER_EQUITY to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.OWNER_DRAWING -> pair(
                LedgerAccount.OWNER_DRAWING to PostingSide.DEBIT,
                LedgerAccount.CASH to PostingSide.CREDIT,
                amount,
            )

            FinancialEventType.ASSET_PURCHASE_CASH -> pair(
                LedgerAccount.EQUIPMENT_ASSET to PostingSide.DEBIT,
                LedgerAccount.CASH to PostingSide.CREDIT,
                amount,
            )

        }
        return JournalEntry(
            id = "journal-${event.id}",
            sourceEventId = event.id,
            date = event.date,
            description = event.description,
            postings = postings,
        ).requireBalanced()
    }

    private fun pair(
        first: Pair<LedgerAccount, PostingSide>,
        second: Pair<LedgerAccount, PostingSide>,
        amount: Long,
    ) = listOf(
        Posting(first.first, first.second, amount),
        Posting(second.first, second.second, amount),
    )
}
