package com.mankeu.domain

data class PostedRecord(
    val eventId: String,
    val cashFlowRole: CashFlowRole,
    val account: LedgerAccount,
    val side: PostingSide,
    val amountRupiah: Long,
)

object PostingReportCalculator {
    fun calculate(records: List<PostedRecord>): FinancialSummary {
        val income = creditBalance(records, LedgerAccount.REVENUE)
        val operatingExpense = debitBalance(records, LedgerAccount.OPERATING_EXPENSE)
        val cashBalance = debitBalance(records, LedgerAccount.CASH)

        fun cashFlow(role: CashFlowRole): Long = records
            .filter { it.account == LedgerAccount.CASH && it.cashFlowRole == role }
            .sumOf { if (it.side == PostingSide.DEBIT) it.amountRupiah else -it.amountRupiah }

        return FinancialSummary(
            operatingIncomeRupiah = income,
            operatingExpenseRupiah = operatingExpense,
            profitRupiah = income - operatingExpense,
            operatingCashFlowRupiah = cashFlow(CashFlowRole.OPERATING),
            financingCashFlowRupiah = cashFlow(CashFlowRole.FINANCING),
            investingCashFlowRupiah = cashFlow(CashFlowRole.INVESTING),
            cashBalanceRupiah = cashBalance,
            // Notes post no revenue (DEC-029 §2), so this is minus what was paid on notes until the
            // repository adds the note totals; the payable is complete here.
            receivableRupiah = debitBalance(records, LedgerAccount.RECEIVABLE),
            operatingCashInRupiah = records.filter { it.account == LedgerAccount.CASH && it.cashFlowRole == CashFlowRole.OPERATING && it.side == PostingSide.DEBIT }.sumOf { it.amountRupiah },
            operatingCashOutRupiah = records.filter { it.account == LedgerAccount.CASH && it.cashFlowRole == CashFlowRole.OPERATING && it.side == PostingSide.CREDIT }.sumOf { it.amountRupiah },
            payableRupiah = creditBalance(records, LedgerAccount.PAYABLE),
        )
    }

    private fun debitBalance(records: List<PostedRecord>, account: LedgerAccount): Long = records
        .filter { it.account == account }
        .sumOf { if (it.side == PostingSide.DEBIT) it.amountRupiah else -it.amountRupiah }

    private fun creditBalance(records: List<PostedRecord>, account: LedgerAccount): Long = records
        .filter { it.account == account }
        .sumOf { if (it.side == PostingSide.CREDIT) it.amountRupiah else -it.amountRupiah }
}
