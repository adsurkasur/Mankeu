package com.mankeu.presentation.screens

import com.mankeu.presentation.guide.HelpArticle
import com.mankeu.presentation.components.rememberTourListState
import com.mankeu.presentation.components.MankeuTooltip

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import com.mankeu.domain.FinancialSummary
import com.mankeu.domain.MonthlyCashFlow
import com.mankeu.domain.MonthlyCashFlowCalculator
import com.mankeu.domain.TransactionCalculation
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.domain.BookMode
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.presentation.AppUiState
import com.mankeu.presentation.Destination
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.isDarkScheme
import com.mankeu.presentation.components.EmptyCard
import com.mankeu.presentation.components.ActualMonthCard
import com.mankeu.presentation.components.PlanFeasibilityCard
import com.mankeu.presentation.components.MetricTone
import com.mankeu.presentation.components.NoticeCard
import com.mankeu.presentation.components.SectionTitle
import com.mankeu.presentation.components.SimpleMetric
import com.mankeu.presentation.components.SimpleMetricGrid
import com.mankeu.presentation.components.SkeletonHero
import com.mankeu.presentation.components.SkeletonListItem
import com.mankeu.presentation.components.SkeletonMetricGrid
import com.mankeu.presentation.components.TourTarget
import com.mankeu.presentation.components.tourTarget
import com.mankeu.presentation.LocalMotion
import com.mankeu.presentation.appLocale
import com.mankeu.presentation.toRupiah
import java.time.format.DateTimeFormatter

/**
 * The Beranda (home) destination: cash headline, the four key metrics, expense
 * distribution, stock, plan feasibility and this month's break-even status.
 */
@Composable
fun HomeScreen(
    state: AppUiState,
    onNavigate: (Destination) -> Unit,
    onOpenData: () -> Unit,
    onOpenPlan: () -> Unit = {},
    expanded: Boolean = false,
) {
    // Proyeksi reads the business plan (DEC-014) whenever the unit has one; postings otherwise.
    // Both produce the same summary and schedule shapes, so every card below reads one code path.
    val plan = state.plan.takeIf { state.selectedBookMode == BookMode.PROJECTION }
    val summary = plan?.summary ?: state.summary
    val monthlyCashFlow = remember(state.cashFlowRecords, plan) {
        plan?.schedule ?: MonthlyCashFlowCalculator.calculateFromRecords(state.cashFlowRecords)
    }

    LazyColumn(
        state = rememberTourListState(),
        contentPadding = PaddingValues(
            start = if (expanded) 24.dp else 16.dp,
            end = if (expanded) 24.dp else 16.dp,
            top = 12.dp,
            // Clears the speed dial. Scaffold pads around a FAB for its own content slot, but the
            // list scrolls under it, so without this the last card is permanently half-covered.
            bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.isLoading) {
            item { SkeletonHero() }
            item { SkeletonMetricGrid() }
            items(3) { SkeletonListItem() }
            return@LazyColumn
        }
        // No business or field yet: the app used to quietly create "Usaha Saya"/"Umum" during
        // startup, so a first-time owner met a dashboard for a farm they had never named. Now the
        // first screen states plainly what is missing and offers the one action that fixes it.
        if (state.businesses.isEmpty()) {
            item {
                EmptyCard(
                    stringResource(R.string.setup_business_title),
                    stringResource(R.string.setup_business_body),
                    stringResource(R.string.scope_add_business),
                    onOpenData,
                    learn = HelpArticle.START_SCOPE,
                )
            }
            return@LazyColumn
        }
        if (state.projects.isEmpty()) {
            item {
                EmptyCard(
                    stringResource(R.string.setup_project_title),
                    stringResource(R.string.setup_project_body),
                    stringResource(R.string.scope_add_project),
                    onOpenData,
                    learn = HelpArticle.START_SCOPE,
                )
            }
            return@LazyColumn
        }
        item {
            HeroCard(
                mode = state.selectedBookMode,
                amount = summary.cashBalanceRupiah,
                latestMonth = monthlyCashFlow.lastOrNull(),
                projectId = state.selectedProjectId,
                modifier = Modifier.tourTarget(TourTarget.HERO_SUMMARY),
                onClick = { onNavigate(Destination.REPORTS) },
            )
        }
        item {
            Column(
                modifier = Modifier.animateContentSize(tween(maxOf(LocalMotion.current.mediumMillis, 1), easing = FastOutSlowInEasing)),
            ) {
                val profitTone = when {
                    summary.profitRupiah < 0 -> MetricTone.DANGER
                    summary.profitRupiah > 0 -> MetricTone.SUCCESS
                    else -> MetricTone.PRIMARY
                }
                val cashFlowTone = when {
                    summary.operatingCashFlowRupiah < 0 -> MetricTone.DANGER
                    summary.operatingCashFlowRupiah > 0 -> MetricTone.SUCCESS
                    else -> MetricTone.PRIMARY
                }
                // Operating cash flow leads: it answers "is the business itself paying for
                // itself", which is the question a loss-making month topped up with owner capital
                // hides from the balance in the hero card. Profit follows, then operating income and expense.
                SimpleMetricGrid(
                    listOf(
                        SimpleMetric(stringResource(R.string.home_cash_flow), summary.operatingCashFlowRupiah, Icons.Default.SwapHoriz, cashFlowTone),
                        SimpleMetric(stringResource(R.string.home_profit), summary.profitRupiah, Icons.Default.Assessment, profitTone),
                        SimpleMetric(stringResource(R.string.reports_operating_income), summary.operatingIncomeRupiah, Icons.Default.ArrowDownward, MetricTone.SUCCESS),
                        SimpleMetric(stringResource(R.string.reports_operating_expense), summary.operatingExpenseRupiah, Icons.Default.ArrowUpward, MetricTone.DANGER),
                    ),
                )
            }
        }
        item {
            ExpenseDistributionCard(
                state = state,
            )
        }
        item {
            StockCard(items = state.stockItems, onManage = { onNavigate(Destination.STOCK) })
        }
        // Feasibility is the reason the plan exists, so it gets its own card with a meter rather
        // than more figures in the grid. Realisasi asks the same question of this month.
        if (state.selectedBookMode == BookMode.PROJECTION) {
            item {
                if (plan != null) {
                    PlanFeasibilityCard(plan, onOpenPlan = onOpenPlan)
                } else {
                    EmptyCard(
                        stringResource(R.string.plan_empty_title),
                        stringResource(R.string.plan_empty_body),
                        stringResource(R.string.plan_open),
                        onOpenPlan,
                        learn = HelpArticle.PLAN_CREATE,
                    )
                }
            }
        } else {
            state.actualMonth?.let { actual -> item { ActualMonthCard(actual, state.products, Modifier.tourTarget(TourTarget.HOME_BEP)) } }
        }
        if (state.notices.isNotEmpty()) {
            item {
                SectionTitle(
                    stringResource(R.string.home_import_notice_title),
                    stringResource(R.string.home_import_notice_sub),
                )
            }
            items(state.notices.take(3), key = { it.id }) { notice ->
                NoticeCard(notice.title, notice.explanation, Icons.Default.Info)
            }
        }
    }
}

