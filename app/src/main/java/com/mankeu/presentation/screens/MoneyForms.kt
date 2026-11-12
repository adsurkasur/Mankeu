package com.mankeu.presentation.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.mankeu.R
import com.mankeu.data.CategoryEntity
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.DefaultMasterData
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.MoneyEvent
import com.mankeu.domain.StockItem
import com.mankeu.domain.TransactionCalculation
import com.mankeu.presentation.AppUiState
import com.mankeu.presentation.BusinessWords
import com.mankeu.presentation.MankeuType
import com.mankeu.presentation.Space
import com.mankeu.presentation.components.DateField
import com.mankeu.presentation.components.MankeuSheet
import com.mankeu.presentation.components.form.FullPageForm
import com.mankeu.presentation.components.form.ListRowField
import com.mankeu.presentation.components.form.MoneyChip
import com.mankeu.presentation.components.form.MoneyInput
import com.mankeu.presentation.components.form.PickerPage
import com.mankeu.presentation.components.form.ResultBar
import com.mankeu.presentation.components.sanitizeDecimal
import com.mankeu.presentation.toRupiah
import java.time.LocalDate

/** The four money pages of U5; the + menu and Beranda open one of them. */
enum class MoneyFormMode { PURCHASE, OTHER_IN, OTHER_OUT, CAPITAL }

/** What a money page saves; the hidden fields of an edited record (reference, plan link, cost nature…) are kept by the caller. */
data class MoneyEntry(
    val type: FinancialEventType,
    val amountRupiah: Long,
    val description: String,
    val category: CategoryEntity,
    val date: LocalDate,
    val quantityText: String? = null,
    val unit: String? = null,
    val unitPriceRupiah: Long? = null,
    val stockItemId: String? = null,
)

/** The page an existing record is corrected on (DEC-019): by direction, category role and material. */
fun moneyFormModeFor(event: MoneyEvent, categories: List<CategoryEntity>): MoneyFormMode {
    val role = CategoryRole.parse(categories.firstOrNull { it.id == event.categoryId }?.role)
    return when {
        role == CategoryRole.CAPITAL_IN || role == CategoryRole.DRAWING -> MoneyFormMode.CAPITAL
        BusinessWords.isIncoming(event.type) == true -> MoneyFormMode.OTHER_IN
        event.stockItemId != null -> MoneyFormMode.PURCHASE
        else -> MoneyFormMode.OTHER_OUT
    }
}

private fun MoneyFormMode.roles(): Set<CategoryRole> = when (this) {
    MoneyFormMode.PURCHASE, MoneyFormMode.OTHER_OUT -> setOf(CategoryRole.EXPENSE, CategoryRole.ASSET)
    MoneyFormMode.OTHER_IN -> setOf(CategoryRole.REVENUE)
    MoneyFormMode.CAPITAL -> setOf(CategoryRole.CAPITAL_IN, CategoryRole.DRAWING)
}

private fun String.toInputQuantity() = replace('.', ',')

