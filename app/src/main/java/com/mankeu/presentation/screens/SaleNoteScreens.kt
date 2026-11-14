package com.mankeu.presentation.screens

import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.data.ContactDraft
import com.mankeu.data.ContactEntity
import com.mankeu.data.ProductRecord
import com.mankeu.data.SaleDraft
import com.mankeu.data.SaleLineDraft
import com.mankeu.data.SaleRecord
import com.mankeu.domain.ProductCosting
import com.mankeu.domain.SaleStatus
import com.mankeu.domain.SalesMath
import com.mankeu.domain.StockItem
import com.mankeu.domain.TransactionCalculation
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.MankeuType
import com.mankeu.presentation.ShareLine
import com.mankeu.presentation.ShareNote
import com.mankeu.presentation.ShareText
import com.mankeu.presentation.ShareWords
import com.mankeu.presentation.Space
import com.mankeu.presentation.UiText
import com.mankeu.presentation.resolve
import com.mankeu.presentation.components.DateField
import com.mankeu.presentation.components.MankeuDatePicker
import com.mankeu.presentation.components.MankeuSheet
import com.mankeu.presentation.components.ReportRow
import com.mankeu.presentation.components.form.FullPageForm
import com.mankeu.presentation.components.form.LineEditSheet
import com.mankeu.presentation.components.form.ListRowField
import com.mankeu.presentation.components.form.MoneyChip
import com.mankeu.presentation.components.form.MoneyInput
import com.mankeu.presentation.components.form.PickerPage
import com.mankeu.presentation.components.form.QuantityStepper
import com.mankeu.presentation.components.form.ResultBar
import com.mankeu.presentation.components.form.stepQuantity
import com.mankeu.presentation.toDisplayDate
import com.mankeu.presentation.toDisplayQuantity
import com.mankeu.presentation.toRupiah
import java.time.LocalDate

/** The note list filters in Catatan; Beranda's Tagihan card opens the list on one of them. */
enum class NoteFilter { ALL, OPEN, OVERDUE }

/** One product line while typed; text until saved, so "0", blank or "1," never reach the math. */
private data class SaleLineInput(val productId: String, val unit: String, val quantity: String, val price: String, val name: String = "")

private val saleLineSaver = listSaver<SnapshotStateList<SaleLineInput>, String>(
    save = { rows -> listOf("v2") + rows.flatMap { listOf(it.productId, it.unit, it.quantity, it.price, it.name) } },
    restore = { flat ->
        val current = flat.firstOrNull() == "v2"
        (if (current) flat.drop(1) else flat).chunked(if (current) 5 else 4).map {
            SaleLineInput(it[0], it[1], it[2], it[3], if (current) it[4] else "")
        }.toMutableStateList()
    },
)

private enum class PaidChoice { FULL, NONE, PART }

/** The quick due dates of U-3: days after the note's date. */
private val DUE_DAYS = listOf(7L, 14L, 30L)

/** A product's base unit and its sale units, the names a note line may use (repository rule). */
private fun ProductRecord.unitNames() = (units.sortedBy { it.sortOrder }.map { it.name } + product.unit).distinctBy { it.lowercase() }

private fun ProductRecord.priceOf(unit: String) = units.firstOrNull { it.name.equals(unit, ignoreCase = true) }?.priceRupiah

/** Notes newest first, the order "last used" reads from. */
private fun List<SaleRecord>.newestFirst() = sortedWith(compareByDescending<SaleRecord> { it.sale.dateEpochDay }.thenByDescending { it.sale.createdAtEpochMillis })

/**
 * A new line of [record]: the unit it was last sold in (a wholesaler sells dus, audit L12), else its
 * main sale unit; quantity 1 and that unit's price.
 */
private fun newLine(record: ProductRecord, history: List<SaleRecord>): SaleLineInput {
    val lastUnit = history.asSequence().flatMap { it.lines.asSequence() }.firstOrNull { it.productId == record.product.id }?.unitName
        ?.takeIf { u -> record.unitNames().any { it.equals(u, ignoreCase = true) } }
    val unit = lastUnit ?: record.mainUnit?.name ?: record.product.unit
    return SaleLineInput(record.product.id, unit, "1", record.priceOf(unit)?.toString().orEmpty())
}

/** A new note's line: the product last sold in [history] (else the first product), in its last unit, quantity 1. */
private fun startLines(products: List<ProductRecord>, history: List<SaleRecord>): List<SaleLineInput> {
    val first = history.asSequence().flatMap { it.lines.sortedBy { l -> l.sortOrder }.asSequence() }
        .mapNotNull { l -> products.firstOrNull { it.product.id == l.productId } }.firstOrNull() ?: products.firstOrNull()
    return listOfNotNull(first?.let { newLine(it, history) })
}

