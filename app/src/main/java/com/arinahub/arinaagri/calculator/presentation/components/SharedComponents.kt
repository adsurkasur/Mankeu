package com.arinahub.arinaagri.calculator.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arinahub.arinaagri.calculator.presentation.isDarkScheme
import com.arinahub.arinaagri.calculator.R
import com.arinahub.arinaagri.calculator.domain.FinancialEventType
import com.arinahub.arinaagri.calculator.domain.MoneyEvent
import com.arinahub.arinaagri.calculator.domain.RecordStatus
import com.arinahub.arinaagri.calculator.presentation.LocalArinaSemanticColors
import com.arinahub.arinaagri.calculator.presentation.toDisplayDate
import com.arinahub.arinaagri.calculator.presentation.toRupiah

/**
 * Cross-screen building blocks shared by Home, Reports, and More. Kept separate from the
 * per-destination files so a change to how an "empty state" or "notice" looks does not force a
 * diff across every screen file.
 */

@Composable
fun NoticeCard(title: String, body: String, icon: ImageVector, onClick: (() -> Unit)? = null) {
    val semantic = LocalArinaSemanticColors.current
    OutlinedCard(
        modifier = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth(),
        // Pinned explicitly to `surface` (white) rather than trusting OutlinedCard's default: the
        // same class of bug as EmptyCard below — an unpinned card tone reading visibly grayer than
        // its white neighbours ("kontras ga rata" between adjacent cards on Beranda/Pengaturan).
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = RoundedCornerShape(16.dp), color = semantic.warningContainer) {
                Icon(icon, contentDescription = null, tint = semantic.onWarningContainer, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Black)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun EmptyCard(title: String, body: String, action: String? = null, onAction: (() -> Unit)? = null) {
    // Root cause of the owner-reported "Belum ada catatan" card reading visibly grayer than the
    // white metric tiles right above it: a bare `Card()` here left `containerColor` unset, and
    // Material3's own default for that overload is NOT the same `surface` token every other card
    // in this app pins explicitly — it drifts one step darker (`surfaceContainerLow`/tonal
    // elevation blend). Pinning it here matches SimpleMetricCard/EventCard/ReportCard exactly, so
    // every card on a given screen reads as the same white-on-cream two-tone instead of an
    // inconsistent, "jomplang" mix.
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(28.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null && onAction != null) Button(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EventCard(
    event: MoneyEvent,
    onReverse: (String, String) -> Unit,
    onDelete: ((String) -> Unit)? = null,
) {
    val semantic = LocalArinaSemanticColors.current
    var confirmReverse by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val incoming = event.type in setOf(
        FinancialEventType.CASH_INCOME,
        FinancialEventType.CREDIT_SALE,
        FinancialEventType.RECEIVABLE_COLLECTION,
        FinancialEventType.OWNER_CAPITAL,
        FinancialEventType.LOAN_RECEIVED,
    )
    val active = event.status == RecordStatus.POSTED && event.reversedByEventId == null && event.type != FinancialEventType.REVERSAL
    val tone = if (incoming) semantic.success to semantic.successContainer else semantic.danger to semantic.dangerContainer

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = tone.second) {
                Icon(
                    if (incoming) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(20.dp),
                    tint = tone.first,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.description, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${event.category} • ${event.date.toDisplayDate()}${event.counterparty?.let { " • $it" }.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                if (event.reversedByEventId != null || event.type == FinancialEventType.REVERSAL) {
                    Text(
                        stringResource(R.string.event_reversed_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (incoming) "+" else "−") + event.amountRupiah.toRupiah(),
                    fontWeight = FontWeight.Black,
                    color = tone.first,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (active) {
                        IconButton(
                            onClick = { confirmReverse = true },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Redo,
                                contentDescription = stringResource(R.string.action_reverse_event),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = { confirmDelete = true },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = stringResource(R.string.action_delete_event),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }

    val reversalReason = stringResource(R.string.event_reverse_reason, event.description)
    if (confirmReverse) {
        ArinaSheet(
            title = stringResource(R.string.dialog_reverse_event_title),
            onDismiss = { confirmReverse = false },
            confirmLabel = stringResource(R.string.action_reverse_event),
            destructive = false,
            onConfirm = {
                onReverse(event.id, reversalReason)
                confirmReverse = false
            },
        ) {
            Text(stringResource(R.string.dialog_reverse_event_body))
            Spacer(Modifier.height(4.dp))
            Text(
                "${event.description} • ${event.amountRupiah.toRupiah()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    if (confirmDelete && onDelete != null) {
        ArinaSheet(
            title = stringResource(R.string.dialog_delete_event_title),
            onDismiss = { confirmDelete = false },
            confirmLabel = stringResource(R.string.action_delete_event),
            destructive = true,
            onConfirm = {
                onDelete(event.id)
                confirmDelete = false
            },
        ) {
            Text(stringResource(R.string.dialog_delete_event_body))
            Spacer(Modifier.height(4.dp))
            Text(
                "${event.description} • ${event.amountRupiah.toRupiah()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Tone selects which container color a [SimpleMetricCard] uses; kept intentionally small
 *  (no bespoke enum per screen) so Budget/Reports/Home all read from the same three tones. */
enum class MetricTone { PRIMARY, SUCCESS, DANGER }

data class SimpleMetric(val label: String, val amount: Long, val icon: ImageVector, val tone: MetricTone = MetricTone.PRIMARY)

@Composable
fun SimpleMetricGrid(metrics: List<SimpleMetric>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { metric -> SimpleMetricCard(metric, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SimpleMetricCard(metric: SimpleMetric, modifier: Modifier = Modifier) {
    val semantic = LocalArinaSemanticColors.current
    val (container, onContainer, iconTint) = when (metric.tone) {
        MetricTone.PRIMARY -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primary)
        MetricTone.SUCCESS -> Triple(semantic.successContainer, semantic.onSuccessContainer, semantic.success)
        MetricTone.DANGER -> Triple(semantic.dangerContainer, semantic.onDangerContainer, semantic.danger)
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = container) {
                Icon(metric.icon, contentDescription = null, tint = iconTint, modifier = Modifier.padding(8.dp).size(20.dp))
            }
            Text(metric.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Text(metric.amount.toRupiah(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = onContainer, maxLines = 1)
        }
    }
}

@Composable
fun ReportCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            content()
        }
    }
}

@Composable
fun ReportRow(
    label: String,
    amount: Long,
    emphasized: Boolean = false,
    asMoney: Boolean = true,
    highlightNegative: Boolean = true,
) {
    val semantic = LocalArinaSemanticColors.current
    val isNegative = highlightNegative && amount < 0
    val valueColor = when {
        isNegative -> semantic.danger
        emphasized -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    // The label wraps, the amount never does: "Kebutuhan Modal Kerja (Defisit Puncak)" ate the row
    // and broke "Rp1.150.000" across three lines, which is unreadable as a number.
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontWeight = if (emphasized) FontWeight.Black else FontWeight.Normal,
        )
        Text(
            if (asMoney) amount.toRupiah() else amount.toString(),
            fontWeight = if (emphasized || isNegative) FontWeight.Black else FontWeight.Normal,
            color = valueColor,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
fun ComparisonRow(
    label: String,
    projection: Long,
    realization: Long,
    // Colour follows the meaning of the row, not the sign of the number: spending Rp1.150.000 more
    // than the plan was rendered in success green because the difference was positive.
    higherIsBetter: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.compare_plan, projection.toRupiah()), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.compare_actual, realization.toRupiah()), style = MaterialTheme.typography.bodySmall)
        }
        val semantic = LocalArinaSemanticColors.current
        val delta = realization - projection
        Text(
            stringResource(R.string.compare_delta, delta.toRupiah()),
            color = if ((delta >= 0) == higherIsBetter) semantic.success else semantic.danger,
            fontWeight = FontWeight.Black,
        )
    }
}

/**
 * An adaptive brand logo badge for Arina Agri Calculator.
 *
 * In Light mode: renders a crisp forest green mark on a soft mint container (green on light).
 * In Dark mode: renders a luminous emerald green mark on a dark container (green on dark).
 */
@Composable
fun ArinaAppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    badgeShape: Shape = CircleShape,
    showContainer: Boolean = true,
) {
    val dark = isDarkScheme()
    val containerColor = if (dark) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    }
    val logoTint = MaterialTheme.colorScheme.primary

    if (showContainer) {
        Box(
            modifier = modifier
                .size(size)
                .background(containerColor, badgeShape)
                .padding(size * 0.14f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mankeu_logo),
                contentDescription = stringResource(R.string.app_name),
                tint = logoTint,
                modifier = Modifier.fillMaxSize(),
            )
        }
    } else {
        Icon(
            painter = painterResource(R.drawable.ic_mankeu_logo),
            contentDescription = stringResource(R.string.app_name),
            tint = logoTint,
            modifier = modifier.size(size),
        )
    }
}