/**
 * Belanja, Uang masuk lain, Uang keluar lain and Modal & ambil pribadi (U5, audit L16 L17): full
 * pages with at most four fields, in the order what → how much → details.
 *
 * - **Belanja** starts from "Beli apa?" on the material last bought; with a material it is quantity
 *   and price (filled with its current price), the total live above Simpan with "Stok Minyak +10
 *   liter", and the category follows the material. "Bukan bahan" turns it into category chips and
 *   one amount. No cost nature.
 * - **Uang masuk lain / Uang keluar lain**: amount, category chips, note, date.
 * - **Modal & ambil pribadi**: in or out as a segment, amount, note, date; the category is implied.
 *
 * Validation stays in the repository; this only keeps Simpan off until the minimum is there.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoneyFormScreen(
    mode: MoneyFormMode,
    state: AppUiState,
    existing: MoneyEvent?,
    /** "Catat juga uangnya?" (U3): the material and quantity Belanja opens with. */
    prefill: Pair<String, String>?,
    onSave: (MoneyEntry) -> Unit,
    onClose: () -> Unit,
    onCreateCategory: (String, CategoryRole) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: SnackbarHostState? = null,
) {
    val materials = state.stockItems
    val allowed = state.categories.filter { CategoryRole.parse(it.role) in mode.roles() }
    val lastMaterial = state.events.firstOrNull { e -> e.stockItemId != null && materials.any { it.id == e.stockItemId } }?.stockItemId
    val startMaterial: String? = when {
        existing != null -> existing.stockItemId
        prefill != null -> prefill.first
        mode == MoneyFormMode.PURCHASE -> lastMaterial ?: materials.firstOrNull()?.id
        else -> null
    }
    var materialId by rememberSaveable { mutableStateOf(startMaterial) }
    val material = materials.firstOrNull { it.id == materialId }
    var quantity by rememberSaveable {
        mutableStateOf(existing?.quantityText?.toInputQuantity() ?: prefill?.second?.toInputQuantity().orEmpty())
    }
    var unitPrice by rememberSaveable { mutableStateOf(existing?.unitPriceRupiah ?: material?.basePriceRupiah?.takeIf { it > 0 }) }
    var amount by rememberSaveable { mutableStateOf(existing?.amountRupiah) }
    var categoryId by rememberSaveable { mutableStateOf(existing?.categoryId) }
    // A default category filled in by the page is not "something typed": only a tap on a chip makes the page dirty.
    var categoryTouched by rememberSaveable { mutableStateOf(false) }
    var outgoing by rememberSaveable {
        mutableStateOf(existing?.let { CategoryRole.parse(state.categories.firstOrNull { c -> c.id == it.categoryId }?.role) == CategoryRole.DRAWING } ?: false)
    }
    var description by rememberSaveable { mutableStateOf(existing?.description.orEmpty()) }
    var date by rememberSaveable { mutableStateOf((existing?.date ?: LocalDate.now()).toString()) }
    var details by rememberSaveable { mutableStateOf(mode != MoneyFormMode.PURCHASE || existing?.description?.isNotBlank() == true) }
    var pickingMaterial by rememberSaveable { mutableStateOf(false) }
    var newCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val snapshot = listOf(materialId, quantity, unitPrice, amount, categoryTouched, outgoing, description, date).toString()
    val initial = rememberSaveable { snapshot }

    // A material-less page needs a category: the last one used here, else the usual starting one.
    val capitalRole = if (outgoing) CategoryRole.DRAWING else CategoryRole.CAPITAL_IN
    val category: CategoryEntity? = when {
        mode == MoneyFormMode.CAPITAL -> allowed.firstOrNull { CategoryRole.parse(it.role) == capitalRole }
        material != null -> allowed.firstOrNull { it.id == categoryId }
            ?: state.events.firstOrNull { it.stockItemId == material.id }?.categoryId?.let { id -> allowed.firstOrNull { it.id == id } }
            ?: allowed.firstOrNull { it.id == defaultCategoryId(allowed.filter { c -> CategoryRole.parse(c.role) == CategoryRole.EXPENSE }, state.events) }
        else -> allowed.firstOrNull { it.id == categoryId }
    }
    LaunchedEffect(mode, allowed.size, material == null) {
        if (mode != MoneyFormMode.CAPITAL && material == null && allowed.none { it.id == categoryId }) {
            categoryId = if (mode == MoneyFormMode.OTHER_IN && state.products.isNotEmpty()) {
                // A business with products sells through notes (audit L16): money in here is "other",
                // so it starts on the last such category used, else the last starting income category.
                state.events.firstOrNull { e -> e.saleId == null && allowed.any { it.id == e.categoryId } }?.categoryId
                    ?: allowed.maxByOrNull { c -> DefaultMasterData.categories.indexOfFirst { it.name == c.name } }?.id
            } else {
                defaultCategoryId(allowed, state.events)
            }
        }
    }
    LaunchedEffect(state.categories, pendingCategory) {
        pendingCategory?.let { name -> state.categories.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { categoryId = it.id; pendingCategory = null } }
    }

    val parsedQuantity = TransactionCalculation.parseQuantity(quantity)
    val total: Long? = if (material != null) {
        if (parsedQuantity != null && unitPrice != null && unitPrice!! > 0) TransactionCalculation.computeTotalRupiah(parsedQuantity, unitPrice!!) else null
    } else {
        amount?.takeIf { it > 0 }
    }
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
    val saveBlockedReason = when {
        material != null && parsedQuantity == null -> stringResource(R.string.form_need_quantity)
        material != null && (unitPrice == null || unitPrice!! <= 0) -> stringResource(R.string.form_need_price)
        total == null -> stringResource(R.string.form_need_amount)
        category == null -> stringResource(R.string.form_need_category)
        parsedDate == null -> stringResource(R.string.form_need_date)
        else -> null
    }
    val amountFocus = remember { FocusRequester() }
    val title = stringResource(
        when (mode) {
            MoneyFormMode.PURCHASE -> R.string.money_title_purchase
            MoneyFormMode.OTHER_IN -> R.string.money_title_in
            MoneyFormMode.OTHER_OUT -> R.string.money_title_out
            MoneyFormMode.CAPITAL -> R.string.money_title_capital
        },
    )

    Box(modifier.fillMaxSize()) {
        FullPageForm(
            title = title,
            dirty = snapshot != initial,
            onClose = onClose,
            snackbarHost = snackbarHost,
            saveBlockedReason = saveBlockedReason,
            onSave = {
                val cat = category ?: return@FullPageForm
                val t = total ?: return@FullPageForm
                val d = parsedDate ?: return@FullPageForm
                onSave(
                    MoneyEntry(
                        type = CategoryRole.parse(cat.role).cashTypeFor(mode == MoneyFormMode.OTHER_IN || (mode == MoneyFormMode.CAPITAL && !outgoing))
                            ?: FinancialEventType.CASH_EXPENSE,
                        amountRupiah = t,
                        // A purchase without a note is titled by what was bought ("Minyak 10 liter", audit L17).
                        description = description.trim().ifBlank { material?.let { "${it.name} $quantity ${it.unit}" }.orEmpty() },
                        category = cat,
                        date = d,
                        quantityText = quantity.takeIf { material != null } ?: existing?.quantityText,
                        unit = material?.unit ?: existing?.unit,
                        unitPriceRupiah = unitPrice.takeIf { material != null } ?: existing?.unitPriceRupiah,
                        stockItemId = material?.id,
                    ),
                )
            },
            result = total?.let { t ->
                {
                    ResultBar(
                        text = stringResource(R.string.sale_result_total, t.toRupiah()),
                        detail = material?.let { m -> parsedQuantity?.let { stringResource(R.string.purchase_stock_up, m.name, quantity, m.unit) } },
                    )
                }
            },
        ) {
            if (mode == MoneyFormMode.PURCHASE) {
                ListRowField(
                    label = stringResource(R.string.purchase_what),
                    value = material?.name ?: stringResource(R.string.purchase_not_material),
                    onClick = { pickingMaterial = true },
                )
            }
            if (mode == MoneyFormMode.CAPITAL) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(false to R.string.capital_in, true to R.string.capital_out).forEachIndexed { i, (out, label) ->
                        SegmentedButton(
                            selected = outgoing == out,
                            onClick = { outgoing = out },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
            }
            if (material != null) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = sanitizeDecimal(it) },
                    label = { Text(stringResource(R.string.purchase_quantity)) },
                    suffix = { Text(material.unit) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().focusRequester(amountFocus),
                )
                MoneyInput(
                    label = stringResource(R.string.purchase_price, material.unit),
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    chips = listOfNotNull(
                        material.basePriceRupiah.takeIf { it > 0 && it != unitPrice }?.let { MoneyChip(stringResource(R.string.purchase_last_price), it) },
                    ),
                    imeAction = ImeAction.Done,
                )
            } else {
                MoneyInput(
                    label = stringResource(R.string.entry_amount),
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.focusRequester(amountFocus),
                )
                if (mode != MoneyFormMode.CAPITAL) {
                    Text(stringResource(R.string.money_category), style = MankeuType.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                        allowed.forEach { c ->
                            FilterChip(selected = categoryId == c.id, onClick = { categoryId = c.id; categoryTouched = true }, label = { Text(c.name) })
                        }
                        FilterChip(selected = false, onClick = { newCategory = "" }, label = { Text(stringResource(R.string.money_category_new)) })
                    }
                }
            }
            if (!details) {
                TextButton(onClick = { details = true }) { Text(stringResource(R.string.money_more_details)) }
            } else {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.entry_description)) },
                    placeholder = { Text(stringResource(if (mode == MoneyFormMode.OTHER_IN || mode == MoneyFormMode.CAPITAL) R.string.money_note_hint_in else R.string.money_note_hint_out)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                DateField(value = date, label = stringResource(R.string.entry_date), onValueChange = { date = it })
            }
        }
        // The keyboard comes up on the number, the one thing that always has to be typed.
        LaunchedEffect(Unit) { if (existing == null) runCatching { amountFocus.requestFocus() } }

        if (pickingMaterial) {
            val notMaterial = stringResource(R.string.purchase_not_material)
            val notMaterialHint = stringResource(R.string.purchase_not_material_hint)
            val perUnit = stringResource(R.string.purchase_material_detail)
            PickerPage<StockItem?>(
                title = stringResource(R.string.purchase_what),
                items = listOf<StockItem?>(null) + materials,
                itemLabel = { it?.name ?: notMaterial },
                itemKey = { it?.let { material -> "material_" + material.id } ?: "not_material" },
                itemDetail = { m -> m?.let { perUnit.format(it.basePriceRupiah.toRupiah(), it.unit) } ?: notMaterialHint },
                onPick = { m ->
                    materialId = m?.id
                    if (m != null) unitPrice = m.basePriceRupiah.takeIf { it > 0 }
                    pickingMaterial = false
                },
                onClose = { pickingMaterial = false },
            )
        }
        newCategory?.let { name ->
            MankeuSheet(
                title = stringResource(R.string.entry_new_category_title),
                onDismiss = { newCategory = null },
                confirmBlockedReason = if (name.isBlank()) stringResource(R.string.form_need_name) else null,
                onConfirm = {
                    val role = if (mode == MoneyFormMode.OTHER_IN) CategoryRole.REVENUE else CategoryRole.EXPENSE
                    onCreateCategory(name.trim(), role)
                    pendingCategory = name.trim()
                    newCategory = null
                },
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { newCategory = it }, singleLine = true,
                    label = { Text(stringResource(R.string.entry_new_category_hint)) }, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
