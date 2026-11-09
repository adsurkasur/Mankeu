package com.mankeu.presentation.screens

import com.mankeu.presentation.guide.HelpArticle
import com.mankeu.presentation.FeedbackEvent
import com.mankeu.presentation.LocalFeedback
import com.mankeu.presentation.components.MankeuTooltip

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import java.math.BigDecimal
import java.math.RoundingMode
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mankeu.presentation.components.SkeletonHero
import com.mankeu.presentation.components.SkeletonListItem
import com.mankeu.domain.MonthlyCashFlowCalculator
import com.mankeu.R
import com.mankeu.presentation.AppUiState
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.LocalMotion
import com.mankeu.presentation.appLocale
import com.mankeu.presentation.components.MankeuSheet
import com.mankeu.presentation.components.ComparisonRow
import com.mankeu.presentation.components.DateField
import com.mankeu.presentation.components.MoneyField
import com.mankeu.presentation.components.sanitizeDecimal
import com.mankeu.presentation.components.EmptyCard
import com.mankeu.presentation.components.MetricTone
import com.mankeu.presentation.components.ReportCard
import com.mankeu.presentation.components.ActualMonthCard
import com.mankeu.presentation.components.ProfitDetailRows
import com.mankeu.presentation.components.hasProfitDetail
import com.mankeu.presentation.components.PlanBreakdownCard
import com.mankeu.presentation.components.PlanFeasibilityCard
import com.mankeu.presentation.components.ReportRow
import com.mankeu.presentation.components.SectionTitle
import com.mankeu.presentation.toDisplayDate
import com.mankeu.domain.TransactionCalculation
import com.mankeu.presentation.toDisplayQuantity
import com.mankeu.presentation.toRupiah
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.mankeu.presentation.components.charts.MonthlyCashFlowChart

private enum class ReportSection(val labelRes: Int) {
    FINANCIALS(R.string.reports_tab_financials),
    FEASIBILITY(R.string.reports_tab_feasibility),
    COMPARISON(R.string.reports_tab_comparison),
}

/**
 * The Laporan (reports) destination. Every number here is re-derived from posted journal
 * entries (see `docs/DOMAIN_MODEL.md`) — this screen never edits source data, only presents it.
 */