/**
 * Cash headline. Switching Proyeksi <-> Realisasi is not a different screen — it is the same
 * question asked of a different book — so the card stays put and only its wording and figure are
 * exchanged, sliding in the direction of travel. Previously the label and the number were swapped
 * in a single frame, which read as a glitch rather than as a deliberate change of scope.
 */
@Composable
private fun HeroCard(
    mode: BookMode,
    amount: Long,
    latestMonth: MonthlyCashFlow?,
    modifier: Modifier = Modifier,
    projectId: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val motion = LocalMotion.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier.fillMaxWidth(),
    ) {
        // The hero is the one place the app fills a large area with an accent colour, so its text
        // colour has to follow the fill. In light mode the deep primary/tertiary pair carries white
        // text at 6.4:1; in dark mode those roles are *light* greens and blues, where white text
        // would sit at about 2:1, so dark mode fills with the container tones and writes on them
        // with onPrimaryContainer (7.2:1 at both ends of the gradient).
        val dark = isDarkScheme()
        val semantic = LocalMankeuSemanticColors.current
        val isNegative = amount < 0
        val targetHeroStart = when {
            isNegative && dark -> semantic.dangerContainer
            isNegative -> semantic.danger
            dark -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.primary
        }
        val targetHeroEnd = when {
            isNegative && dark -> Color(0xFF5D1805)
            isNegative -> Color(0xFF7C2E19)
            dark -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.tertiary
        }
        val targetOnHero = when {
            isNegative && dark -> semantic.onDangerContainer
            isNegative -> semantic.onDanger
            dark -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onPrimary
        }

        val colorAnimationSpec = tween<Color>(
            durationMillis = maxOf(motion.mediumMillis, 1),
            easing = FastOutSlowInEasing,
        )
        val heroStart by animateColorAsState(targetHeroStart, colorAnimationSpec, label = "heroStart")
        val heroEnd by animateColorAsState(targetHeroEnd, colorAnimationSpec, label = "heroEnd")
        val onHero by animateColorAsState(targetOnHero, colorAnimationSpec, label = "onHero")
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(heroStart, heroEnd)),
                    MaterialTheme.shapes.extraLarge,
                )
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            role = androidx.compose.ui.semantics.Role.Button,
                            onClickLabel = stringResource(R.string.reports_cash_flow),
                            onClick = onClick,
                        )
                    } else Modifier
                )
                .padding(20.dp)
                .animateContentSize(tween(maxOf(motion.mediumMillis, 1), easing = FastOutSlowInEasing)),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedContent(
                targetState = Pair(mode, projectId ?: ""),
                transitionSpec = {
                    val enterMillis = maxOf(motion.mediumMillis, 1)
                    val exitMillis = maxOf(motion.fastMillis, 1)
                    val downwards = targetState.first == BookMode.REALIZATION
                    (slideInVertically(tween(enterMillis, easing = FastOutSlowInEasing)) { full ->
                        if (downwards) full / 3 else -full / 3
                    } + fadeIn(tween(enterMillis)))
                        .togetherWith(
                            slideOutVertically(tween(exitMillis, easing = FastOutSlowInEasing)) { full ->
                                if (downwards) -full / 3 else full / 3
                            } + fadeOut(tween(exitMillis)),
                        )
                },
                label = "heroBalance",
            ) { (currentMode, _) ->
                val projection = currentMode == BookMode.PROJECTION
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(if (projection) R.string.home_balance_plan else R.string.home_balance_actual),
                        color = onHero.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        amount.toRupiah(),
                        color = onHero,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        stringResource(if (projection) R.string.home_balance_sub_plan else R.string.home_balance_sub_actual),
                        color = onHero.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    // A balance on its own does not say which way the business is moving: a
                    // healthy-looking figure can be last month's capital top-up sitting still. The trend
                    // line names the direction of the most recent month in the same breath.
                    val locale = appLocale()
                    val trend = when {
                        latestMonth == null || latestMonth.netRupiah == 0L -> stringResource(R.string.home_trend_flat)
                        latestMonth.netRupiah > 0 -> stringResource(
                            R.string.home_trend_surplus,
                            latestMonth.netRupiah.toRupiah(),
                            latestMonth.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                        )
                        else -> stringResource(
                            R.string.home_trend_deficit,
                            (-latestMonth.netRupiah).toRupiah(),
                            latestMonth.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MankeuTooltip(stringResource(if (projection) R.string.tooltip_mode_plan else R.string.tooltip_mode_actual)) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = onHero.copy(alpha = 0.16f),
                            ) {
                                Text(
                                    stringResource(
                                        if (projection) R.string.home_mode_badge_plan else R.string.home_mode_badge_actual,
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = onHero,
                                    maxLines = 1,
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        // TrendingUp/Down, not the money-direction arrows used elsewhere: beside
                        // the word "Surplus" a downward arrow reads as a decline, which is the
                        // opposite of what the figure says.
                        Icon(
                            if ((latestMonth?.netRupiah ?: 0L) < 0) {
                                Icons.AutoMirrored.Filled.TrendingDown
                            } else {
                                Icons.AutoMirrored.Filled.TrendingUp
                            },
                            contentDescription = null,
                            tint = onHero.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            trend,
                            color = onHero.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                        )
                    }
                    if (onClick != null) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "Rincian Arus Kas \u2192",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = onHero.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        }
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
    val expenseCategories = remember(state.categoryTotals, state.budgetItems, state.selectedBookMode, otherLabel, palette) {
        val categoryExpenses = mutableMapOf<String, Long>()
        // Summed by the database (`observeCategoryTotals`). Doing it here meant holding every event in
        // memory to add up a dozen slices, and it silently went wrong the moment the event list
        // was capped for display.
        state.categoryTotals.forEach { row ->
            val cat = row.category.ifBlank { otherLabel }
            categoryExpenses[cat] = (categoryExpenses[cat] ?: 0L) + row.totalRupiah
        }
        if (categoryExpenses.isEmpty() && state.selectedBookMode == BookMode.PROJECTION) {
            val catMap = state.categories.associateBy { it.id }
            state.budgetItems.forEach { item ->
                val role = catMap[item.categoryId]?.role
                if (role != CategoryRole.REVENUE.name) {
                    val catName = catMap[item.categoryId]?.name ?: otherLabel
                    categoryExpenses[catName] = (categoryExpenses[catName] ?: 0L) + item.plannedTotalRupiah
                }
            }
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

    if (expenseCategories.isNotEmpty()) {
        val totalExpense = expenseCategories.sumOf { it.amount }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.large,
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(tween(maxOf(motion.mediumMillis, 1), easing = FastOutSlowInEasing))
                .tourTarget(TourTarget.HOME_COST_CHART),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
                                    "${String.format(java.util.Locale.US, "%.1f", slice.percentage)}%",
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

