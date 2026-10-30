package com.mankeu.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.data.ContactDraft
import com.mankeu.data.ContactEntity
import com.mankeu.data.PayableRecord
import com.mankeu.data.ProductRecord
import com.mankeu.data.SaleDraft
import com.mankeu.data.SaleLineDraft
import com.mankeu.data.SaleRecord
import com.mankeu.domain.ProductCosting
import com.mankeu.domain.SaleStatus
import com.mankeu.domain.SalesMath
import com.mankeu.domain.StockItem
import com.mankeu.domain.TransactionCalculation
import com.mankeu.presentation.ChoiceMenu
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.components.DateField
import com.mankeu.presentation.components.MankeuSheet
import com.mankeu.presentation.components.MoneyField
import com.mankeu.presentation.components.ReportRow
import com.mankeu.presentation.components.sanitizeDecimal
import com.mankeu.presentation.toDisplayDate
import com.mankeu.presentation.toDisplayQuantity
import com.mankeu.presentation.toRupiah
import java.time.LocalDate

/** The note list filters in Catatan; Beranda's Tagihan card opens the list on one of them. */
enum class NoteFilter { ALL, OPEN, OVERDUE }

/** One product line while typed; text until saved, so "0", blank or "1," never reach the math. */
private data class SaleLineInput(val productId: String?, val unit: String = "", val quantity: String = "", val price: String = "")

private val saleLineSaver = listSaver<SnapshotStateList<SaleLineInput>, String>(
    save = { rows -> rows.flatMap { listOf(it.productId.orEmpty(), it.unit, it.quantity, it.price) } },
    restore = { flat -> flat.chunked(4).map { SaleLineInput(it[0].ifEmpty { null }, it[1], it[2], it[3]) }.toMutableStateList() },
)

private enum class PaidChoice { FULL, NONE, PART }

/** A product's base unit and its sale units, the names a note line may use (repository rule). */
private fun ProductRecord.unitNames() = (units.sortedBy { it.sortOrder }.map { it.name } + product.unit).distinctBy { it.lowercase() }

private fun ProductRecord.priceOf(unit: String) = units.firstOrNull { it.name.equals(unit, ignoreCase = true) }?.priceRupiah

/**
 * The sales note (spec #17, F2-3, F2-7): a full page with Simpan fixed at the bottom (#13). Line
 * totals, HPP and profit preview while typing with the same functions the repository stores with;
 * every rule is checked by `FinanceRepository.saveSale`, whose message the shell shows while this
 * page stays open.
 */
