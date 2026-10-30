package com.mankeu.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.data.ProductRecord
import com.mankeu.domain.ActualMonthAnalysis
import com.mankeu.domain.PlanAnalysis
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.LocalMotion
import com.mankeu.presentation.toDisplayQuantity
import com.mankeu.presentation.toRupiah
import java.math.BigDecimal

/**
 * The business plan's answers (DEC-014), shared by Beranda and Laporan so the two never drift.
 *
 * [PlanFeasibilityCard] answers "will this cover its costs?" at a glance; [PlanBreakdownCard] is
 * the itemised monthly table in the lecturer's benchmark format, for checking the arithmetic;
 * [ActualMonthCard] is the same question asked of this month's Realisasi.
 */

enum class FeasibilityStatus { FEASIBLE, WATCH, NOT_FEASIBLE, UNKNOWN }

/**
 * From the R/C ratio (revenue ÷ total cost). Below 1,00 the business does not cover its costs. The
 * band between 0,95 and 1,00 is called out separately rather than lumped in with a clear loss: at
 * that distance a single price change or one more month of sales settles it.
 */
fun feasibilityStatusOf(revenueCostRatio: BigDecimal?): FeasibilityStatus = when {
    revenueCostRatio == null -> FeasibilityStatus.UNKNOWN
    revenueCostRatio >= BigDecimal.ONE -> FeasibilityStatus.FEASIBLE
    revenueCostRatio >= BigDecimal("0.95") -> FeasibilityStatus.WATCH
    else -> FeasibilityStatus.NOT_FEASIBLE
}

