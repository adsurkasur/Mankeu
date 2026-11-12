package com.mankeu.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.domain.MoneyEvent
import com.mankeu.domain.SaleStatus
import com.mankeu.presentation.AppUiState
import com.mankeu.presentation.CatatanFilter
import com.mankeu.presentation.FeedbackEvent
import com.mankeu.presentation.LedgerDayGroup
import com.mankeu.presentation.LocalFeedback
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.UnifiedLedger
import com.mankeu.presentation.UnifiedLedgerItem
import com.mankeu.presentation.components.EmptyCard
import com.mankeu.presentation.components.NoticeCard
import com.mankeu.presentation.components.SectionTitle
import com.mankeu.presentation.components.dateWords
import com.mankeu.presentation.guide.HelpArticle
import com.mankeu.presentation.toDisplayDate
import com.mankeu.presentation.toRupiah
import java.time.LocalDate

/**
 * Tab Catatan (U6): Satu daftar gabungan menurut tanggal yang memuat:
 * - Nota penjualan (status Lunas/Sebagian/Belum dibayar & sisa)
 * - Pembayaran nota ("Bayar nota ...")
 * - Belanja, uang masuk lain, uang keluar lain, dan modal/ambil pribadi
 * - Filter chip terintegrasi: Semua / Belum lunas / Uang masuk / Uang keluar
 * - Daftar polos ber-pemisah dengan judul hari dan total kecil harian
 */
