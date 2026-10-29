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
import com.mankeu.domain.BookMode
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
    onOpenPlan: () -> Unit = {},
    onRecordTransaction: () -> Unit = {},
) {
    var section by rememberSaveable { mutableStateOf(ReportSection.FINANCIALS) }
    val planMode = state.selectedBookMode == BookMode.PROJECTION
    // Proyeksi reports from the business plan (DEC-014) whenever the unit has one; postings
    // otherwise. The plan's schedule already excludes depreciation, which is not cash.
    val plan = state.plan.takeIf { planMode }
    val summary = plan?.summary ?: state.summary
    val monthlyCashFlow = plan?.schedule ?: MonthlyCashFlowCalculator.calculateFromRecords(state.cashFlowRecords)

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
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
        item {
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

        // Nothing recorded and nothing planned: every card below would render a column of Rp0,
        // which reads as "your business made nothing" rather than "you have not told me anything
        // yet". Say which, and offer the one action that fixes it.
        if (!state.isLoading && state.eventCount == 0 && plan == null) {
            item {
                EmptyCard(
                    title = stringResource(
                        if (planMode) R.string.reports_empty_plan_title else R.string.reports_empty_title,
                    ),
                    body = stringResource(
                        if (planMode) R.string.reports_empty_plan_body else R.string.reports_empty_body,
                    ),
                    action = stringResource(if (planMode) R.string.plan_open else R.string.reports_empty_action),
                    onAction = if (planMode) onOpenPlan else onRecordTransaction,
                )
            }
            return@LazyColumn
        }

        // One AnimatedContent for the whole section body, so switching tab slides the new
        // report in rather than swapping it in place. It has to be a single LazyColumn item:
        // items are emitted into the list scope and cannot themselves be wrapped in a transition.
        // The sections are short enough to compose eagerly; the ledger-length lists all live on
        // Beranda, not here.
        item {
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
                        ReportSection.FEASIBILITY -> FeasibilitySection(state, planMode, summary, monthlyCashFlow, onOpenPlan)
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
    ReportCard(stringResource(R.string.reports_income_statement)) {
        // The income-versus-expense donut is gone with the hand-rolled charts. Two slices is not a
        // composition — it is one comparison, and the three rows immediately below already state
        // it exactly, with the figures the owner has to reconcile against a workbook. The cost
        // *breakdown* donut on Beranda, which really does have a dozen slices, stays.
        ReportRow(stringResource(R.string.reports_operating_income), summary.operatingIncomeRupiah)
        ReportRow(stringResource(R.string.reports_operating_expense), -summary.operatingExpenseRupiah)
        HorizontalDivider()
        ReportRow(stringResource(R.string.reports_net_profit), summary.profitRupiah, emphasized = true)
        if (summary.hasProfitDetail()) {
            HorizontalDivider()
            ProfitDetailRows(summary)
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
    planMode: Boolean,
    summary: com.mankeu.domain.FinancialSummary,
    monthlyCashFlow: List<com.mankeu.domain.MonthlyCashFlow>,
    onOpenPlan: () -> Unit,
) {
    val plan = state.plan
    if (planMode) {
        if (plan == null) {
            EmptyCard(
                stringResource(R.string.plan_empty_title),
                stringResource(R.string.plan_empty_body),
                stringResource(R.string.plan_open),
                onOpenPlan,
                learn = HelpArticle.PLAN_CREATE,
            )
            return
        }
        PlanFeasibilityCard(plan, onOpenPlan = onOpenPlan)
        PlanBreakdownCard(plan)
        return
    }

    // Realisasi: this month against its own break-even, then what was actually financed.
    state.actualMonth?.let { ActualMonthCard(it, state.products) }
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
 * eleven months of sales. Realisasi profit counts HPP of goods sold, fixed costs and depreciation (DEC-029 §4).
 */
@Composable
private fun ComparisonSection(state: AppUiState) {
    val planMonths = state.plan?.months?.size ?: 1
    val actualMonths = state.realizationMonths
    fun perMonth(total: Long, months: Int) =
        java.math.BigDecimal.valueOf(total).divide(java.math.BigDecimal.valueOf(months.toLong()), 0, java.math.RoundingMode.HALF_UP).toLong()
    val plan = state.projectionSummary
    val actual = state.realizationSummary
    ReportCard(stringResource(R.string.reports_comparison_title)) {
        Text(
            stringResource(R.string.reports_comparison_note, planMonths, actualMonths),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ComparisonRow(stringResource(R.string.reports_income), perMonth(plan.operatingIncomeRupiah, planMonths), perMonth(actual.operatingIncomeRupiah, actualMonths))
        ComparisonRow(stringResource(R.string.reports_operating_expense), perMonth(plan.operatingExpenseRupiah, planMonths), perMonth(actual.operatingExpenseRupiah, actualMonths), higherIsBetter = false)
        ComparisonRow(stringResource(R.string.reports_profit_loss), perMonth(plan.profitRupiah, planMonths), perMonth(actual.profitRupiah, actualMonths))
    }
}