/**
 * The sales note (spec #17, F2-3, U5): what → how much → who → payment → details, at most four
 * things on screen. Product lines are compact rows ("2 dus Keripik · Rp600.000") with − and + for
 * the count; tapping one opens a three-field sheet (unit, quantity, price). The customer is a chip
 * (last customers, or search / add), payment a segment, the due date chips only while unpaid.
 * Date, discount and note sit behind "+ Tambah detail". The live total and profit sit above Simpan.
 * Every rule is still checked by `FinanceRepository.saveSale`, whose message shows while this page
 * stays open.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaleNoteEditorScreen(
    title: String,
    existing: SaleRecord?,
    products: List<ProductRecord>,
    materials: List<StockItem>,
    contacts: List<ContactEntity>,
    /** The notes "last used" comes from: last customers, last product and unit. */
    sales: List<SaleRecord>,
    onSave: (SaleDraft) -> Unit,
    onSaveContact: (String?, ContactDraft, (String) -> Unit, (UiText) -> Unit) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: SnackbarHostState? = null,
) {
    val history = remember(sales) { sales.newestFirst() }
    var contactId by rememberSaveable { mutableStateOf(existing?.sale?.contactId) }
    var date by rememberSaveable { mutableStateOf((existing?.sale?.dateEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now()).toString()) }
    var due by rememberSaveable { mutableStateOf(existing?.dueDate?.toString().orEmpty()) }
    var discount by rememberSaveable { mutableStateOf(existing?.sale?.discountRupiah?.takeIf { it > 0 }) }
    var notes by rememberSaveable { mutableStateOf(existing?.sale?.notes.orEmpty()) }
    var paidChoice by rememberSaveable { mutableStateOf(PaidChoice.FULL) }
    var partAmount by rememberSaveable { mutableStateOf<Long?>(null) }
    var details by rememberSaveable { mutableStateOf(existing != null && (existing.sale.discountRupiah > 0 || existing.sale.notes.isNotBlank())) }
    var pickingCustomer by rememberSaveable { mutableStateOf(false) }
    var pickingProduct by rememberSaveable { mutableStateOf(false) }
    var addingFreeLine by rememberSaveable { mutableStateOf(false) }
    var pickingDue by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }
    val lines = rememberSaveable(saver = saleLineSaver) {
        val stored = existing?.lines.orEmpty().sortedBy { it.sortOrder }
            .map { SaleLineInput(it.productId.orEmpty(), it.unitName, it.quantityText.replace('.', ','), it.unitPriceRupiah.toString(), it.name) }
        (if (existing != null) stored else startLines(products, history)).toMutableStateList()
    }
    // Until the owner touches the lines, picking a customer restarts them on what that customer
    // last bought ("Toko A takes dus", audit L12): the untouched default follows the customer.
    val untouched = existing == null && lines.toList() == startLines(products, history.filter { contactId != null && it.sale.contactId == contactId }.ifEmpty { history })
    val pickCustomer = { id: String? ->
        if (untouched) {
            val next = startLines(products, history.filter { id != null && it.sale.contactId == id }.ifEmpty { history })
            lines.clear()
            lines.addAll(next)
        }
        contactId = id
    }
    val snapshot = listOf(lines.toList(), contactId, date, due, discount, notes, paidChoice, partAmount)
    val initial = rememberSaveable { snapshot.toString() }

    val semantic = LocalMankeuSemanticColors.current
    val prices = remember(materials) { materials.associate { it.id to it.basePriceRupiah } }
    val costs = remember(products, prices) { products.associate { it.product.id to ProductCosting.cost(it.spec, prices) } }
    // Per line: (total, hpp) once product, quantity and price are valid; null = not countable yet.
    val results = lines.map { line ->
        val record = products.firstOrNull { it.product.id == line.productId }
        val quantity = TransactionCalculation.parseQuantity(line.quantity)
        val price = line.price.toLongOrNull()?.takeIf { it > 0 }
        if (quantity == null || price == null || (line.productId.isBlank() && line.name.isBlank())) null
        else if (line.productId.isBlank()) SalesMath.lineTotal(quantity, price) to 0L
        else if (record == null) null
        else SalesMath.lineTotal(quantity, price) to (record.product.manualCostRupiah?.takeIf { record.recipe.isEmpty() && record.product.kind != "TRADED" }
            ?.let { TransactionCalculation.computeTotalRupiah(record.contentOf(line.unit) * quantity, it) }
            ?: ProductCosting.hpp(record.spec, prices, record.contentOf(line.unit) * quantity))
    }
    val unknownCost = lines.any { line -> line.productId.isBlank() || (products.firstOrNull { it.product.id == line.productId }?.let { record ->
        !costs.getValue(record.product.id).complete && (record.recipe.isNotEmpty() || record.product.manualCostRupiah == null || record.product.kind == "TRADED")
    } ?: true) }
    val subtotal = results.sumOf { it?.first ?: 0 }
    val hpp = results.sumOf { it?.second ?: 0 }
    val discountRupiah = discount ?: 0
    // Checked here only to explain and to keep Simpan off; saveSale stays the authority.
    val discountTooHigh = discountRupiah > subtotal
    val total = (subtotal - discountRupiah).coerceAtLeast(0)
    val paidNow = when (paidChoice) {
        PaidChoice.FULL -> total
        PaidChoice.NONE -> 0
        PaidChoice.PART -> partAmount ?: 0
    }
    val partTooHigh = paidChoice == PaidChoice.PART && (partAmount ?: 0) > total
    val partFocus = remember { FocusRequester() }
    val discountFocus = remember { FocusRequester() }
    var partFocused by remember { mutableStateOf(false) }
    var discountFocused by remember { mutableStateOf(false) }
    var partTouched by rememberSaveable { mutableStateOf(false) }
    var discountTouched by rememberSaveable { mutableStateOf(false) }
    // F2-3: a customer is needed while the note is not fully paid; shown before Simpan, not only after.
    val needsCustomer = contactId == null && (existing?.paid ?: paidNow) < total
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
    val showDue = if (existing == null) paidChoice != PaidChoice.FULL else existing.outstanding > 0 || due.isNotBlank()
    val customer = contacts.firstOrNull { it.id == contactId } ?: existing?.contact?.takeIf { it.id == contactId }
    val recent = (history.mapNotNull { it.contact }.filter { c -> contacts.any { it.id == c.id } }.distinctBy { it.id }.take(3) + listOfNotNull(customer))
        .distinctBy { it.id }

    Box(modifier.fillMaxSize()) {
        FullPageForm(
            title = title,
            dirty = snapshot.toString() != initial,
            onClose = onClose,
            snackbarHost = snackbarHost,
            saveBlockedReason = when {
                lines.any { line -> products.any { it.product.id == line.productId && it.product.kind == "TRADED" && it.recipe.isEmpty() } } -> stringResource(R.string.sale_trade_cost_unsupported)
                lines.isEmpty() -> stringResource(R.string.form_need_sale_line)
                results.any { it == null } -> {
                    val line = lines[results.indexOfFirst { it == null }]
                    when {
                        line.productId.isBlank() && line.name.isBlank() -> stringResource(R.string.form_need_name)
                        line.productId.isNotBlank() && products.none { it.product.id == line.productId } -> stringResource(R.string.form_choose_item)
                        TransactionCalculation.parseQuantity(line.quantity) == null -> stringResource(R.string.form_need_quantity)
                        else -> stringResource(R.string.form_need_price)
                    }
                }
                partTooHigh -> stringResource(R.string.sale_max_amount, total.toRupiah())
                parsedDate == null -> stringResource(R.string.form_need_date)
                discountTooHigh -> stringResource(R.string.sale_discount_too_high, subtotal.toRupiah())
                else -> null
            },
            onInvalidSave = {
                when {
                    lines.any { line -> products.any { it.product.id == line.productId && it.product.kind == "TRADED" && it.recipe.isEmpty() } } -> false
                    lines.isEmpty() || results.any { it == null } -> false
                    partTooHigh -> { partTouched = true; partFocus.requestFocus(); true }
                    parsedDate == null -> false
                    discountTooHigh -> { discountTouched = true; discountFocus.requestFocus(); true }
                    else -> false
                }
            },
            onSave = {
                onSave(
                    SaleDraft(
                        contactId = contactId,
                        date = parsedDate ?: return@FullPageForm,
                        dueDate = due.takeIf { showDue }?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                        discountRupiah = discountRupiah,
                        notes = notes,
                        lines = lines.map { SaleLineDraft(it.productId.ifBlank { null }, it.unit, it.quantity, it.price.toLongOrNull() ?: 0, name = it.name) },
                        paidNowRupiah = paidNow,
                    ),
                )
            },
            result = if (total > 0) {
                {
                    ResultBar(
                        text = stringResource(R.string.sale_result_total, total.toRupiah()),
                        detail = if (unknownCost) stringResource(R.string.sale_cost_unknown) else stringResource(if (total - hpp < 0) R.string.sale_result_loss else R.string.sale_result_profit, kotlin.math.abs(total - hpp).toRupiah()),
                    )
                }
            } else {
                null
            },
        ) {
            // What and how much.
            FormLabel(stringResource(R.string.sale_products))
            if (products.isEmpty() && lines.isEmpty()) Text(stringResource(R.string.sale_no_products), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column {
                lines.forEachIndexed { index, line ->
                    val record = products.firstOrNull { it.product.id == line.productId }
                    NoteLineRow(
                        title = listOf(line.quantity.ifBlank { "?" }, line.unit, record?.product?.name ?: line.name.ifBlank { "—" }).filter { it.isNotBlank() }.joinToString(" "),
                        amount = results[index]?.first?.toRupiah(),
                        profit = if (unknownCost) null else results[index]?.let { (t, h) -> t - h },
                        onClick = { editing = index },
                        onMinus = stepQuantity(line.quantity, -1)?.let { q -> { lines[index] = line.copy(quantity = q) } },
                        onPlus = { stepQuantity(line.quantity, 1)?.let { q -> lines[index] = line.copy(quantity = q) } },
                    )
                    HorizontalDivider()
                }
            }
            if (products.isNotEmpty()) {
                TextButton(onClick = { pickingProduct = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.sale_add_line))
                }
            }

            TextButton(onClick = { addingFreeLine = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(stringResource(R.string.sale_add_free_line))
            }

            // Who.
            FormLabel(stringResource(R.string.sale_customer))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                FilterChip(selected = contactId == null, onClick = { pickCustomer(null) }, label = { Text(stringResource(R.string.sale_customer_general)) })
                recent.forEach { c ->
                    FilterChip(selected = contactId == c.id, onClick = { pickCustomer(c.id) }, label = { Text(c.name) })
                }
                AssistChip(
                    onClick = { pickingCustomer = true },
                    label = { Text(stringResource(R.string.sale_customer_search)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (needsCustomer) {
                    Text(
                        stringResource(R.string.sale_customer_needed),
                        style = MankeuType.Caption,
                        color = semantic.warning,
                    )
                }
            }

            // Payment.
            if (existing == null) {
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
                    MoneyInput(
                        label = stringResource(R.string.sale_paid_amount),
                        value = partAmount,
                        onValueChange = { partAmount = it },
                        isError = partTouched && partTooHigh,
                        supportingText = { Text(stringResource(R.string.sale_max_amount, total.toRupiah())) },
                        fieldModifier = Modifier.focusRequester(partFocus).onFocusChanged { focus ->
                            if (partFocused && !focus.isFocused) partTouched = true
                            partFocused = focus.isFocused
                        },
                    )
                }
            } else {
                Text(stringResource(R.string.sale_already_paid, existing.paid.toRupiah()), style = MankeuType.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showDue && parsedDate != null) {
                FormLabel(stringResource(R.string.sale_due))
                val dueDate = runCatching { LocalDate.parse(due) }.getOrNull()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                    DUE_DAYS.forEach { days ->
                        val target = parsedDate.plusDays(days)
                        FilterChip(
                            selected = dueDate == target,
                            onClick = { due = if (dueDate == target) "" else target.toString() },
                            label = { Text(stringResource(R.string.sale_due_days, days.toInt())) },
                        )
                    }
                    val custom = dueDate != null && DUE_DAYS.none { parsedDate.plusDays(it) == dueDate }
                    FilterChip(
                        selected = custom,
                        onClick = { pickingDue = true },
                        label = { Text(if (custom) dueDate.toDisplayDate() else stringResource(R.string.sale_due_pick)) },
                    )
                }
            }

            // Details.
            if (!details) {
                OutlinedButton(onClick = { details = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) { Text(stringResource(R.string.form_more_details)) }
            } else {
                DateField(value = date, label = stringResource(R.string.sale_date), onValueChange = { date = it })
                MoneyInput(
                    label = stringResource(R.string.sale_discount),
                    value = discount,
                    onValueChange = { discount = it },
                    isError = discountTouched && discountTooHigh,
                    supportingText = if (discountTouched && discountTooHigh) ({ Text(stringResource(R.string.sale_discount_too_high, subtotal.toRupiah())) }) else null,
                    fieldModifier = Modifier.focusRequester(discountFocus).onFocusChanged { focus ->
                        if (discountFocused && !focus.isFocused) discountTouched = true
                        discountFocused = focus.isFocused
                    },
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.sale_notes)) }, modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        editing?.let { index ->
            val line = lines.getOrNull(index) ?: return@let
            if (line.productId.isBlank()) {
                FreeLineSheet(line, onDismiss = { editing = null }, onSave = { lines[index] = it; editing = null }, onDelete = { lines.removeAt(index); editing = null })
            } else {
                val record = products.firstOrNull { it.product.id == line.productId } ?: return@let
                LineSheet(record = record, line = line, onDismiss = { editing = null },
                    onSave = { lines[index] = it; editing = null }, onDelete = { lines.removeAt(index); editing = null })
            }
        }
        if (addingFreeLine) {
            FreeLineSheet(SaleLineInput("", "", "1", ""), onDismiss = { addingFreeLine = false },
                onSave = { lines.add(it); addingFreeLine = false }, onDelete = null)
        }
        if (pickingDue) {
            MankeuDatePicker(initial = runCatching { LocalDate.parse(due) }.getOrNull() ?: parsedDate, onDismiss = { pickingDue = false }) {
                due = it.toString()
                pickingDue = false
            }
        }
        if (pickingProduct) {
            PickerPage(
                title = stringResource(R.string.sale_product_pick),
                items = products,
                itemLabel = { it.product.name },
                itemKey = { it.product.id },
                itemDetail = { r -> r.units.sortedBy { it.sortOrder }.joinToString(" · ") { "${it.name} ${it.priceRupiah.toRupiah()}" }.ifBlank { null } },
                onPick = { lines.add(newLine(it, history)); pickingProduct = false },
                onClose = { pickingProduct = false },
            )
        }
        if (pickingCustomer) {
            PickerPage(
                title = stringResource(R.string.contacts_pick_title),
                items = contacts.filter { it.isCustomer || !it.isSupplier },
                itemLabel = { it.name },
                itemKey = { it.id },
                itemDetail = { it.phone },
                onPick = { pickCustomer(it.id); pickingCustomer = false },
                onClose = { pickingCustomer = false },
                // "Tambah 'Toko A'" creates the customer with that name and picks it (audit L13).
                onAdd = { name -> onSaveContact(null, ContactDraft(name.trim()), { id -> contactId = id; pickingCustomer = false }, {}) },
            )
        }
    }
}

/** A section label inside a form: the plain caption above chips or rows. */
@Composable
private fun FormLabel(text: String) {
    Text(text, style = MankeuType.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * One note line as a compact row: "2 dus Keripik · Rp600.000" with its profit below; the text opens
 * the line sheet, − and + change the count without opening anything.
 */
@Composable
private fun NoteLineRow(title: String, amount: String?, profit: Long?, onClick: () -> Unit, onMinus: (() -> Unit)?, onPlus: () -> Unit) {
    val semantic = LocalMankeuSemanticColors.current
    Row(Modifier.fillMaxWidth().heightIn(min = Space.Row), verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier.weight(1f).clickable(role = Role.Button, onClick = onClick).padding(vertical = Space.Small),
        ) {
            Text(listOfNotNull(title, amount).joinToString(" · "), style = MankeuType.Body, fontWeight = FontWeight.SemiBold)
            profit?.let {
                Text(
                    stringResource(if (it < 0) R.string.sale_result_loss else R.string.sale_result_profit, kotlin.math.abs(it).toRupiah()),
                    style = MankeuType.Caption,
                    color = if (it <= 0) semantic.danger else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = { onMinus?.invoke() }, enabled = onMinus != null) {
            Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.sale_line_less, title))
        }
        IconButton(onClick = onPlus) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.sale_line_more, title))
        }
    }
}

