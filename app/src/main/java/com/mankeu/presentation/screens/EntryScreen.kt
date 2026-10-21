package com.mankeu.presentation.screens

import com.mankeu.domain.MoneyEvent
import androidx.compose.foundation.layout.Box
import com.mankeu.presentation.guide.rememberSheetTour
import com.mankeu.presentation.guide.SheetTour
import com.mankeu.presentation.guide.Tour
import com.mankeu.presentation.guide.HelpArticle
import com.mankeu.presentation.components.rememberTourListState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.CostNature
import com.mankeu.data.PlanAssembly
import com.mankeu.domain.FinancialEventType
import com.mankeu.presentation.AppUiState
import com.mankeu.presentation.ChoiceMenu
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.LocalMotion
import com.mankeu.presentation.FeedbackEvent
import com.mankeu.presentation.LocalFeedback
import com.mankeu.presentation.components.DateField
import com.mankeu.domain.TransactionCalculation
import com.mankeu.presentation.components.EmptyCard
import com.mankeu.presentation.components.EventCard
import com.mankeu.presentation.components.MoneyField
import com.mankeu.presentation.components.TourTarget
import com.mankeu.presentation.components.tourTarget
import com.mankeu.presentation.components.SectionTitle
import com.mankeu.presentation.components.sanitizeDecimal
import com.mankeu.presentation.components.sanitizeRupiah
import com.mankeu.presentation.toRupiah
import java.math.BigDecimal
import java.time.LocalDate
import com.mankeu.presentation.BusinessWords
import com.mankeu.presentation.components.NoticeCard
import kotlinx.coroutines.launch

private data class EntryKind(
    val type: FinancialEventType,
    val labelRes: Int,
    val helperRes: Int,
    val icon: ImageVector,
)

private val entryKinds = listOf(
    EntryKind(FinancialEventType.CASH_INCOME, R.string.kind_cash_in, R.string.kind_cash_in_help, Icons.Default.ArrowDownward),
    EntryKind(FinancialEventType.CASH_EXPENSE, R.string.kind_cash_out, R.string.kind_cash_out_help, Icons.Default.ArrowUpward),
)

