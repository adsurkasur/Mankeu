package com.mankeu.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.data.SaleRecord
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.MonthlySummary
import com.mankeu.presentation.AppUiState
import com.mankeu.presentation.Destination
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.MankeuCard
import com.mankeu.presentation.MankeuType
import com.mankeu.presentation.Space
import com.mankeu.presentation.components.EmptyCard
import com.mankeu.presentation.components.NoticeCard
import com.mankeu.presentation.components.SkeletonHero
import com.mankeu.presentation.components.SkeletonListItem
import com.mankeu.presentation.components.form.ListRowField
import com.mankeu.presentation.guide.HelpArticle
import com.mankeu.presentation.toRupiah
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Beranda (U4, plan ruling 4): answers "how is the business this month, and what do I do now".
 *
 * One main number (Untung bulan ini, tap for its four lines), the cash on hand, the three quick
 * actions, who has not paid yet by name, supplier debt when there is any, real low-stock warnings
 * and the way into Rencana usaha. Every money figure is this month's [MonthlySummary] (P7); the
 * charts and reconciliation lines live in Laporan.
 */
@Composable
fun HomeScreen(
    state: AppUiState,
    onNavigate: (Destination) -> Unit,
    onManageScope: () -> Unit,
    onOpenPlan: () -> Unit = {},
    /** Opens the entry form for a kind; OWNER_CAPITAL presets money in with the capital category. */
    onQuickRecord: (FinancialEventType) -> Unit = {},
    /** Opens the Catatan note list on a filter. */
    onOpenBills: (NoteFilter) -> Unit = {},
    onNewSale: () -> Unit = {},
    /** Product picker, then the production sheet. */
    onProduce: () -> Unit = {},
    onNewProduct: () -> Unit = {},
    /** Kontak, searched on this name ("" = the whole list). U5 replaces it with the customer page. */
    onOpenContact: (String) -> Unit = {},
    expanded: Boolean = false,
) {
    val month = state.thisMonth
    LazyColumn(
        contentPadding = PaddingValues(
            start = if (expanded) Space.Section else Space.Edge,
            end = if (expanded) Space.Section else Space.Edge,
            top = Space.Edge,
            bottom = Space.Section,
        ),
        verticalArrangement = Arrangement.spacedBy(Space.Edge),
    ) {
        // Loading, or the month has not arrived yet (U2 re-review N-1): placeholders, never zeros.
        if (state.isLoading || (state.projects.isNotEmpty() && month == null)) {
            item { SkeletonHero() }
            items(3) { SkeletonListItem() }
            return@LazyColumn
        }
        if (state.businesses.isEmpty()) {
            item {
                EmptyCard(
                    stringResource(R.string.setup_business_title),
                    stringResource(R.string.setup_business_body),
                    stringResource(R.string.scope_add_business),
                    onManageScope,
                    learn = HelpArticle.START_SCOPE,
                )
            }
            return@LazyColumn
        }
        if (state.projects.isEmpty() || month == null) {
            item {
                EmptyCard(
                    stringResource(R.string.setup_project_title),
                    stringResource(R.string.setup_project_body),
                    stringResource(R.string.scope_add_project),
                    onManageScope,
                    learn = HelpArticle.START_SCOPE,
                )
            }
            return@LazyColumn
        }
        // Nothing recorded yet: the first three steps instead of a column of Rp0.
        val noData = state.sales.isEmpty() && state.eventCount == 0 &&
            month.cashOnHandRupiah == 0L && month.moneyInRupiah == 0L && month.moneyOutRupiah == 0L
        if (noData) {
            item {
                FirstSteps(
                    hasProduct = state.products.isNotEmpty(),
                    onNewProduct = onNewProduct,
                    onNewSale = onNewSale,
                    onSeeProfit = { onNavigate(Destination.REPORTS) },
                )
            }
            item { ListRowField(stringResource(R.string.home_plan_row), "", onOpenPlan) }
            return@LazyColumn
        }

        item {
            val last = state.monthlySummaries.firstOrNull { it.month == month.month.minusMonths(1) }
            ProfitCard(month, last)
        }
        item { ListRowField(stringResource(R.string.home_cash_on_hand), month.cashOnHandRupiah.toRupiah(), { onNavigate(Destination.REPORTS) }) }
        // A first-time owner who records sales but not the money already in the drawer sees a negative
        // balance and reads it as a loss (audit 2026-10-22): say why and offer the entry that fixes it.
        if (month.cashOnHandRupiah < 0 && state.summary.financingCashFlowRupiah <= 0) {
            item {
                EmptyCard(
                    stringResource(R.string.home_negative_cash_title),
                    stringResource(R.string.home_negative_cash_body),
                    stringResource(R.string.home_negative_cash_action),
                    { onQuickRecord(FinancialEventType.OWNER_CAPITAL) },
                    learn = HelpArticle.FAQ_CASH_NEGATIVE,
                )
            }
        }
        item {
            QuickActions(
                onSell = onNewSale,
                onBuy = { onQuickRecord(FinancialEventType.CASH_EXPENSE) },
                onProduce = onProduce,
            )
        }
        // "Who owes me now" (U4 review I-1, controller ruling): rows and total from one source, the open
        // notes dated up to today; Laporan keeps MonthlySummary's month-end receivable.
        val bills = customerBills(state.sales, LocalDate.now())
        if (bills.isNotEmpty()) {
            item {
                BillsCard(
                    totalRupiah = bills.sumOf { it.outstandingRupiah },
                    bills = bills,
                    onOpenContact = onOpenContact,
                    onReceive = { onOpenBills(NoteFilter.OPEN) },
                )
            }
        }
        if (month.payableRupiah != 0L) {
            item {
                // Neutral: the warning colour is kept for what customers owe, so two brown figures do not compete.
                MankeuCard(onClick = { onOpenContact("") }) {
                    Column {
                        Text(stringResource(R.string.home_payable_title), style = MankeuType.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(month.payableRupiah.toRupiah(), style = MankeuType.Figure)
                    }
                }
            }
        }
        val low = state.stockItems.filter { it.isLowStock }
        items(low, key = { "low-" + it.id }) { item ->
            WarningRow(stringResource(R.string.home_low_stock, item.name, item.currentStock.toQuantityText(), item.unit)) {
                onNavigate(Destination.PRODUCTS)
            }
        }
        item { ListRowField(stringResource(R.string.home_plan_row), "", onOpenPlan) }
        items(state.notices.take(3), key = { it.id }) { notice ->
            NoticeCard(notice.title, notice.explanation, Icons.Default.Info)
        }
    }
}

/** What one customer still owes on open notes, and how long the oldest of them has been open. [name] null = "Umum". */
internal data class CustomerBill(val name: String?, val outstandingRupiah: Long, val ageDays: Int)

/**
 * Open notes dated up to [today], grouped by customer, largest debt first. Notes without a customer are one
 * group ("Umum"); notes dated in the future are not owed yet. The Tagihan card's total is the sum of these.
 */
internal fun customerBills(sales: List<SaleRecord>, today: LocalDate): List<CustomerBill> =
    sales.filter { it.outstanding > 0 && it.sale.dateEpochDay <= today.toEpochDay() }
        .groupBy { it.contact?.id }
        .map { (_, notes) ->
            val oldest = LocalDate.ofEpochDay(notes.minOf { it.sale.dateEpochDay })
            CustomerBill(notes.first().contact?.name, notes.sumOf { it.outstanding }, ChronoUnit.DAYS.between(oldest, today).toInt().coerceAtLeast(0))
        }
        .sortedByDescending { it.outstandingRupiah }

@Composable
private fun ProfitCard(month: MonthlySummary, lastMonth: MonthlySummary?) {
    val semantic = LocalMankeuSemanticColors.current
    var open by rememberSaveable { mutableStateOf(false) }
    val profit = month.profitRupiah
    MankeuCard(onClick = { open = !open }) {
        // Label, figure and comparison read as one block; the card's 12 dp gap separates blocks.
        Column {
            Text(stringResource(R.string.home_profit), style = MankeuType.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                profit.toRupiah(),
                style = MankeuType.Figure,
                color = when {
                    profit > 0 -> semantic.success
                    profit < 0 -> semantic.danger
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            val diff = lastMonth?.let { profit - it.profitRupiah }
            Text(
                when {
                    diff == null -> stringResource(R.string.home_profit_tap)
                    diff > 0 -> stringResource(R.string.home_vs_last_up, diff.toRupiah())
                    diff < 0 -> stringResource(R.string.home_vs_last_down, (-diff).toRupiah())
                    else -> stringResource(R.string.home_vs_last_same)
                },
                style = MankeuType.Caption,
                color = when {
                    diff != null && diff > 0 -> semantic.success
                    diff != null && diff < 0 -> semantic.danger
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        AnimatedVisibility(open) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.Small)) {
                HorizontalDivider(Modifier.padding(bottom = Space.Small))
                AmountLine(stringResource(R.string.home_breakdown_sales), month.salesRupiah)
                if (month.otherIncomeRupiah != 0L) AmountLine(stringResource(R.string.home_breakdown_other_income), month.otherIncomeRupiah)
                AmountLine(stringResource(R.string.home_breakdown_hpp), month.hppRupiah)
                AmountLine(stringResource(R.string.home_breakdown_other_cost), month.otherCostRupiah)
                HorizontalDivider()
                AmountLine(stringResource(R.string.home_breakdown_profit), profit, bold = true)
            }
        }
    }
}

@Composable
private fun AmountLine(label: String, amount: Long, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MankeuType.Body, modifier = Modifier.weight(1f))
        Text(amount.toRupiah(), style = MankeuType.Body, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun QuickActions(onSell: () -> Unit, onBuy: () -> Unit, onProduce: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
        QuickAction(stringResource(R.string.home_quick_sell), Icons.Default.PointOfSale, onSell, Modifier.weight(1f))
        QuickAction(stringResource(R.string.home_quick_buy), Icons.Default.ShoppingCart, onBuy, Modifier.weight(1f))
        QuickAction(stringResource(R.string.home_quick_produce), Icons.Default.Factory, onProduce, Modifier.weight(1f))
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = Space.RowTwoLine),
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(Space.Small),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null)
            Text(label, style = MankeuType.Body, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun BillsCard(totalRupiah: Long, bills: List<CustomerBill>, onOpenContact: (String) -> Unit, onReceive: () -> Unit) {
    val semantic = LocalMankeuSemanticColors.current
    val noName = stringResource(R.string.home_bill_no_name)
    MankeuCard {
        Column {
            Text(stringResource(R.string.home_receivable_title), style = MankeuType.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(totalRupiah.toRupiah(), style = MankeuType.Figure, color = semantic.warning)
        }
        Column {
            bills.take(3).forEachIndexed { index, bill ->
                if (index > 0) HorizontalDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = Space.Row)
                        .clickable(role = Role.Button) { if (bill.name != null) onOpenContact(bill.name) else onReceive() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.Field),
                ) {
                    Text(bill.name ?: noName, style = MankeuType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(
                        when (bill.ageDays) {
                            0 -> stringResource(R.string.home_bill_today, bill.outstandingRupiah.toRupiah())
                            1 -> stringResource(R.string.home_bill_yesterday, bill.outstandingRupiah.toRupiah())
                            else -> stringResource(R.string.home_bill_age, bill.outstandingRupiah.toRupiah(), bill.ageDays)
                        },
                        style = MankeuType.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (bills.size > 3) {
                Text(
                    stringResource(R.string.home_bills_more, bills.size - 3),
                    style = MankeuType.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onReceive, contentPadding = PaddingValues(horizontal = 0.dp)) {
            Text(stringResource(R.string.home_bills_receive), style = MankeuType.Body, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WarningRow(text: String, onClick: () -> Unit) {
    val semantic = LocalMankeuSemanticColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = Space.Row).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.Field),
    ) {
        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = semantic.warning)
        Text(text, style = MankeuType.Body, color = semantic.warning, modifier = Modifier.weight(1f))
    }
}

/** No records yet: three steps, each a button, ticked once done (plan ruling 4). */
@Composable
private fun FirstSteps(hasProduct: Boolean, onNewProduct: () -> Unit, onNewSale: () -> Unit, onSeeProfit: () -> Unit) {
    MankeuCard {
        Text(stringResource(R.string.home_steps_title), style = MankeuType.Title)
        Column {
            StepRow(1, stringResource(R.string.home_step_product), done = hasProduct, onClick = onNewProduct)
            HorizontalDivider()
            StepRow(2, stringResource(R.string.home_step_sale), done = false, onClick = onNewSale)
            HorizontalDivider()
            StepRow(3, stringResource(R.string.home_step_profit), done = false, onClick = onSeeProfit)
        }
    }
}

@Composable
private fun StepRow(number: Int, text: String, done: Boolean, onClick: (() -> Unit)?) {
    val semantic = LocalMankeuSemanticColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Space.Row)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.Field),
    ) {
        Box(
            Modifier.size(32.dp).background(if (done) semantic.success else MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.home_step_done), tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(18.dp))
            } else {
                Text(number.toString(), style = MankeuType.Body, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Text(
            text,
            style = MankeuType.Body,
            color = if (onClick != null && !done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (onClick != null && !done) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
    }
}