/** The line sheet: unit, quantity and price (filled from the unit); Hapus removes the line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LineSheet(record: ProductRecord, line: SaleLineInput, onDismiss: () -> Unit, onSave: (SaleLineInput) -> Unit, onDelete: () -> Unit) {
    var draft by remember(line) { mutableStateOf(line) }
    val quantityFocus = remember { FocusRequester() }
    val priceFocus = remember { FocusRequester() }
    var quantityFocused by remember { mutableStateOf(false) }
    var priceFocused by remember { mutableStateOf(false) }
    var quantityTouched by rememberSaveable { mutableStateOf(false) }
    var priceTouched by rememberSaveable { mutableStateOf(false) }
    val quantityInvalid = TransactionCalculation.parseQuantity(draft.quantity) == null
    val priceInvalid = (draft.price.toLongOrNull() ?: 0) <= 0
    LineEditSheet(
        title = record.product.name,
        onDismiss = onDismiss,
        onSave = { onSave(draft) },
        saveBlockedReason = when {
            TransactionCalculation.parseQuantity(draft.quantity) == null -> stringResource(R.string.form_need_quantity)
            (draft.price.toLongOrNull() ?: 0) <= 0 -> stringResource(R.string.form_need_price)
            else -> null
        },
        onDelete = onDelete,
        onInvalidSave = {
            when {
                quantityInvalid -> { quantityTouched = true; quantityFocus.requestFocus() }
                priceInvalid -> { priceTouched = true; priceFocus.requestFocus() }
            }
        },
    ) {
        UnitPicker(
            value = draft.unit,
            options = record.unitNames(),
            onSelect = { unit -> draft = draft.copy(unit = unit, price = record.priceOf(unit)?.toString() ?: draft.price) },
            onCustom = null,
            label = stringResource(R.string.editor_unit_name),
        )
        QuantityStepper(
            value = draft.quantity,
            onValueChange = { draft = draft.copy(quantity = it) },
            label = stringResource(R.string.sale_quantity),
            isError = quantityTouched && quantityInvalid,
            fieldModifier = Modifier.focusRequester(quantityFocus).onFocusChanged { focus ->
                if (quantityFocused && !focus.isFocused) quantityTouched = true
                quantityFocused = focus.isFocused
            },
        )
        if (quantityTouched && quantityInvalid) Text(stringResource(R.string.form_need_quantity), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        val usual = record.priceOf(draft.unit)
        MoneyInput(
            label = stringResource(R.string.sale_price, draft.unit),
            value = draft.price.toLongOrNull(),
            onValueChange = { draft = draft.copy(price = it?.toString().orEmpty()) },
            isError = priceTouched && priceInvalid,
            supportingText = if (priceTouched && priceInvalid) ({ Text(stringResource(R.string.form_need_price)) }) else null,
            fieldModifier = Modifier.focusRequester(priceFocus).onFocusChanged { focus ->
                if (priceFocused && !focus.isFocused) priceTouched = true
                priceFocused = focus.isFocused
            },
            chips = listOfNotNull(usual?.takeIf { it.toString() != draft.price }?.let { MoneyChip(stringResource(R.string.sale_price_usual), it) }),
            imeAction = ImeAction.Done,
        )
    }
}

/** A sale of an item or service that has not been saved as a product. */
@Composable
private fun FreeLineSheet(line: SaleLineInput, onDismiss: () -> Unit, onSave: (SaleLineInput) -> Unit, onDelete: (() -> Unit)?) {
    var draft by remember(line) { mutableStateOf(line) }
    LineEditSheet(
        title = stringResource(R.string.sale_free_line_title), onDismiss = onDismiss,
        onSave = { onSave(draft.copy(name = draft.name.trim())) },
        saveBlockedReason = when {
            draft.name.isBlank() -> stringResource(R.string.form_need_name)
            TransactionCalculation.parseQuantity(draft.quantity) == null -> stringResource(R.string.form_need_quantity)
            (draft.price.toLongOrNull() ?: 0) <= 0 -> stringResource(R.string.form_need_price)
            else -> null
        },
        onDelete = onDelete,
    ) {
        OutlinedTextField(value = draft.name, onValueChange = { draft = draft.copy(name = it) },
            label = { Text(stringResource(R.string.sale_free_line_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = draft.quantity, onValueChange = { draft = draft.copy(quantity = com.mankeu.presentation.components.sanitizeDecimal(it)) },
            label = { Text(stringResource(R.string.sale_quantity)) }, singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        MoneyInput(label = stringResource(R.string.sale_free_line_price), value = draft.price.toLongOrNull(),
            onValueChange = { draft = draft.copy(price = it?.toString().orEmpty()) }, imeAction = ImeAction.Done)
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
 * one card per note. "Belum lunas" is the default, so a long paid history stays out of the way.
 */
fun LazyListScope.saleNoteItems(
    sales: List<SaleRecord>,
    filter: NoteFilter,
    onFilter: (NoteFilter) -> Unit,
    onOpen: (String) -> Unit,
) {
    if (sales.isEmpty()) return
    val today = LocalDate.now()
    val visible = sales.filtered(filter, today)
    item(key = "notes-title") {
        Text(stringResource(R.string.notes_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
    }
    item(key = "notes-filter") {
        val filters = listOf(NoteFilter.ALL to R.string.notes_filter_all, NoteFilter.OPEN to R.string.notes_filter_open, NoteFilter.OVERDUE to R.string.notes_filter_overdue)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters, key = { it.first.name }) { (value, label) ->
                FilterChip(selected = filter == value, onClick = { onFilter(value) }, label = { Text(stringResource(label)) })
            }
        }
    }
    if (visible.isEmpty()) {
        item(key = "notes-empty") { Text(stringResource(R.string.notes_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    items(visible, key = { "note-" + it.sale.id }) { record -> SaleNoteCard(record, today) { onOpen(record.sale.id) } }
}

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

/** The words of Kirim (U-3) from string resources. */
@Composable
fun shareWords() = ShareWords(
    noteTitle = stringResource(R.string.share_note_title),
    customer = stringResource(R.string.share_customer),
    date = stringResource(R.string.share_date),
    total = stringResource(R.string.share_total),
    paid = stringResource(R.string.share_paid),
    left = stringResource(R.string.share_left),
    due = stringResource(R.string.share_due),
    reminderTitle = stringResource(R.string.share_reminder_title),
    reminderTotal = stringResource(R.string.share_reminder_total),
    thanks = stringResource(R.string.share_thanks),
)

/** A stored note as the plain text of Kirim. */
fun SaleRecord.toShareNote(products: List<ProductRecord>) = ShareNote(
    date = LocalDate.ofEpochDay(sale.dateEpochDay),
    lines = lines.sortedBy { it.sortOrder }.map { line ->
        ShareLine(
            product = products.firstOrNull { it.product.id == line.productId }?.product?.name ?: line.name.ifBlank { "—" },
            quantity = TransactionCalculation.parseQuantity(line.quantityText)?.toDisplayQuantity() ?: line.quantityText,
            unit = line.unitName,
            unitPriceRupiah = line.unitPriceRupiah,
            totalRupiah = line.totalRupiah,
        )
    },
    total = total,
    paid = paid,
    due = dueDate,
)

/**
 * Kirim: with a phone number, a choice between WhatsApp straight to that number and any other app;
 * without one, straight to the share sheet. [onSend] gets the text and the WhatsApp link (null =
 * the share sheet), so the screen never builds an Intent itself.
 */
@Composable
fun SendChoice(text: String, phone: String?, onSend: (String, String?) -> Unit, onDismiss: () -> Unit) {
    val link = ShareText.whatsAppLink(phone, text)
    if (link == null) {
        LaunchedEffect(text) { onSend(text, null); onDismiss() }
        return
    }
    MankeuSheet(title = stringResource(R.string.share_title), onDismiss = onDismiss, confirmLabel = null) {
        ListRowField(label = stringResource(R.string.share_whatsapp), value = phone.orEmpty(), onClick = { onSend(text, link); onDismiss() })
        ListRowField(label = stringResource(R.string.share_other_app), value = "", onClick = { onSend(text, null); onDismiss() })
    }
}

/** A note with its lines and payments; Kirim, Ubah and Bayar fixed at the bottom, Hapus at the end of the page. */
@Composable
fun SaleNoteDetailScreen(
    record: SaleRecord,
    products: List<ProductRecord>,
    onPay: (amountRupiah: Long, date: LocalDate, onDone: () -> Unit, onError: (UiText) -> Unit) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    businessName: String = "",
    onSend: (text: String, whatsAppLink: String?) -> Unit = { _, _ -> },
    onOpenCustomer: ((String) -> Unit)? = null,
) {
    var paying by rememberSaveable { mutableStateOf(false) }
    var sending by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val words = shareWords()
    Column(modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(Space.Edge), verticalArrangement = Arrangement.spacedBy(Space.Field)) {
            item(key = "sale_detail_header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val name = record.contact?.name ?: stringResource(R.string.sale_customer_none)
                    val contactId = record.sale.contactId
                    if (contactId != null && onOpenCustomer != null) {
                        ListRowField(label = name, value = stringResource(R.string.customer_open), onClick = { onOpenCustomer(contactId) })
                    } else {
                        Text(name, style = MankeuType.Title)
                    }
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
            item(key = "sale_detail_products_label") { Text(stringResource(R.string.sale_products), style = MankeuType.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(record.lines.sortedBy { it.sortOrder }, key = { it.id }) { line ->
                val name = products.firstOrNull { it.product.id == line.productId }?.product?.name ?: line.name.ifBlank { "—" }
                val quantity = TransactionCalculation.parseQuantity(line.quantityText)?.toDisplayQuantity() ?: line.quantityText
                Column {
                    Row {
                        Text("$quantity ${line.unitName} $name × ${line.unitPriceRupiah.toRupiah()}", modifier = Modifier.weight(1f))
                        Text(line.totalRupiah.toRupiah(), fontWeight = FontWeight.Bold)
                    }
                    Text(
                        stringResource(R.string.sale_line_result, line.totalRupiah.toRupiah(), line.hppRupiah.toRupiah(), (line.totalRupiah - line.hppRupiah).toRupiah()),
                        style = MankeuType.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = "sale_detail_totals") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReportRow(stringResource(R.string.sale_subtotal), record.subtotal.toRupiah())
                    if (record.sale.discountRupiah > 0) ReportRow(stringResource(R.string.sale_discount_row), record.sale.discountRupiah.toRupiah())
                    ReportRow(stringResource(R.string.sale_total), record.total.toRupiah(), emphasized = true)
                    ReportRow(stringResource(R.string.sale_profit), if (record.lines.any { !it.costKnown || it.productId == null }) stringResource(R.string.sale_cost_unknown) else (record.total - record.hpp).toRupiah())
                }
            }
            if (record.sale.notes.isNotBlank()) item(key = "sale_detail_notes") { Text(record.sale.notes) }
            item(key = "sale_detail_payments_label") { Text(stringResource(R.string.sale_payments), style = MankeuType.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (record.payments.isEmpty()) {
                item(key = "sale_detail_no_payments") { Text(stringResource(R.string.sale_no_payments), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(record.payments.sortedBy { it.date }, key = { it.id }) { payment ->
                ReportRow(payment.date.toDisplayDate(), payment.amountRupiah.toRupiah())
            }
            item(key = "sale_detail_delete") {
                TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.sale_delete_action), color = LocalMankeuSemanticColors.current.danger) }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(Space.Edge),
            horizontalArrangement = Arrangement.spacedBy(Space.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { sending = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Space.Small))
                Text(stringResource(R.string.share_action))
            }
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_edit)) }
            Button(onClick = { paying = true }, enabled = record.outstanding > 0, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.sale_pay)) }
        }
    }

    if (sending) {
        SendChoice(
            text = ShareText.note(businessName, record.contact?.name, record.toShareNote(products), words),
            phone = record.contact?.phone,
            onSend = onSend,
            onDismiss = { sending = false },
        )
    }
    if (paying) {
        PaySheet(
            outstanding = record.outstanding,
            earliest = LocalDate.ofEpochDay(record.sale.dateEpochDay),
            onDismiss = { paying = false },
            onPay = onPay,
        )
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

/**
 * Terima bayar: the amount starts on what is left (with a "Lunas" chip to go back to it) and the
 * date on today. Shared by a note's Bayar and the customer page.
 */
@Composable
fun PaySheet(
    outstanding: Long,
    earliest: LocalDate,
    onDismiss: () -> Unit,
    onPay: (amountRupiah: Long, date: LocalDate, onDone: () -> Unit, onError: (UiText) -> Unit) -> Unit,
    title: String = stringResource(R.string.sale_pay_title),
) {
    var amount by rememberSaveable { mutableStateOf<Long?>(outstanding) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var error by remember { mutableStateOf<UiText?>(null) }
    val value = amount ?: 0
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    val amountOk = value in 1..outstanding
    val dateOk = parsed != null && !parsed.isBefore(earliest)
    val amountFocus = remember { FocusRequester() }
    var amountFocused by remember { mutableStateOf(false) }
    var amountTouched by rememberSaveable { mutableStateOf(false) }
    MankeuSheet(
        title = title,
        onDismiss = onDismiss,
        confirmBlockedReason = when {
            value <= 0 -> stringResource(R.string.form_need_amount)
            !amountOk -> stringResource(R.string.sale_max_amount, outstanding.toRupiah())
            parsed == null -> stringResource(R.string.form_need_date)
            !dateOk -> stringResource(R.string.sale_pay_date_early)
            else -> null
        },
        onInvalidConfirm = { if (!amountOk) { amountTouched = true; amountFocus.requestFocus() } },
        onConfirm = { error = null; onPay(value, parsed!!, onDismiss, { error = it }) },
    ) {
        MoneyInput(
            label = stringResource(R.string.sale_pay_amount),
            value = amount,
            onValueChange = { amount = it },
            isError = amountTouched && !amountOk,
            supportingText = { Text(if (amountTouched && value <= 0) stringResource(R.string.form_need_amount) else stringResource(R.string.sale_max_amount, outstanding.toRupiah())) },
            fieldModifier = Modifier.focusRequester(amountFocus).onFocusChanged { focus ->
                if (amountFocused && !focus.isFocused) amountTouched = true
                amountFocused = focus.isFocused
            },
            // Only when the amount was changed: it starts on what is left.
            chips = listOfNotNull(MoneyChip(stringResource(R.string.sale_paid_full), outstanding).takeIf { amount != outstanding }),
            imeAction = ImeAction.Done,
        )
        DateField(value = date, label = stringResource(R.string.sale_pay_date), onValueChange = { date = it })
        if (parsed != null && !dateOk) Text(stringResource(R.string.sale_pay_date_early), color = MaterialTheme.colorScheme.error)
        // A refusal from the repository: the snackbar is behind this sheet.
        error?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
    }
}