@Composable
fun FeasibilityStatusChip(status: FeasibilityStatus) {
    val semantic = LocalMankeuSemanticColors.current
    val (labelRes, container, content) = when (status) {
        FeasibilityStatus.FEASIBLE ->
            Triple(R.string.feasibility_status_feasible, semantic.successContainer, semantic.onSuccessContainer)
        FeasibilityStatus.WATCH ->
            Triple(R.string.feasibility_status_watch, semantic.warningContainer, semantic.onWarningContainer)
        FeasibilityStatus.NOT_FEASIBLE ->
            Triple(R.string.feasibility_status_not_feasible, semantic.dangerContainer, semantic.onDangerContainer)
        FeasibilityStatus.UNKNOWN -> Triple(
            R.string.feasibility_status_unknown,
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    // A status badge, not a chip: an AssistChip with a no-op onClick drew a ripple and read as a
    // button that did nothing. Long-press explains what the status is based on.
    MankeuTooltip(stringResource(R.string.tooltip_feasibility_status)) {
        Surface(shape = RoundedCornerShape(50), color = container, contentColor = content) {
            Text(
                stringResource(labelRes),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

/** A labelled meter: caption on the left, figure on the right, bar underneath. */
@Composable
fun MeterRow(
    label: String,
    value: String,
    progress: Float,
    trackColor: Color,
    barColor: Color,
    supporting: String? = null,
) {
    val motion = LocalMotion.current
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(maxOf(motion.mediumMillis, 1), easing = FastOutSlowInEasing),
        label = "meter",
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1)
        }
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = barColor,
            trackColor = trackColor,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        if (supporting != null) {
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun statusColor(status: FeasibilityStatus): Color {
    val semantic = LocalMankeuSemanticColors.current
    return when (status) {
        FeasibilityStatus.FEASIBLE -> semantic.success
        FeasibilityStatus.WATCH -> semantic.onWarningContainer
        FeasibilityStatus.NOT_FEASIBLE -> semantic.danger
        FeasibilityStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
    }
}

@Composable
private fun CardHeader(title: String, status: FeasibilityStatus) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                Icons.Default.Insights,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(6.dp).size(20.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        FeasibilityStatusChip(status)
    }
}

/** Revenue against break-even as a bar: full means the break-even point is reached. */
private fun progressTo(revenue: Long, breakEven: Long?): Float =
    if (breakEven == null || breakEven <= 0) 0f else (revenue.toFloat() / breakEven.toFloat())

private fun ratioText(ratio: BigDecimal?, undefined: String) = ratio?.toDisplayQuantity() ?: undefined

/** The plan at a glance: status, monthly break-even against the target, profit and capital need. */
@Composable
fun PlanFeasibilityCard(plan: PlanAnalysis, modifier: Modifier = Modifier, onOpenPlan: (() -> Unit)? = null) {
    val status = feasibilityStatusOf(plan.revenueCostRatio)
    val undefined = stringResource(R.string.reports_undefined)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CardHeader(stringResource(R.string.feasibility_title), status)
            if (plan.seasonal) {
                Text(stringResource(R.string.plan_seasonal_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val bep = plan.breakEvenRupiah
            if (bep != null) {
                MeterRow(
                    label = stringResource(R.string.plan_bep_month),
                    value = bep.toRupiah(),
                    progress = progressTo(plan.revenueRupiah, bep),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    barColor = statusColor(status),
                    supporting = if (plan.revenueRupiah >= bep) {
                        stringResource(R.string.plan_target_above, plan.revenueRupiah.toRupiah())
                    } else {
                        stringResource(R.string.plan_target_below, plan.revenueRupiah.toRupiah(), (bep - plan.revenueRupiah).toRupiah())
                    },
                )
            } else if (plan.products.isNotEmpty()) {
                Text(stringResource(R.string.plan_no_mix), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            plan.products.forEach { item ->
                val units = item.breakEvenUnits
                ReportRow(
                    stringResource(R.string.plan_bep_product, item.product.name),
                    if (units == null) undefined else "${units.toDisplayQuantity()} ${item.product.unit}",
                )
                if (item.sellsAtALoss) {
                    Text(
                        stringResource(R.string.plan_product_loss, item.product.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalMankeuSemanticColors.current.danger,
                    )
                }
            }
            HorizontalDivider()
            ReportRow(stringResource(R.string.plan_profit_month), plan.profitRupiah, emphasized = true)
            Text(
                stringResource(R.string.feasibility_rc_caption, ratioText(plan.revenueCostRatio, undefined)) + "  ·  " +
                    stringResource(R.string.feasibility_bcr_caption, ratioText(plan.benefitCostRatio, undefined)),
                fontWeight = FontWeight.Bold,
            )
            if (plan.capitalNeedRupiah > 0) {
                ReportRow(stringResource(R.string.plan_capital_need), plan.capitalNeedRupiah, highlightNegative = false)
                ReportRow(stringResource(R.string.plan_capital_planned), plan.plannedCapitalRupiah, highlightNegative = false)
                ReportRow(stringResource(R.string.plan_capital_shortfall), plan.capitalShortfallRupiah, emphasized = plan.capitalShortfallRupiah > 0, highlightNegative = false)
                Text(stringResource(R.string.plan_capital_need_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onOpenPlan != null) {
                FilledTonalButton(onClick = onOpenPlan, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.plan_open))
                }
            }
        }
    }
}

/**
 * The plan's monthly cost and revenue, itemised the way the lecturer's benchmark lays it out:
 * each fixed cost, one-off costs spread over the period, depreciation, then variable cost,
 * total cost, revenue, profit, R/C and B/C, and each product's unit economics.
 */
@Composable
fun PlanBreakdownCard(plan: PlanAnalysis, modifier: Modifier = Modifier) {
    val undefined = stringResource(R.string.reports_undefined)
    ReportCard(stringResource(R.string.plan_breakdown_title), modifier) {
        Text(stringResource(R.string.plan_fixed_header), fontWeight = FontWeight.Bold)
        plan.fixedCost.recurring.forEach { (name, amount) -> ReportRow(name, amount, highlightNegative = false) }
        plan.fixedCost.oneOffSpread.forEach { (name, amount) ->
            ReportRow(stringResource(R.string.plan_oneoff_spread, name), amount, highlightNegative = false)
        }
        plan.fixedCost.depreciation.forEach { (name, amount) ->
            ReportRow(stringResource(R.string.plan_depreciation, name), amount, highlightNegative = false)
        }
        ReportRow(stringResource(R.string.plan_fixed_total), plan.fixedCost.totalRupiah, emphasized = true)
        if (plan.breakEvenBaseRupiah != plan.fixedCost.totalRupiah) {
            ReportRow(stringResource(R.string.plan_bep_base), plan.breakEvenBaseRupiah, highlightNegative = false)
            Text(stringResource(R.string.plan_bep_base_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
        ReportRow(stringResource(R.string.plan_variable_total), plan.variableCostRupiah)
        ReportRow(stringResource(R.string.plan_total_cost), plan.totalCostRupiah, emphasized = true)
        ReportRow(stringResource(R.string.plan_revenue), plan.revenueRupiah)
        ReportRow(stringResource(R.string.plan_profit), plan.profitRupiah, emphasized = true)
        Text(stringResource(R.string.reports_rc_ratio, ratioText(plan.revenueCostRatio, undefined)), fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.reports_bc_ratio, ratioText(plan.benefitCostRatio, undefined)), fontWeight = FontWeight.Bold)
        plan.products.forEach { item ->
            HorizontalDivider()
            Text(item.product.name, fontWeight = FontWeight.Bold)
            ReportRow(stringResource(R.string.plan_price_unit, item.product.unit), item.product.priceRupiah)
            ReportRow(stringResource(R.string.plan_vc_unit), item.variableCostPerUnit, highlightNegative = false)
            ReportRow(stringResource(R.string.plan_cm_unit), item.contributionMargin)
            item.product.targetPerMonth?.let {
                ReportRow(stringResource(R.string.plan_target_month), "${it.toDisplayQuantity()} ${item.product.unit}")
            }
            ReportRow(
                stringResource(R.string.feasibility_bep_unit_label),
                item.breakEvenUnits?.let { "${it.toDisplayQuantity()} ${item.product.unit}" } ?: undefined,
            )
            item.breakEvenRupiah?.let { ReportRow(stringResource(R.string.feasibility_bep_money_label), it) }
        }
    }
}

/** True when the month has anything for [ProfitDetailRows] to show. */
fun com.mankeu.domain.MonthlySummary.hasProfitDetail() =
    hppRupiah != 0L || productionSpendRupiah != 0L || receivableRupiah != 0L || payableRupiah != 0L

/**
 * HPP, production spend and its variance with one plain sentence (F2-1/F2-8, #19), then what customers and
 * suppliers are still owed. Minimal until F4 redesigns the reports.
 */
@Composable
fun ProfitDetailRows(
    summary: com.mankeu.domain.MonthlySummary,
    /** False on Beranda, where the Tagihan card already shows both amounts. */
    withBills: Boolean = true,
) {
    if (summary.hppRupiah != 0L || summary.productionSpendRupiah != 0L) {
        ReportRow(stringResource(R.string.profit_hpp_sold), summary.hppRupiah, highlightNegative = false)
        ReportRow(stringResource(R.string.profit_production_spend), summary.productionSpendRupiah, highlightNegative = false)
        ReportRow(stringResource(R.string.profit_variance), summary.productionVarianceRupiah, highlightNegative = false)
        val variance = summary.productionVarianceRupiah
        if (variance != 0L) {
            Text(
                stringResource(if (variance > 0) R.string.profit_variance_over else R.string.profit_variance_under, kotlin.math.abs(variance).toRupiah()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (withBills && (summary.receivableRupiah != 0L || summary.payableRupiah != 0L)) {
        ReportRow(stringResource(R.string.profit_receivable), summary.receivableRupiah, highlightNegative = false)
        ReportRow(stringResource(R.string.profit_payable), summary.payableRupiah, highlightNegative = false)
    }
}

/** "Status BEP bulan ini": this month's Realisasi against its own break-even point. */
@Composable
fun ActualMonthCard(
    actual: ActualMonthAnalysis,
    products: List<ProductRecord>,
    /** The same month's [com.mankeu.domain.MonthlySummary]: its spend-vs-HPP gap is the one every screen shows (U2, L27). */
    month: com.mankeu.domain.MonthlySummary?,
    modifier: Modifier = Modifier,
) {
    // No sales yet (a planting or set-up month) is not "not feasible": there is nothing to judge.
    val noSales = actual.revenueRupiah == 0L
    val status = if (noSales) FeasibilityStatus.UNKNOWN else feasibilityStatusOf(actual.revenueCostRatio)
    val undefined = stringResource(R.string.reports_undefined)
    val semantic = LocalMankeuSemanticColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardHeader(stringResource(R.string.actual_title), status)
            val bep = actual.breakEvenRupiah
            if (bep != null) {
                MeterRow(
                    label = stringResource(R.string.actual_revenue),
                    value = actual.revenueRupiah.toRupiah(),
                    progress = progressTo(actual.revenueRupiah, bep),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    barColor = if (actual.reached == true) semantic.success else MaterialTheme.colorScheme.primary,
                    supporting = if (actual.reached == true) {
                        stringResource(R.string.actual_reached, bep.toRupiah())
                    } else {
                        stringResource(R.string.actual_short, (bep - actual.revenueRupiah).toRupiah(), bep.toRupiah())
                    },
                )
            } else {
                ReportRow(stringResource(R.string.actual_revenue), actual.revenueRupiah)
                Text(
                    stringResource(if (noSales && products.isNotEmpty()) R.string.actual_no_sales else R.string.actual_no_bep),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val records = products.associateBy { it.product.id }
            // Units are counted in the main sale unit (PlanAssembly.actualMonth), so name that unit.
            actual.unitsSold.forEach { (id, units) ->
                records[id]?.let { ReportRow(stringResource(R.string.actual_units_sold, it.product.name), "${units.toDisplayQuantity()} ${it.mainUnit?.name ?: it.product.unit}") }
            }
            ReportRow(stringResource(R.string.actual_fixed_cost), actual.fixedCostRupiah, highlightNegative = false)
            ReportRow(stringResource(R.string.plan_total_cost), actual.totalCostRupiah, highlightNegative = false)
            ReportRow(stringResource(R.string.actual_profit), actual.profitRupiah, emphasized = true)
            // The gap at note HPP snapshots, not at today's recipe cost: one selisih per month (audit L27).
            val variance = month?.productionVarianceRupiah ?: 0L
            if (month != null && month.hppRupiah > 0 && variance != 0L) {
                Text(
                    stringResource(if (variance > 0) R.string.profit_variance_over else R.string.profit_variance_under, kotlin.math.abs(variance).toRupiah()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!noSales) {
                Text(
                    stringResource(R.string.reports_rc_ratio, ratioText(actual.revenueCostRatio, undefined)),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