@Composable
fun ReportsScreen(
    state: AppUiState,
    onRecordTransaction: () -> Unit = {},
) {
    var section by rememberSaveable { mutableStateOf(ReportSection.FINANCIALS) }
    // U4: no Rencana/Realisasi switch; Laporan reports the records. The plan is read only by
    // Kelayakan's empty state and by Uang vs rencana (projectionSummary).
    val summary = state.summary
    val monthlyCashFlow = remember(state.cashFlowRecords) { MonthlyCashFlowCalculator.calculateFromRecords(state.cashFlowRecords) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "reports_title") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle(
                    title = stringResource(R.string.reports_title),
                    subtitle = stringResource(R.string.reports_subtitle),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item(key = "reports_tabs") {
            val feedback = LocalFeedback.current
            PrimaryTabRow(
                selectedTabIndex = ReportSection.entries.indexOf(section),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ReportSection.entries.forEach { item ->
                    Tab(
                        selected = section == item,
                        onClick = {
                            if (section != item) feedback(FeedbackEvent.TAP)
                            section = item
                        },
                        text = {
                            // Two lines, not one with an ellipsis: the tab names say which
                            // statement each section holds ("Laba Rugi dan Arus Kas"), and a name
                            // clipped to "Laba Rug…" would defeat the point of naming them at all.
                            // Fixed rather than scrollable so all three stay visible at once.
                            Text(
                                stringResource(item.labelRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (section == item) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }

        // The month has not arrived yet (U2 re-review N-1): placeholders, never a column of Rp0.
        if (state.isLoading || (state.selectedProjectId != null && state.thisMonth == null)) {
            item(key = "reports_skeleton_hero") { SkeletonHero() }
            items(2, key = { "reports_skeleton_$it" }) { SkeletonListItem() }
            return@LazyColumn
        }
        // Nothing recorded: every card below would render a column of Rp0, which reads as "your
        // business made nothing" rather than "you have not told me anything yet".
        if (state.eventCount == 0 && state.sales.isEmpty()) {
            item(key = "reports_empty") {
                EmptyCard(
                    title = stringResource(R.string.reports_empty_title),
                    body = stringResource(R.string.reports_empty_body),
                    action = stringResource(R.string.reports_empty_action),
                    onAction = onRecordTransaction,
                )
            }
            return@LazyColumn
        }

        // One AnimatedContent for the whole section body, so switching tab slides the new
        // report in rather than swapping it in place. It has to be a single LazyColumn item:
        // items are emitted into the list scope and cannot themselves be wrapped in a transition.
        // The sections are short enough to compose eagerly; the ledger-length lists all live on
        // Beranda, not here.
        item(key = "reports_body") {
            val motion = LocalMotion.current
            AnimatedContent(
                targetState = section,
                transitionSpec = {
                    val forward = ReportSection.entries.indexOf(targetState) >= ReportSection.entries.indexOf(initialState)
                    val enterMillis = maxOf(motion.mediumMillis, 1)
                    val exitMillis = maxOf(motion.fastMillis, 1)
                    (slideInHorizontally(tween(enterMillis, easing = FastOutSlowInEasing)) { full ->
                        if (forward) full / 8 else -full / 8
                    } + fadeIn(tween(enterMillis)))
                        .togetherWith(fadeOut(tween(exitMillis)))
                        .using(SizeTransform(clip = false))
                },
                label = "reportSection",
            ) { current ->
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    when (current) {
                        ReportSection.FINANCIALS -> FinancialsSection(state, summary, monthlyCashFlow)
                        ReportSection.FEASIBILITY -> FeasibilitySection(state, summary, monthlyCashFlow)
                        ReportSection.COMPARISON -> ComparisonSection(state)
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialsSection(
    state: AppUiState,
    summary: com.mankeu.domain.FinancialSummary,
    monthlyCashFlow: List<com.mankeu.domain.MonthlyCashFlow>,
) {
    // The current month's MonthlySummary, named by month (U2); F4 adds a month selector. ReportsScreen
    // shows placeholders until it arrives, so it is never null here.
    val month = state.thisMonth ?: return
    val title = stringResource(R.string.reports_income_statement_month, month.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", appLocale())))
    ReportCard(title) {
        // The income-versus-expense donut is gone with the hand-rolled charts. Two slices is not a
        // composition — it is one comparison, and the three rows immediately below already state
        // it exactly, with the figures the owner has to reconcile against a workbook. The cost
        // *breakdown* donut on Beranda, which really does have a dozen slices, stays.
        ReportRow(stringResource(R.string.reports_operating_income), month.incomeRupiah)
        ReportRow(stringResource(R.string.reports_operating_expense), -(month.hppRupiah + month.otherCostRupiah))
        HorizontalDivider()
        ReportRow(stringResource(R.string.reports_net_profit), month.profitRupiah, emphasized = true)
        if (month.hasProfitDetail()) {
            HorizontalDivider()
            ProfitDetailRows(month)
        }
        Text(
            stringResource(R.string.reports_income_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    ReportCard(stringResource(R.string.reports_cash_flow)) {
        ReportRow(stringResource(R.string.reports_cf_operating), summary.operatingCashFlowRupiah)
        ReportRow(stringResource(R.string.reports_cf_investing), summary.investingCashFlowRupiah)
        ReportRow(stringResource(R.string.reports_cf_financing), summary.financingCashFlowRupiah)
        HorizontalDivider()
        ReportRow(stringResource(R.string.reports_cf_closing), summary.cashBalanceRupiah, emphasized = true)
    }
    // Moved from Beranda (U4); a mix of one or two categories says nothing (audit L19).
    ExpenseDistributionCard(state)
    if (monthlyCashFlow.isNotEmpty()) {
        SectionTitle(stringResource(R.string.reports_monthly_title), stringResource(R.string.reports_monthly_subtitle))
        val locale = appLocale()
        
        MonthlyCashFlowChart(
            months = monthlyCashFlow,
            modifier = Modifier.padding(bottom = 16.dp),
            locale = locale,
        )
        
        monthlyCashFlow.forEach { month ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(month.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)), fontWeight = FontWeight.Black)
                    ReportRow(stringResource(R.string.reports_monthly_operating), month.operatingRupiah)
                    ReportRow(stringResource(R.string.reports_monthly_investing), month.investingRupiah)
                    ReportRow(stringResource(R.string.reports_monthly_financing), month.financingRupiah)
                    HorizontalDivider()
                    ReportRow(stringResource(R.string.reports_monthly_net), month.netRupiah, emphasized = true)
                    ReportRow(stringResource(R.string.reports_monthly_cumulative), month.cumulativeRupiah, emphasized = true)
                }
            }
        }
    }
}

@Composable
private fun FeasibilitySection(
    state: AppUiState,
    summary: com.mankeu.domain.FinancialSummary,
    monthlyCashFlow: List<com.mankeu.domain.MonthlyCashFlow>,
) {
    // This month against its own break-even, then what was actually financed. The plan's own
    // feasibility is on the Rencana usaha page (U4).
    state.actualMonth?.let { ActualMonthCard(it, state.products, state.thisMonth) }
    ReportCard(stringResource(R.string.reports_financing_actual_title)) {
        Text(
            stringResource(R.string.reports_financing_actual_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReportRow(stringResource(R.string.reports_cf_operating), summary.operatingCashFlowRupiah)
        ReportRow(stringResource(R.string.reports_cf_investing), summary.investingCashFlowRupiah)
        ReportRow(stringResource(R.string.reports_financing_net), summary.financingCashFlowRupiah)
        HorizontalDivider()
        ReportRow(stringResource(R.string.reports_cf_closing), summary.cashBalanceRupiah, emphasized = true)
    }
    // The deepest the business went below zero before counting capital paid in: what it had to
    // be financed with.
    var cumulative = 0L
    val need = monthlyCashFlow.maxOfOrNull { month ->
        cumulative += month.operatingRupiah + month.investingRupiah
        -cumulative
    }?.coerceAtLeast(0) ?: 0
    ReportCard(stringResource(R.string.reports_working_capital)) {
        ReportRow(stringResource(R.string.reports_financing_gap), need, emphasized = true)
        Text(
            stringResource(R.string.reports_working_capital_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Plan against actual as averages per month. The plan covers its own period (often 12 months)
 * and Realisasi only the months recorded so far, so comparing totals produced a "shortfall" of
 * eleven months of sales. Both sides are cash, before depreciation ([com.mankeu.domain.PlanComparison]).
 */
@Composable
private fun ComparisonSection(state: AppUiState) {
    val planMonths = state.plan?.months?.size ?: 1
    val actualMonths = state.realizationMonths
    // Realisasi: the money in and out of every MonthlySummary (U2), cash basis; never called profit.
    val rows = com.mankeu.domain.PlanComparison.perMonth(
        state.projectionSummary, planMonths,
        state.monthlySummaries.sumOf { it.moneyInRupiah }, state.monthlySummaries.sumOf { it.moneyOutRupiah }, actualMonths,
    )
    ReportCard(stringResource(R.string.reports_comparison_title)) {
        Text(
            stringResource(R.string.reports_comparison_note, planMonths, actualMonths),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ComparisonRow(stringResource(R.string.reports_money_in), rows.planIncome, rows.actualIncome)
        ComparisonRow(stringResource(R.string.reports_money_out), rows.planExpense, rows.actualExpense, higherIsBetter = false)
        ComparisonRow(stringResource(R.string.reports_profit_loss), rows.planProfit, rows.actualProfit)
    }
}

@Composable
private fun ExpenseDistributionCard(
    state: AppUiState,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val otherLabel = stringResource(R.string.category_other)
    // Six category hues, harmonised toward the brand green and re-toned per theme; see
    // MankeuSemanticColors.chart. The raw Material 2 hues that used to live here ignored the palette
    // and stayed identical in dark mode, where they glared. Read outside the remember block: it is
    // a composition-local read, and it has to re-run when the theme changes.
    val palette = LocalMankeuSemanticColors.current.chart
    val expenseCategories = remember(state.categoryTotals, otherLabel, palette) {
        val categoryExpenses = mutableMapOf<String, Long>()
        // Summed by the database (`observeCategoryTotals`). Doing it here meant holding every event in
        // memory to add up a dozen slices, and it silently went wrong the moment the event list
        // was capped for display.
        state.categoryTotals.forEach { row ->
            val cat = row.category.ifBlank { otherLabel }
            categoryExpenses[cat] = (categoryExpenses[cat] ?: 0L) + row.totalRupiah
        }
        val total = categoryExpenses.values.sum()
        if (total == 0L) emptyList()
        else {
            categoryExpenses.entries
                .sortedByDescending { it.value }
                .mapIndexed { index, entry ->
                    ExpenseSlice(
                        category = entry.key,
                        amount = entry.value,
                        percentage = (entry.value.toDouble() / total.toDouble()) * 100.0,
                        color = palette[index % palette.size],
                    )
                }
        }
    }

    if (expenseCategories.size >= 3) {
        val totalExpense = expenseCategories.sumOf { it.amount }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.large,
            modifier = modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.home_expense_mix),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        totalExpense.toRupiah(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Donut Chart Canvas
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.size(90.dp)) {
                            var startAngle = -90f
                            val strokeWidth = 20.dp.toPx()
                            expenseCategories.forEach { slice ->
                                val sweep = (slice.percentage / 100.0 * 360.0).toFloat()
                                drawArc(
                                    color = slice.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth),
                                )
                                startAngle += sweep
                            }
                        }
                        Text(
                            stringResource(R.string.home_expense_mix_center, expenseCategories.size),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Legend
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        expenseCategories.take(4).forEach { slice ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(slice.color, CircleShape),
                                )
                                Text(
                                    slice.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${String.format(appLocale(), "%.1f", slice.percentage)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ExpenseSlice(
    val category: String,
    val amount: Long,
    val percentage: Double,
    val color: Color,
)