@Composable
fun EntryScreen(
    state: AppUiState,
    onEdit: (MoneyEvent) -> Unit,
    onDelete: (String) -> Unit,
    onManageScope: () -> Unit,
    onNew: (MoneyFormMode) -> Unit = {},
    onOpenSale: (String) -> Unit = {},
    filter: CatatanFilter = CatatanFilter.ALL,
    onFilter: (CatatanFilter) -> Unit = {},
    noteFilter: NoteFilter? = null,
    onNoteFilter: ((NoteFilter) -> Unit)? = null,
    onLoadEarlier: () -> Unit = {},
) {
    var search by rememberSaveable { mutableStateOf("") }
    val feedback = LocalFeedback.current
    val today = remember { LocalDate.now() }

    val activeFilter = if (noteFilter == NoteFilter.OVERDUE) CatatanFilter.OVERDUE else filter

    val dayGroups = remember(state.sales, state.events, state.products, activeFilter, search, today, state.ledgerOldestDate, state.ledgerSupplierPaidRupiah) {
        UnifiedLedger.build(
            sales = state.sales,
            events = state.events,
            products = state.products,
            filter = activeFilter,
            search = search,
            today = today,
            oldestDate = state.ledgerOldestDate,
            supplierPaymentsRupiah = state.ledgerSupplierPaidRupiah,
        )
    }

    val hasEntries = UnifiedLedger.hasAnyEntries(state.sales, state.events)
    val totalCount = state.sales.size + state.eventCount

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "entry_title") {
            SectionTitle(
                title = stringResource(R.string.entry_title),
                subtitle = stringResource(R.string.ledger_subtitle, totalCount),
            )
        }

        if (state.selectedProjectId == null || state.projects.isEmpty()) {
            item(key = "entry_no_project") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.entry_no_project_banner_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                        Text(
                            stringResource(R.string.entry_no_project_banner_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Button(
                            onClick = onManageScope,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) {
                            Text(stringResource(R.string.entry_action_add_project), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (hasEntries) {
            item(key = "entry_search") {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text(stringResource(R.string.home_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (search.isNotBlank()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_close))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item(key = "entry_filter_chips") {
                val filters = listOf(
                    CatatanFilter.ALL to stringResource(R.string.notes_filter_all),
                    CatatanFilter.UNPAID to stringResource(R.string.notes_filter_open),
                    CatatanFilter.OVERDUE to stringResource(R.string.notes_filter_overdue),
                    CatatanFilter.INCOME to stringResource(R.string.reports_money_in),
                    CatatanFilter.EXPENSE to stringResource(R.string.reports_money_out),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters, key = { it.first.name }) { (catFilter, label) ->
                        FilterChip(
                            selected = activeFilter == catFilter,
                            onClick = {
                                feedback(FeedbackEvent.TAP)
                                onFilter(catFilter)
                                onNoteFilter?.invoke(
                                    when (catFilter) {
                                        CatatanFilter.UNPAID -> NoteFilter.OPEN
                                        CatatanFilter.OVERDUE -> NoteFilter.OVERDUE
                                        else -> NoteFilter.ALL
                                    }
                                )
                            },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        when {
            !hasEntries -> item(key = "entry_empty") {
                EmptyCard(
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.home_empty_body),
                    action = stringResource(R.string.home_empty_action),
                    learn = HelpArticle.RECORD_INCOME,
                    onAction = { onNew(MoneyFormMode.OTHER_IN) },
                )
            }

            dayGroups.isEmpty() -> item(key = "entry_no_match") {
                EmptyCard(
                    title = stringResource(R.string.home_no_match_title),
                    body = stringResource(R.string.home_no_match_body),
                )
            }

            else -> {
                dayGroups.forEach { group ->
                    item(key = "day_header_${group.date}") {
                        LedgerDayHeader(group = group)
                    }

                    group.items.forEachIndexed { index, item ->
                        item(key = item.id) {
                            Column {
                                LedgerRowItem(
                                    item = item,
                                    today = today,
                                    onOpenSale = onOpenSale,
                                    onEdit = onEdit,
                                )
                                if (index < group.items.size - 1) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }
        // Outside the match branches: an empty search/filter must never strand older records.
        if (state.ledgerHasEarlier) {
            item(key = "entry_load_earlier") {
                Button(onClick = onLoadEarlier, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.ledger_load_earlier))
                }
            }
        }
    }
}

@Composable
private fun LedgerDayHeader(group: LedgerDayGroup) {
    val semantic = LocalMankeuSemanticColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = dateWords(group.date),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (group.totalInRupiah > 0) {
                Text(
                    text = "+" + group.totalInRupiah.toRupiah(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = semantic.success,
                )
            }
            if (group.totalInRupiah > 0 && group.totalOutRupiah > 0) {
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (group.totalOutRupiah > 0) {
                Text(
                    text = "−" + group.totalOutRupiah.toRupiah(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = semantic.danger,
                )
            }
        }
    }
}

@Composable
private fun LedgerRowItem(
    item: UnifiedLedgerItem,
    today: LocalDate,
    onOpenSale: (String) -> Unit,
    onEdit: (MoneyEvent) -> Unit,
) {
    val semantic = LocalMankeuSemanticColors.current
    val feedback = LocalFeedback.current

    val onClick = {
        feedback(FeedbackEvent.TAP)
        when (item) {
            is UnifiedLedgerItem.Sale -> onOpenSale(item.record.sale.id)
            is UnifiedLedgerItem.SalePayment -> {
                if (item.saleRecord != null) {
                    onOpenSale(item.saleRecord.sale.id)
                } else {
                    onEdit(item.event)
                }
            }
            is UnifiedLedgerItem.Money -> onEdit(item.event)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            when (item) {
                is UnifiedLedgerItem.Sale -> {
                    Text(
                        text = item.customerName ?: stringResource(R.string.sale_customer_none),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val details = listOfNotNull(
                        item.productSummary.ifBlank { stringResource(R.string.fab_sale_note) },
                        item.record.dueDate?.takeIf { item.outstandingRupiah > 0 }?.let {
                            stringResource(R.string.sale_due_on, it.toDisplayDate())
                        },
                    ).joinToString(" · ")
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                is UnifiedLedgerItem.SalePayment -> {
                    Text(
                        text = stringResource(R.string.sale_pay_title) + " " + item.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.event.category.ifBlank { stringResource(R.string.sale_payments) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                is UnifiedLedgerItem.Money -> {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val subtitle = listOfNotNull(
                        item.category,
                        item.event.reference?.takeIf { it.isNotBlank() }?.let { "Ref: $it" },
                    ).joinToString(" · ")
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            when (item) {
                is UnifiedLedgerItem.Sale -> {
                    Text(
                        text = item.totalRupiah.toRupiah(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    SaleStatusBadge(item.record, today)
                    if (item.status == SaleStatus.PARTIAL) {
                        Text(
                            text = stringResource(R.string.sale_left, item.outstandingRupiah.toRupiah()),
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.warning,
                        )
                    }
                }
                is UnifiedLedgerItem.SalePayment -> {
                    Text(
                        text = "+" + item.amountRupiah.toRupiah(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = semantic.success,
                    )
                }
                is UnifiedLedgerItem.Money -> {
                    val (sign, color) = when {
                        item.isIncoming -> "+" to semantic.success
                        item.isCreditPurchase -> "" to MaterialTheme.colorScheme.onSurface
                        else -> "−" to semantic.danger
                    }
                    Text(
                        text = sign + item.amountRupiah.toRupiah(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                    if (item.isCreditPurchase) {
                        Text(
                            text = stringResource(R.string.notes_filter_open),
                            style = MaterialTheme.typography.labelSmall,
                            color = semantic.warning,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