/**
 * The Catat (record transaction) destination: pick money in or out, fill amount/description/date
 * and a category. The category's role decides the stored cash type (DEC-011 K-2), so owner capital,
 * drawings and equipment purchases need no extra buttons. Advanced fields (quantity/unit/reference/
 * budget link) are collapsed by default to keep the common case fast.
 *
 * The detail form opens as a [ModalBottomSheet] once a kind is picked, instead of expanding
 * inline underneath the picker (the previous behaviour). Inline expansion pushed the amount/
 * description/save controls further down the same scrolling list the kind buttons live in — on a
 * phone screen the save button routinely ended up below the fold, so picking a kind "did
 * something" with no visible result until the owner scrolled down to find it. A bottom sheet is
 * the correct native pattern here: it's the same one-thing-at-a-time flow (pick kind -> fill
 * fields -> save) but the form is never fighting the picker for scroll position, and dismissing
 * it (swipe down, tap the scrim, or a successful save) unambiguously returns to the picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    state: AppUiState,
    onRecord: (
        FinancialEventType, Long, String, String?, String, LocalDate, String?, String?, String?, Long?, String?,
        CostNature?, String?, Int?, String?,
    ) -> Unit,
    onDelete: (String) -> Unit,
    onOpenData: () -> Unit,
    isSaving: Boolean = false,
    onStartTour: (() -> Unit)? = null,
    /** Set by the Beranda/Catat speed dial: open straight onto this kind's form. */
    requestedKind: FinancialEventType? = null,
    onRequestedKindConsumed: () -> Unit = {},
    onCreateCategory: (String, CategoryRole) -> Unit = { _, _ -> },
) {
    var selected by rememberSaveable { mutableStateOf<FinancialEventType?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var direction by rememberSaveable { mutableStateOf("ALL") }
    var amount by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var reference by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("") }
    var unitPrice by rememberSaveable { mutableStateOf("") }
    var budgetItemId by rememberSaveable { mutableStateOf<String?>(null) }
    // DEC-014: the product a sale was for, a per-entry cost-nature override, an asset's life.
    var productId by rememberSaveable { mutableStateOf<String?>(null) }
    var costNature by rememberSaveable { mutableStateOf<String?>(null) }
    var usefulLife by rememberSaveable { mutableStateOf(PlanAssembly.DEFAULT_ASSET_LIFE_MONTHS.toString()) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var otherKinds by rememberSaveable { mutableStateOf(false) }
    var isCreatingCategory by rememberSaveable { mutableStateOf(false) }
    var newCategoryName by rememberSaveable { mutableStateOf("") }
    var pendingCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    var isAutoCalculated by rememberSaveable { mutableStateOf(false) }
    var isUnitPriceAutoDerived by rememberSaveable { mutableStateOf(false) }
    // Set the moment a save is dispatched, cleared when the sheet next opens.
    //
    // `isSaving` alone was not enough: the write finishes in a few milliseconds, well before the
    // sheet's exit animation does, so a second and third tap landed on a form this block had
    // already cleared and failed its *validation* — the owner saw "Jumlah uang harus lebih dari
    // nol" immediately after a save that had in fact worked. Observed with three synthetic taps
    // fired inside one frame.
    var justSaved by rememberSaveable { mutableStateOf(false) }
    // The record being corrected with Ubah; null when the form records a new one.
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }

    /** Opens the form on [event], filled in, so saving corrects it in place (DEC-019). */
    fun startEdit(event: MoneyEvent) {
        editingId = event.id
        selected = if (BusinessWords.isIncoming(event.type)) FinancialEventType.CASH_INCOME else FinancialEventType.CASH_EXPENSE
        amount = event.amountRupiah.toString()
        description = event.description
        date = event.date.toString()
        categoryId = event.categoryId
        reference = event.reference.orEmpty()
        quantity = event.quantityText.orEmpty()
        unit = event.unit.orEmpty()
        unitPrice = event.unitPriceRupiah?.toString().orEmpty()
        budgetItemId = event.budgetItemId
        productId = event.productId
        costNature = event.costNature?.name
        usefulLife = (event.usefulLifeMonths ?: PlanAssembly.DEFAULT_ASSET_LIFE_MONTHS).toString()
        advanced = quantity.isNotBlank() || unit.isNotBlank() || unitPrice.isNotBlank() || reference.isNotBlank()
        isAutoCalculated = false
        isUnitPriceAutoDerived = false
        localError = null
        justSaved = false
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScope = rememberCoroutineScope()

    fun closeSheet() {
        sheetScope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) selected = null
        }
    }

    val errAmount = stringResource(R.string.entry_err_amount)
    val errDescription = stringResource(R.string.entry_err_description)
    val errDate = stringResource(R.string.entry_err_date)
    val errCategory = stringResource(R.string.entry_err_category)
    val errQuantity = stringResource(R.string.entry_err_quantity)
    val errNoProject = stringResource(R.string.entry_err_no_project)
    val errLife = stringResource(R.string.entry_err_life)
    // These three errors are shown on their own field and focus it, so the keyboard's
    // bring-into-view scrolls the field above the IME. The rest keep the line above Simpan.
    val inlineErrors = setOf(errAmount, errDescription)
    val amountFocus = remember { FocusRequester() }
    val descriptionFocus = remember { FocusRequester() }
    val feedback = LocalFeedback.current

    val kind = entryKinds.firstOrNull { it.type == selected }
    val selectedRole = CategoryRole.parse(state.categories.firstOrNull { it.id == categoryId }?.role)
    val incoming = selected == FinancialEventType.CASH_INCOME
    // Operating categories first, then capital/drawing/asset ones (which never touch profit).
    val compatibleCategories = state.categories
        .filter { CategoryRole.parse(it.role).cashTypeFor(incoming) != null }
        .sortedBy { CategoryRole.parse(it.role) !in setOf(CategoryRole.REVENUE, CategoryRole.EXPENSE) }
    LaunchedEffect(selected, compatibleCategories) {
        if (compatibleCategories.none { it.id == categoryId }) categoryId = compatibleCategories.firstOrNull()?.id
    }
    LaunchedEffect(state.categories, pendingCategoryName) {
        pendingCategoryName?.let { pending ->
            val match = state.categories.firstOrNull { it.name.equals(pending, ignoreCase = true) }
            if (match != null) {
                categoryId = match.id
                pendingCategoryName = null
            }
        }
    }
    // Consumed immediately so returning to Catat later does not reopen the form on its own.
    LaunchedEffect(requestedKind) {
        requestedKind?.let {
            editingId = null
            selected = it
            localError = null
            justSaved = false
            onRequestedKindConsumed()
        }
    }

    val visibleEvents = state.events.filter { event ->
        val matchesText = search.isBlank() ||
            listOf(event.description, event.category, event.reference)
                .filterNotNull()
                .any { it.contains(search, ignoreCase = true) }
        val incoming = BusinessWords.isIncoming(event.type)
        matchesText && when (direction) {
            "IN" -> incoming == true
            "OUT" -> incoming == false
            else -> true
        }
    }

    LazyColumn(
        state = rememberTourListState(),
        // bottom clears the speed dial; see HomeScreen for why the list needs it itself.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionTitle(
                title = stringResource(R.string.entry_title),
                subtitle = stringResource(R.string.ledger_subtitle, state.eventCount),
                modifier = Modifier.tourTarget(TourTarget.ENTRY_HISTORY),
            )
        }

        if (state.selectedProjectId == null || state.projects.isEmpty()) {
            item {
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
                            onClick = onOpenData,
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

        if (state.eventCount > 0) {
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text(stringResource(R.string.home_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                val filters = listOf(
                    "ALL" to stringResource(R.string.money_all),
                    "IN" to stringResource(R.string.money_in_short),
                    "OUT" to stringResource(R.string.money_out_short),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters) { (value, label) ->
                        FilterChip(
                            selected = direction == value,
                            onClick = { feedback(FeedbackEvent.TAP); direction = value },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        when {
            state.eventCount == 0 -> item {
                EmptyCard(
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.home_empty_body),
                    action = stringResource(R.string.home_empty_action),
                    learn = HelpArticle.RECORD_INCOME,
                    onAction = {
                        editingId = null
                        selected = FinancialEventType.CASH_INCOME
                        localError = null
                        justSaved = false
                    },
                )
            }

            visibleEvents.isEmpty() -> item {
                EmptyCard(
                    title = stringResource(R.string.home_no_match_title),
                    body = stringResource(R.string.home_no_match_body),
                )
            }

            else -> {
                items(visibleEvents, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onEdit = { startEdit(it) },
                        onDelete = onDelete,
                    )
                }
                if (state.eventCount > state.events.size) {
                    item {
                        NoticeCard(
                            title = stringResource(R.string.ledger_truncated_title),
                            body = stringResource(
                                R.string.ledger_truncated_body,
                                state.events.size,
                                state.eventCount,
                            ),
                            icon = Icons.Default.Info,
                        )
                    }
                }
            }
        }
    }

    // Detail form lives in a bottom sheet
    if (kind != null) {
        val sheetMotion = LocalMotion.current
        ModalBottomSheet(
            onDismissRequest = { feedback(FeedbackEvent.TAP); selected = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
        ) {
          val formTour = rememberSheetTour(Tour.ENTRY_FORM)
          Box(Modifier.fillMaxWidth()) {
            // Same layout contract as MankeuSheet: the keyboard shrinks the sheet (imePadding on the
            // outer column), only the fields scroll inside what is left (weight + verticalScroll),
            // and Batal / Simpan sit below the scroll so they stay on screen with the keyboard up.
            // The save button used to live at the end of the scrolling content, where an open
            // keyboard made it unreachable even by scrolling.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().tourTarget(TourTarget.ENTRY_KINDS)) {
                    SegmentedButton(
                        selected = selected == FinancialEventType.CASH_INCOME,
                        onClick = {
                            selected = FinancialEventType.CASH_INCOME
                            localError = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) {
                        Text(stringResource(R.string.kind_cash_in), fontWeight = FontWeight.Bold)
                    }
                    SegmentedButton(
                        selected = selected == FinancialEventType.CASH_EXPENSE,
                        onClick = {
                            selected = FinancialEventType.CASH_EXPENSE
                            localError = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) {
                        Text(stringResource(R.string.kind_cash_out), fontWeight = FontWeight.Bold)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(kind.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(stringResource(kind.labelRes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(stringResource(kind.helperRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (editingId != null) {
                    Text(stringResource(R.string.entry_editing), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                MoneyField(
                    label = stringResource(R.string.entry_amount),
                    value = amount,
                    modifier = Modifier.focusRequester(amountFocus).tourTarget(TourTarget.ENTRY_AMOUNT),
                    isError = localError == errAmount,
                    supportingText = {
                        if (localError == errAmount) {
                            Text(errAmount)
                            return@MoneyField
                        }
                        val parsedAmt = amount.toLongOrNull()
                        val parsedQty = TransactionCalculation.parseQuantity(quantity)
                        val parsedPrice = unitPrice.toLongOrNull()
                        if (isAutoCalculated && parsedQty != null && parsedPrice != null && parsedPrice > 0 && parsedAmt != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    stringResource(
                                        R.string.entry_auto_calculated,
                                        quantity,
                                        unit.ifBlank { stringResource(R.string.entry_unit_default) },
                                        parsedPrice.toRupiah(),
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        } else if (parsedAmt != null) {
                            Text(parsedAmt.toRupiah())
                        } else if (amount.isNotBlank()) {
                            Text(stringResource(R.string.entry_amount_invalid), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    onValueChange = { raw ->
                        val clean = sanitizeRupiah(raw)
                        amount = clean
                        if (localError == errAmount) localError = null
                        isAutoCalculated = false
                        val parsedAmt = clean.toLongOrNull()
                        val parsedQty = TransactionCalculation.parseQuantity(quantity)
                        if (parsedQty != null && parsedAmt != null && parsedAmt > 0) {
                            val derived = TransactionCalculation.computeUnitPriceRupiah(parsedAmt, parsedQty)
                            unitPrice = derived.toString()
                            isUnitPriceAutoDerived = true
                        } else if (parsedAmt == null || parsedAmt == 0L) {
                            if (isUnitPriceAutoDerived) {
                                unitPrice = ""
                                isUnitPriceAutoDerived = false
                            }
                        }
                    },
                )

                // Quick Amount Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val increments = listOf(
                        "+50 rb" to 50_000L,
                        "+100 rb" to 100_000L,
                        "+500 rb" to 500_000L,
                        "+1 jt" to 1_000_000L,
                        "+5 jt" to 5_000_000L,
                    )
                    items(increments) { (label, value) ->
                        androidx.compose.material3.SuggestionChip(
                            onClick = {
                                val current = amount.toLongOrNull() ?: 0L
                                val newAmt = current + value
                                amount = sanitizeRupiah(newAmt.toString())
                                if (localError == errAmount) localError = null
                                isAutoCalculated = false
                                val parsedQty = TransactionCalculation.parseQuantity(quantity)
                                if (parsedQty != null && newAmt > 0) {
                                    val derived = TransactionCalculation.computeUnitPriceRupiah(newAmt, parsedQty)
                                    unitPrice = derived.toString()
                                    isUnitPriceAutoDerived = true
                                }
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        )
                    }
                    item {
                        androidx.compose.material3.SuggestionChip(
                            onClick = {
                                amount = ""
                                isAutoCalculated = false
                                if (isUnitPriceAutoDerived) {
                                    unitPrice = ""
                                    isUnitPriceAutoDerived = false
                                }
                            },
                            label = { Text(stringResource(R.string.entry_clear), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) },
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        if (localError == errDescription) localError = null
                    },
                    label = { Text(stringResource(R.string.entry_description)) },
                    isError = localError == errDescription,
                    supportingText = if (localError == errDescription) {
                        { Text(errDescription) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    placeholder = {
                        Text(
                            if (selected == FinancialEventType.CASH_INCOME) {
                                stringResource(R.string.entry_hint_income)
                            } else {
                                stringResource(R.string.entry_hint_expense)
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(descriptionFocus),
                )
                ChoiceMenu(
                    modifier = Modifier.tourTarget(TourTarget.ENTRY_CATEGORY),
                    fieldLabel = stringResource(R.string.entry_category),
                    label = compatibleCategories.firstOrNull { it.id == categoryId }?.name
                        ?: stringResource(R.string.entry_category_pick),
                    options = compatibleCategories.map { it.id to it.name },
                    onSelected = { categoryId = it },
                    addLabel = stringResource(R.string.entry_add_custom_category),
                    onAddAction = { isCreatingCategory = true },
                )
                AnimatedVisibility(visible = isCreatingCategory) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(R.string.entry_new_category_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            OutlinedTextField(
                                value = newCategoryName,
                                onValueChange = { newCategoryName = it },
                                label = { Text(stringResource(R.string.entry_new_category_hint)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                TextButton(onClick = { isCreatingCategory = false; newCategoryName = "" }) {
                                    Text(stringResource(R.string.action_cancel))
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val trimmed = newCategoryName.trim()
                                        if (trimmed.isNotBlank()) {
                                            val role = if (incoming) CategoryRole.REVENUE else CategoryRole.EXPENSE
                                            onCreateCategory(trimmed, role)
                                            pendingCategoryName = trimmed
                                            isCreatingCategory = false
                                            newCategoryName = ""
                                        }
                                    },
                                    enabled = newCategoryName.isNotBlank(),
                                ) {
                                    Text(stringResource(R.string.entry_new_category_save))
                                }
                            }
                        }
                    }
                }
                if (incoming && selectedRole == CategoryRole.REVENUE && state.products.isNotEmpty()) {
                    // Optional (B-7). Picking a product fills unit and price and opens the detail
                    // so the quantity can be typed; the amount then follows quantity × price.
                    ChoiceMenu(
                        fieldLabel = stringResource(R.string.entry_product),
                        label = state.products.firstOrNull { it.product.id == productId }?.product?.name
                            ?: stringResource(R.string.entry_product_none),
                        options = listOf("" to stringResource(R.string.entry_product_none)) +
                            state.products.map { it.product.id to it.product.name },
                        onSelected = { id ->
                            productId = id.ifBlank { null }
                            state.products.firstOrNull { it.product.id == productId }?.product?.let { product ->
                                unit = product.unit
                                unitPrice = product.priceRupiah.toString()
                                isUnitPriceAutoDerived = false
                                TransactionCalculation.parseQuantity(quantity)?.let { q ->
                                    amount = TransactionCalculation.computeTotalRupiah(q, product.priceRupiah).toString()
                                    isAutoCalculated = true
                                }
                                advanced = true
                            }
                        },
                    )
                }
                if (selectedRole == CategoryRole.ASSET) {
                    OutlinedTextField(
                        value = usefulLife,
                        onValueChange = { usefulLife = it.filter(Char::isDigit).take(4) },
                        label = { Text(stringResource(R.string.plan_line_life)) },
                        supportingText = { Text(stringResource(R.string.entry_life_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                DateField(
                    value = date,
                    label = stringResource(R.string.entry_date),
                    onValueChange = { date = it },
                )
                TextButton(onClick = { advanced = !advanced }) {
                    Text(stringResource(if (advanced) R.string.entry_hide_detail else R.string.entry_show_detail))
                }
                AnimatedVisibility(
                    visible = advanced,
                    enter = expandVertically(tween(maxOf(sheetMotion.mediumMillis, 1), easing = FastOutSlowInEasing)) +
                        fadeIn(tween(maxOf(sheetMotion.mediumMillis, 1))),
                    exit = shrinkVertically(tween(maxOf(sheetMotion.fastMillis, 1), easing = FastOutSlowInEasing)) +
                        fadeOut(tween(maxOf(sheetMotion.fastMillis, 1))),
                ) {
                  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        label = { Text(stringResource(R.string.entry_reference)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { raw ->
                                val clean = sanitizeDecimal(raw)
                                quantity = clean
                                val parsedQty = TransactionCalculation.parseQuantity(clean)
                                val parsedPrice = unitPrice.toLongOrNull()
                                val parsedAmt = amount.toLongOrNull()
                                if (parsedQty != null && parsedPrice != null && parsedPrice > 0) {
                                    val calculated = TransactionCalculation.computeTotalRupiah(parsedQty, parsedPrice)
                                    amount = calculated.toString()
                                    isAutoCalculated = true
                                    isUnitPriceAutoDerived = false
                                } else if (parsedQty != null && parsedAmt != null && parsedAmt > 0 && (parsedPrice == null || parsedPrice == 0L || isUnitPriceAutoDerived)) {
                                    val derived = TransactionCalculation.computeUnitPriceRupiah(parsedAmt, parsedQty)
                                    unitPrice = derived.toString()
                                    isUnitPriceAutoDerived = true
                                    isAutoCalculated = false
                                } else if (parsedQty == null) {
                                    if (isAutoCalculated) isAutoCalculated = false
                                    if (isUnitPriceAutoDerived) {
                                        unitPrice = ""
                                        isUnitPriceAutoDerived = false
                                    }
                                }
                            },
                            label = { Text(stringResource(R.string.entry_quantity)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text(stringResource(R.string.entry_unit)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    MoneyField(
                        label = stringResource(R.string.entry_unit_price),
                        imeAction = ImeAction.Done,
                        value = unitPrice,
                        supportingText = {
                            val parsedPrice = unitPrice.toLongOrNull()
                            val parsedQty = TransactionCalculation.parseQuantity(quantity)
                            val parsedAmt = amount.toLongOrNull()
                            if (isUnitPriceAutoDerived && parsedQty != null && parsedAmt != null && parsedPrice != null) {
                                Text(
                                    stringResource(
                                        R.string.entry_auto_unit_price,
                                        parsedPrice.toRupiah(),
                                        unit.ifBlank { stringResource(R.string.entry_unit_default) },
                                    ),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Medium,
                                )
                            } else if (parsedPrice != null && unit.isNotBlank()) {
                                Text("${parsedPrice.toRupiah()} / $unit")
                            }
                        },
                        onValueChange = { raw ->
                            val clean = sanitizeRupiah(raw)
                            unitPrice = clean
                            isUnitPriceAutoDerived = false
                            val parsedPrice = clean.toLongOrNull()
                            val parsedQty = TransactionCalculation.parseQuantity(quantity)
                            if (parsedQty != null && parsedPrice != null && parsedPrice > 0) {
                                val calculated = TransactionCalculation.computeTotalRupiah(parsedQty, parsedPrice)
                                amount = calculated.toString()
                                isAutoCalculated = true
                            } else if (parsedPrice == null || parsedPrice == 0L) {
                                if (isAutoCalculated) isAutoCalculated = false
                            }
                        },
                    )
                    if (selectedRole == CategoryRole.EXPENSE) {
                        Text(stringResource(R.string.cost_nature_label), style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = costNature == null, onClick = { costNature = null }, label = { Text(stringResource(R.string.cost_nature_inherit)) })
                            FilterChip(selected = costNature == CostNature.FIXED.name, onClick = { costNature = CostNature.FIXED.name }, label = { Text(stringResource(R.string.cost_nature_fixed)) })
                            FilterChip(selected = costNature == CostNature.VARIABLE.name, onClick = { costNature = CostNature.VARIABLE.name }, label = { Text(stringResource(R.string.cost_nature_variable)) })
                        }
                    }
                    if (state.budgetItems.isNotEmpty()) {
                        ChoiceMenu(
                            label = state.budgetItems.firstOrNull { it.id == budgetItemId }?.name
                                ?: stringResource(R.string.entry_link_plan),
                            options = listOf("" to stringResource(R.string.entry_link_none)) + state.budgetItems.map { it.id to it.name },
                            onSelected = { budgetItemId = it.ifBlank { null } },
                        )
                    }
                  }
                }
                localError?.takeIf { it !in inlineErrors }?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
              }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { feedback(FeedbackEvent.TAP); closeSheet() }) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        // Taps dispatched in the same frame all reach here before `enabled` can
                        // recompose, so the handler re-checks what the button shows. Without this
                        // the second tap re-validated a form the first had already cleared and
                        // raised "Jumlah uang harus lebih dari nol" on top of a successful save.
                        if (isSaving || justSaved) return@Button
                        if (state.selectedProjectId == null) {
                            localError = errNoProject
                            return@Button
                        }
                        val parsedAmount = amount.toLongOrNull()
                        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
                        val parsedQuantity = quantity.takeIf(String::isNotBlank)?.let { TransactionCalculation.parseQuantity(it) }
                        val category = compatibleCategories.firstOrNull { it.id == categoryId }
                        localError = when {
                            parsedAmount == null || parsedAmount <= 0 -> errAmount
                            description.isBlank() -> errDescription
                            parsedDate == null -> errDate
                            category == null -> errCategory
                            quantity.isNotBlank() && parsedQuantity == null -> errQuantity
                            selectedRole == CategoryRole.ASSET && (usefulLife.toIntOrNull() ?: 0) <= 0 -> errLife
                            else -> null
                        }
                        if (localError != null) {
                            feedback(FeedbackEvent.REJECT)
                            when (localError) {
                                errAmount -> amountFocus.requestFocus()
                                errDescription -> descriptionFocus.requestFocus()
                            }
                        }
                        if (localError == null && parsedAmount != null && parsedDate != null && category != null) {
                            val reconciled = TransactionCalculation.reconcile(
                                amountRupiah = parsedAmount,
                                quantityText = quantity.ifBlank { null },
                                unitPriceRupiah = unitPrice.toLongOrNull(),
                            )
                            onRecord(
                                CategoryRole.parse(category.role).cashTypeFor(incoming) ?: kind.type,
                                reconciled.totalRupiah,
                                description,
                                category.id,
                                category.name,
                                parsedDate,
                                reference.ifBlank { null },
                                reconciled.quantityText,
                                unit.ifBlank { null },
                                reconciled.unitPriceRupiah,
                                budgetItemId,
                                CostNature.entries.firstOrNull { it.name == costNature }.takeIf { selectedRole == CategoryRole.EXPENSE },
                                productId.takeIf { incoming && selectedRole == CategoryRole.REVENUE },
                                usefulLife.toIntOrNull().takeIf { selectedRole == CategoryRole.ASSET },
                                editingId,
                            )
                            editingId = null
                            amount = ""
                            description = ""
                            reference = ""
                            quantity = ""
                            unit = ""
                            unitPrice = ""
                            budgetItemId = null
                            productId = null
                            costNature = null
                            usefulLife = PlanAssembly.DEFAULT_ASSET_LIFE_MONTHS.toString()
                            isAutoCalculated = false
                            isUnitPriceAutoDerived = false
                            justSaved = true
                            closeSheet()
                        }
                    },
                    // Locked while the write is in flight. The ViewModel refuses a concurrent
                    // second call regardless, so this is the visible half of the guard rather
                    // than the guard itself.
                    enabled = !isSaving && !justSaved,
                    modifier = Modifier.weight(1f).height(52.dp).tourTarget(TourTarget.ENTRY_SAVE),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        stringResource(if (isSaving) R.string.entry_saving else R.string.entry_save),
                        fontWeight = FontWeight.Bold,
                    )
                }
              }
              Spacer(Modifier.height(4.dp))
            }
            SheetTour(formTour)
          }
        }
    }
}


