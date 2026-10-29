package com.mankeu.domain

object LedgerPolicy {
    fun journalFor(event: MoneyEvent): JournalEntry {
        val amount = event.amountRupiah
        val postings = when (event.type) {
            // A sale-note payment settles the receivable; the note itself is the revenue (DEC-029 §2).
            FinancialEventType.CASH_INCOME -> pair(
                LedgerAccount.CASH to PostingSide.DEBIT,
                (if (event.saleId != null) LedgerAccount.RECEIVABLE else LedgerAccount.REVENUE) to PostingSide.CREDIT,
                amount,
            )

            // Paying a credit purchase settles the payable; the cost was taken on purchase (DEC-029 §3).
            FinancialEventType.CASH_EXPENSE -> pair(
                (if (event.settlesEventId != null) LedgerAccount.PAYABLE else LedgerAccount.OPERATING_EXPENSE) to PostingSide.DEBIT,
                LedgerAccount.CASH to PostingSide.CREDIT,
                amount,
            )

            // No cash moves. Limited to expense for now: an asset bought on credit is not offered (DEC-029).
            FinancialEventType.CREDIT_PURCHASE -> pair(
                LedgerAccount.OPERATING_EXPENSE to PostingSide.DEBIT,
                LedgerAccount.PAYABLE to PostingSide.CREDIT,
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