@Composable
fun SaleNoteEditorScreen(
    existing: SaleRecord?,
    products: List<ProductRecord>,
    materials: List<StockItem>,
    contacts: List<ContactEntity>,
    sales: List<SaleRecord>,
    payables: List<PayableRecord>,
    onSave: (SaleDraft) -> Unit,
    onSaveContact: (String?, ContactDraft, (String) -> Unit) -> Unit,
    onArchiveContact: (String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var contactId by rememberSaveable { mutableStateOf(existing?.sale?.contactId) }
    var date by rememberSaveable { mutableStateOf((existing?.sale?.dateEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now()).toString()) }
    var due by rememberSaveable { mutableStateOf(existing?.dueDate?.toString().orEmpty()) }
    var discount by rememberSaveable { mutableStateOf(existing?.sale?.discountRupiah?.takeIf { it > 0 }?.toString().orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(existing?.sale?.notes.orEmpty()) }
    var paidChoice by rememberSaveable { mutableStateOf(PaidChoice.FULL) }
    var partAmount by rememberSaveable { mutableStateOf("") }
    var picking by rememberSaveable { mutableStateOf(false) }
    val lines = rememberSaveable(saver = saleLineSaver) {
        (existing?.lines.orEmpty().sortedBy { it.sortOrder }.map { SaleLineInput(it.productId, it.unitName, it.quantityText.replace('.', ','), it.unitPriceRupiah.toString()) }
            .ifEmpty { listOf(SaleLineInput(null)) }).toMutableStateList()
    }

    val semantic = LocalMankeuSemanticColors.current
    val prices = materials.associate { it.id to it.basePriceRupiah }
    val costs = products.associate { it.product.id to ProductCosting.cost(it.spec, prices) }
    // Per line: (total, hpp) once product, quantity and price are valid; null shows no preview.
    val results = lines.map { line ->
        val record = products.firstOrNull { it.product.id == line.productId }
        val quantity = TransactionCalculation.parseQuantity(line.quantity)
        val price = line.price.toLongOrNull()?.takeIf { it > 0 }
        if (record == null || quantity == null || price == null) null
        else SalesMath.lineTotal(quantity, price) to ProductCosting.hpp(costs.getValue(record.product.id).batchCostRupiah, record.spec.batchYield, record.contentOf(line.unit) * quantity)
    }
    val subtotal = results.sumOf { it?.first ?: 0 }
    val hpp = results.sumOf { it?.second ?: 0 }
    val discountRupiah = discount.toLongOrNull() ?: 0
    val total = subtotal - discountRupiah
    val showDue = if (existing == null) paidChoice != PaidChoice.FULL else existing.outstanding > 0 || due.isNotBlank()

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CustomerField(
                            name = contacts.firstOrNull { it.id == contactId }?.name ?: existing?.contact?.takeIf { it.id == contactId }?.name,
                            onOpen = { picking = true },
                            onClear = { contactId = null },
                        )
                        DateField(value = date, label = stringResource(R.string.sale_date), onValueChange = { date = it })
                        if (showDue) {
                            DateField(value = due, label = stringResource(R.string.sale_due), clearable = true, onValueChange = { due = it })
                        }
                    }
                }
                item { Text(stringResource(R.string.sale_products), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) }
                if (products.isEmpty()) {
                    item { Text(stringResource(R.string.sale_no_products), color = semantic.danger) }
                }
                itemsIndexed(lines) { index, line ->
                    val record = products.firstOrNull { it.product.id == line.productId }
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ChoiceMenu(
                                    fieldLabel = stringResource(R.string.sale_product),
                                    label = record?.product?.name ?: stringResource(R.string.sale_product_pick),
                                    options = products.map { it.product.id to it.product.name },
                                    onSelected = { id ->
                                        val picked = products.first { it.product.id == id }
                                        // The main sale unit and its price (DEC-028 §8); the base unit when it has none.
                                        val unit = picked.mainUnit?.name ?: picked.product.unit
                                        lines[index] = line.copy(productId = id, unit = unit, price = picked.priceOf(unit)?.toString().orEmpty())
                                    },
                                )
                                if (record != null) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ChoiceMenu(
                                            modifier = Modifier.weight(1f),
                                            fieldLabel = stringResource(R.string.sale_unit),
                                            label = line.unit,
                                            options = record.unitNames().map { it to it },
                                            onSelected = { unit -> lines[index] = line.copy(unit = unit, price = record.priceOf(unit)?.toString() ?: line.price) },
                                        )
                                        OutlinedTextField(
                                            value = line.quantity,
                                            onValueChange = { lines[index] = line.copy(quantity = sanitizeDecimal(it)) },
                                            label = { Text(stringResource(R.string.sale_quantity)) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    MoneyField(stringResource(R.string.sale_price, line.unit), line.price) { lines[index] = line.copy(price = it) }
                                    results[index]?.let { (lineTotal, lineHpp) ->
                                        Text(
                                            stringResource(R.string.sale_line_result, lineTotal.toRupiah(), lineHpp.toRupiah(), (lineTotal - lineHpp).toRupiah()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (lineTotal - lineHpp <= 0) semantic.danger else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            if (lines.size > 1) {
                                IconButton(onClick = { lines.removeAt(index) }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.sale_remove_line))
                                }
                            } else {
                                Spacer(Modifier.width(12.dp))
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = { lines.add(SaleLineInput(null)) }, enabled = products.isNotEmpty()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(stringResource(R.string.sale_add_line))
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MoneyField(stringResource(R.string.sale_discount), discount) { discount = it }
                        ReportRow(stringResource(R.string.sale_subtotal), subtotal.toRupiah())
                        if (discountRupiah > 0) ReportRow(stringResource(R.string.sale_discount_row), discountRupiah.toRupiah())
                        ReportRow(stringResource(R.string.sale_total), total.toRupiah(), emphasized = true)
                        ReportRow(stringResource(R.string.sale_profit), (total - hpp).toRupiah())
                    }
                }
                item {
                    if (existing == null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.sale_paid_now), style = MaterialTheme.typography.labelLarge)
                            val choices = listOf(
                                PaidChoice.FULL to R.string.sale_paid_full,
                                PaidChoice.NONE to R.string.sale_paid_none,
                                PaidChoice.PART to R.string.sale_paid_part,
                            )
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                choices.forEachIndexed { i, (choice, label) ->
                                    SegmentedButton(
                                        selected = paidChoice == choice,
                                        onClick = { paidChoice = choice },
                                        shape = SegmentedButtonDefaults.itemShape(i, choices.size),
                                        label = { Text(stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    )
                                }
                            }
                            if (paidChoice == PaidChoice.PART) {
                                MoneyField(stringResource(R.string.sale_paid_amount), partAmount) { partAmount = it }
                            }
                        }
                    } else {
                        Text(stringResource(R.string.sale_already_paid, existing.paid.toRupiah()), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes, onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.sale_notes)) }, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Button(
                onClick = {
                    onSave(
                        SaleDraft(
                            contactId = contactId,
                            date = LocalDate.parse(date),
                            dueDate = due.takeIf { showDue && it.isNotBlank() }?.let(LocalDate::parse),
                            discountRupiah = discountRupiah,
                            notes = notes,
                            // A line without a product is an untouched row, not an error.
                            lines = lines.filter { it.productId != null }.map { SaleLineDraft(it.productId!!, it.unit, it.quantity, it.price.toLongOrNull() ?: 0) },
                            paidNowRupiah = when (paidChoice) {
                                PaidChoice.FULL -> total.coerceAtLeast(0)
                                PaidChoice.NONE -> 0
                                PaidChoice.PART -> partAmount.toLongOrNull() ?: 0
                            },
                        ),
                    )
                },
                enabled = products.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(16.dp).imePadding(),
            ) { Text(stringResource(R.string.action_save)) }
        }

        if (picking) {
            BackHandler { picking = false }
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                ContactsScreen(
                    contacts = contacts,
                    sales = sales,
                    payables = payables,
                    onSave = onSaveContact,
                    onArchive = onArchiveContact,
                    onPick = { contactId = it; picking = false },
                    header = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.contacts_pick_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                            IconButton(onClick = { picking = false }) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close)) }
                        }
                    },
                )
            }
        }
    }
}

/** The customer as a read-only field that opens the contact picker; "Umum" when none (F2-3). */
@Composable
private fun CustomerField(name: String?, onOpen: () -> Unit, onClear: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = name ?: stringResource(R.string.sale_customer_none),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.sale_customer)) },
            supportingText = { Text(stringResource(R.string.sale_customer_hint)) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            trailingIcon = if (name != null) {
                { IconButton(onClick = onClear) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.sale_customer_clear)) } }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )
        // Same reason as DateField: a read-only field eats taps, so the opener is a layer above it
        // that stops short of the clear button.
        Box(Modifier.matchParentSize().padding(end = if (name != null) 56.dp else 0.dp, bottom = 24.dp).clickable(onClick = onOpen))
    }
}

/** Status as colour and text: Lunas, Sebagian, Belum dibayar, or Lewat jatuh tempo (DEC-029 §6). */
@Composable
fun SaleStatusBadge(record: SaleRecord, today: LocalDate = LocalDate.now()) {
    val semantic = LocalMankeuSemanticColors.current
    val (label, container, content) = when {
        record.overdue(today) -> Triple(R.string.sale_status_overdue, semantic.dangerContainer, semantic.onDangerContainer)
        record.status == SaleStatus.PAID -> Triple(R.string.sale_status_paid, semantic.successContainer, semantic.onSuccessContainer)
        record.status == SaleStatus.PARTIAL -> Triple(R.string.sale_status_partial, semantic.warningContainer, semantic.onWarningContainer)
        else -> Triple(R.string.sale_status_unpaid, semantic.dangerContainer, semantic.onDangerContainer)
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(8.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

private fun List<SaleRecord>.filtered(filter: NoteFilter, today: LocalDate) = when (filter) {
    NoteFilter.ALL -> this
    NoteFilter.OPEN -> filter { it.outstanding > 0 }
    NoteFilter.OVERDUE -> filter { it.overdue(today) }
}

/**
 * The notes of the unit above the transactions in Catatan (DEC-029 §12, until F3): filter chips and
 * one card per note. "Semua" shows the newest few until asked for all, so a long history does not
 * push the transactions out of reach.
 */
fun LazyListScope.saleNoteItems(
    sales: List<SaleRecord>,
    filter: NoteFilter,
    onFilter: (NoteFilter) -> Unit,
    showAll: Boolean,
    onShowAll: () -> Unit,
    onOpen: (String) -> Unit,
) {
    if (sales.isEmpty()) return
    val today = LocalDate.now()
    val visible = sales.filtered(filter, today)
    val shown = if (showAll) visible else visible.take(NOTES_SHOWN)
    item(key = "notes-title") {
        Text(stringResource(R.string.notes_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
    }
    item(key = "notes-filter") {
        val filters = listOf(NoteFilter.ALL to R.string.notes_filter_all, NoteFilter.OPEN to R.string.notes_filter_open, NoteFilter.OVERDUE to R.string.notes_filter_overdue)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { (value, label) ->
                FilterChip(selected = filter == value, onClick = { onFilter(value) }, label = { Text(stringResource(label)) })
            }
        }
    }
    if (shown.isEmpty()) {
        item(key = "notes-empty") { Text(stringResource(R.string.notes_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    items(shown, key = { "note-" + it.sale.id }) { record -> SaleNoteCard(record, today) { onOpen(record.sale.id) } }
    if (shown.size < visible.size) {
        item(key = "notes-more") {
            TextButton(onClick = onShowAll) { Text(stringResource(R.string.notes_show_all, visible.size)) }
        }
    }
}

private const val NOTES_SHOWN = 5

@Composable
private fun SaleNoteCard(record: SaleRecord, today: LocalDate, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    record.contact?.name ?: stringResource(R.string.sale_customer_none),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        LocalDate.ofEpochDay(record.sale.dateEpochDay).toDisplayDate(),
                        record.dueDate?.takeIf { record.outstanding > 0 }?.let { stringResource(R.string.sale_due_on, it.toDisplayDate()) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(record.total.toRupiah(), fontWeight = FontWeight.Bold)
                SaleStatusBadge(record, today)
                if (record.status == SaleStatus.PARTIAL) {
                    Text(stringResource(R.string.sale_left, record.outstanding.toRupiah()), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** A note with its lines and payments; Bayar (a short sheet), Ubah and Hapus fixed at the bottom. */
@Composable
fun SaleNoteDetailScreen(
    record: SaleRecord,
    products: List<ProductRecord>,
    onPay: (amountRupiah: Long, date: LocalDate, onDone: () -> Unit) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var paying by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(record.contact?.name ?: stringResource(R.string.sale_customer_none), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(
                        listOfNotNull(
                            LocalDate.ofEpochDay(record.sale.dateEpochDay).toDisplayDate(),
                            record.dueDate?.let { stringResource(R.string.sale_due_on, it.toDisplayDate()) },
                        ).joinToString(" · "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SaleStatusBadge(record)
                        if (record.outstanding > 0) Text(stringResource(R.string.sale_left, record.outstanding.toRupiah()), fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { Text(stringResource(R.string.sale_products), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) }
            items(record.lines.sortedBy { it.sortOrder }, key = { it.id }) { line ->
                val name = products.firstOrNull { it.product.id == line.productId }?.product?.name ?: "—"
                val quantity = TransactionCalculation.parseQuantity(line.quantityText)?.toDisplayQuantity() ?: line.quantityText
                Column {
                    Row {
                        Text("$name · $quantity ${line.unitName} × ${line.unitPriceRupiah.toRupiah()}", modifier = Modifier.weight(1f))
                        Text(line.totalRupiah.toRupiah(), fontWeight = FontWeight.Bold)
                    }
                    Text(
                        stringResource(R.string.sale_line_result, line.totalRupiah.toRupiah(), line.hppRupiah.toRupiah(), (line.totalRupiah - line.hppRupiah).toRupiah()),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReportRow(stringResource(R.string.sale_subtotal), record.subtotal.toRupiah())
                    if (record.sale.discountRupiah > 0) ReportRow(stringResource(R.string.sale_discount_row), record.sale.discountRupiah.toRupiah())
                    ReportRow(stringResource(R.string.sale_total), record.total.toRupiah(), emphasized = true)
                    ReportRow(stringResource(R.string.sale_profit), (record.total - record.hpp).toRupiah())
                }
            }
            if (record.sale.notes.isNotBlank()) item { Text(record.sale.notes) }
            item { Text(stringResource(R.string.sale_payments), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) }
            if (record.payments.isEmpty()) {
                item { Text(stringResource(R.string.sale_no_payments), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(record.payments.sortedBy { it.date }, key = { it.id }) { payment ->
                ReportRow(payment.date.toDisplayDate(), payment.amountRupiah.toRupiah())
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onEdit) { Text(stringResource(R.string.action_edit)) }
            Button(onClick = { paying = true }, enabled = record.outstanding > 0) { Text(stringResource(R.string.sale_pay)) }
        }
    }

    if (paying) {
        var amount by rememberSaveable { mutableStateOf(record.outstanding.toString()) }
        var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
        MankeuSheet(
            title = stringResource(R.string.sale_pay_title),
            onDismiss = { paying = false },
            confirmEnabled = (amount.toLongOrNull() ?: 0) > 0,
            onConfirm = { onPay(amount.toLongOrNull() ?: 0, LocalDate.parse(date)) { paying = false } },
        ) {
            Text(stringResource(R.string.sale_left, record.outstanding.toRupiah()), color = MaterialTheme.colorScheme.onSurfaceVariant)
            MoneyField(stringResource(R.string.sale_pay_amount), amount) { amount = it }
            DateField(value = date, label = stringResource(R.string.sale_pay_date), onValueChange = { date = it })
        }
    }
    if (confirmDelete) {
        MankeuSheet(
            title = stringResource(R.string.sale_delete_title),
            onDismiss = { confirmDelete = false },
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { confirmDelete = false; onDelete() },
        ) {
            Text(
                if (record.payments.isEmpty()) stringResource(R.string.sale_delete_body_none)
                else stringResource(R.string.sale_delete_body, record.payments.size),
            )
        }
    }
}

/** Beranda's Tagihan card: what customers still owe, what is owed to suppliers, overdue notes. */
@Composable
fun BillsCard(receivableRupiah: Long, payableRupiah: Long, overdueCount: Int, onOpen: () -> Unit) {
    val semantic = LocalMankeuSemanticColors.current
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.home_bills_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            ReportRow(stringResource(R.string.profit_receivable), receivableRupiah, highlightNegative = false)
            ReportRow(stringResource(R.string.profit_payable), payableRupiah, highlightNegative = false)
            if (overdueCount > 0) {
                Text(stringResource(R.string.home_bills_overdue, overdueCount), color = semantic.danger, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.home_bills_open), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
