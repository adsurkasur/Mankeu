package com.mankeu.presentation

import com.mankeu.data.PlanAssembly
import com.mankeu.data.ProductDraft
import com.mankeu.data.ProductRecord
import com.mankeu.domain.ActualBepCalculator
import com.mankeu.domain.ActualMonthAnalysis
import com.mankeu.domain.CostNature
import com.mankeu.domain.HppChange
import com.mankeu.domain.ProductCosting
import com.mankeu.domain.PlanAnalysis
import com.mankeu.domain.PlanCalculator
import com.mankeu.domain.Recurrence
import java.time.YearMonth
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.BackupPreview
import com.mankeu.backup.PreparedRestore
import com.mankeu.data.BookEntity
import com.mankeu.data.BudgetItemDraft
import com.mankeu.data.BudgetItemEntity
import com.mankeu.data.BusinessEntity
import com.mankeu.data.CategoryEntity
import com.mankeu.data.EventDraft
import com.mankeu.data.FinanceRepository
import com.mankeu.data.ImportReview
import com.mankeu.data.ImportNoticeEntity
import com.mankeu.data.ImportBatchEntity
import com.mankeu.data.ProjectEntity
import com.mankeu.data.ScopeRemoval
import com.mankeu.data.WorkspaceSelection
import com.mankeu.data.CategoryTotalRow
import com.mankeu.domain.StockItem
import com.mankeu.domain.StockMovementType
import com.mankeu.domain.CashFlowRecord
import com.mankeu.domain.BookMode
import com.mankeu.domain.sha256Hex
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.FinancialSummary
import com.mankeu.domain.MoneyEvent
import com.mankeu.domain.PostingReportCalculator
import com.mankeu.domain.TransactionCalculation
import com.mankeu.importer.FinanceWorkbookImporter
import com.mankeu.importer.WorkbookImportPreview
import com.mankeu.R
import com.mankeu.auth.GoogleIdentity
import com.mankeu.auth.AuthUser
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.sync.SyncController
import com.mankeu.sync.SyncPassphraseLost
import com.mankeu.sync.WrongSyncPassphrase
import com.mankeu.backup.cloud.DriveBackupFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

private val EMPTY_SUMMARY = PostingReportCalculator.calculate(emptyList())

data class AppUiState(
    val businesses: List<BusinessEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val books: List<BookEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val budgetItems: List<BudgetItemEntity> = emptyList(),
    val selectedBusinessId: String? = null,
    val selectedProjectId: String? = null,
    val selectedBookMode: BookMode = BookMode.REALIZATION,
    val selectedBookId: String? = null,
    /** The newest slice of the ledger, for display. Never the basis of a reported total. */
    val events: List<MoneyEvent> = emptyList(),
    /** How many rows the book really holds, regardless of how many [events] carries. */
    val eventCount: Int = 0,
    /** Narrow rows behind the cash-flow schedule, and category totals summed by the database. */
    val cashFlowRecords: List<CashFlowRecord> = emptyList(),
    val categoryTotals: List<CategoryTotalRow> = emptyList(),
    val stockItems: List<StockItem> = emptyList(),
    val summary: FinancialSummary = EMPTY_SUMMARY,
    val projectionSummary: FinancialSummary = EMPTY_SUMMARY,
    val realizationSummary: FinancialSummary = EMPTY_SUMMARY,
    /** Distinct months with Realisasi records; the comparison averages over it. */
    val realizationMonths: Int = 1,
    /** The selected unit's products with recipes, its Proyeksi plan lines and their analysis (DEC-014). */
    val products: List<ProductRecord> = emptyList(),
    val planItems: List<BudgetItemEntity> = emptyList(),
    val plan: PlanAnalysis? = null,
    /** "Status BEP bulan ini" from the Realisasi book; null until there is anything to say. */
    val actualMonth: ActualMonthAnalysis? = null,
    val notices: List<ImportNoticeEntity> = emptyList(),
    val importBatches: List<ImportBatchEntity> = emptyList(),
    /**
     * True only for the initial placeholder emitted before the first database read completes.
     * Screens use it to show skeleton placeholders instead of briefly rendering a real-looking
     * but empty state (which would read as "you have no records" during a cold start).
     */
    val isLoading: Boolean = true,
    /** Set after a purchase changed a material's price: what that does to each product's HPP (spec #20). */
    val priceAlert: PriceAlert? = null,
) {
    /** True when the unit has a business plan worth showing instead of posted projection rows. */
    val hasPlan: Boolean get() = products.isNotEmpty() || planItems.isNotEmpty()
}

/** A material's price moved from [oldPriceRupiah] to [newPriceRupiah] per [unit]; [changes] are the products it touches. */
data class PriceAlert(
    val materialName: String,
    val unit: String,
    val oldPriceRupiah: Long,
    val newPriceRupiah: Long,
    val changes: List<HppChange>,
)

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Loading : ImportUiState
    /** Writing the batch: one transaction, so it cannot be cancelled halfway and offers no Batal. */
    data class Committing(val rows: Int) : ImportUiState
    data class Ready(val preview: WorkbookImportPreview, val review: ImportReview) : ImportUiState
    data class Committed(val count: Int) : ImportUiState
    data class Failure(val message: UiText) : ImportUiState
}

sealed interface TransferUiState {
    data object Idle : TransferUiState
    data class Working(val label: UiText) : TransferUiState
    data class Success(val message: UiText) : TransferUiState
    data class Failure(val message: UiText) : TransferUiState
}

sealed interface RestoreUiState {
    data object Idle : RestoreUiState
    data class Ready(val preview: BackupPreview) : RestoreUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val repository: FinanceRepository,
    private val importer: FinanceWorkbookImporter,
    private val backupManager: EncryptedBackupManager,
    private val identity: GoogleIdentity,
    private val cloudBackupClient: GoogleDriveBackupClient,
    /** The usaha and unit open when the app was last closed, and where to save each new choice. */
    private val rememberedSelection: WorkspaceSelection? = null,
    private val onSelectionChanged: (WorkspaceSelection) -> Unit = {},
    /** Null in tests and screenshot walks: nothing syncs there. */
    private val syncController: SyncController? = null,
) : ViewModel() {
    private val selectedBusinessId = MutableStateFlow<String?>(null)
    private val selectedProjectId = MutableStateFlow<String?>(null)
    private val selectedBookMode = MutableStateFlow(BookMode.REALIZATION)

    val authUser: StateFlow<AuthUser?> = identity.currentUser
    val syncState: StateFlow<SyncController.State> = syncController?.state ?: MutableStateFlow(SyncController.State.Off)
    private var syncJob: Job? = null
    val driveBackups = MutableStateFlow<List<DriveBackupFile>>(emptyList())
    val isDriveLoading = MutableStateFlow(false)

    val importState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val transferState = MutableStateFlow<TransferUiState>(TransferUiState.Idle)
    val restoreState = MutableStateFlow<RestoreUiState>(RestoreUiState.Idle)
    val recoveryAvailable = MutableStateFlow(backupManager.hasLatestRecovery())
    val message = MutableStateFlow<UiText?>(null)
    private val priceAlert = MutableStateFlow<PriceAlert?>(null)
    fun dismissPriceAlert() { priceAlert.value = null }
    /**
     * True while a record-changing operation is in flight.
     *
     * Every mutation was a bare `viewModelScope.launch`, so the UI had no way to know one was
     * running and every save button stayed live through it. On a slow phone the write takes long
     * enough to tap three times, and each tap inserted its own transaction — three identical rows
     * in the ledger, three journal entries, a cash balance wrong by twice the amount.
     */
    val isSaving = MutableStateFlow(false)

    private var preparedRestore: PreparedRestore? = null
    private var recoveryPassword: CharArray? = null

    /**
     * The sharing policy for the per-book query flows.
     *
     * `Eagerly` kept all nineteen flows collecting from the moment the ViewModel was built until
     * the process died — so a book sat open in the background re-ran its queries on every write,
     * draining battery for a screen nobody was looking at. `WhileSubscribed` lets them stop five
     * seconds after the last collector leaves, which covers a configuration change without tearing
     * anything down.
     *
     * Applied to the fourteen flows that are genuinely per-book reads. The four selection flows
     * keep `Eagerly` for the reason given where they are declared.
     *
     * The five-second timeout is the visible half. The half that matters for correctness is
     * `replayExpirationMillis`, which defaults to `Long.MAX_VALUE`: the cached value is *never*
     * reset to the initial one when a flow goes cold. `record`, `savePlanLine` and
     * `saveProduct` read `selectedBookId.value` and `uiState.value` directly, and a reset
     * cache would hand them a null book id and silently drop the user's transaction.
     */
    private val KeepWarm = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000)

    // ---- Selection chain: stays eager, deliberately. ------------------------------------------
    //
    // These four decide *which* book every flow below reads, and two of them are collected
    // permanently by `init` to reconcile the selection — so they can never go cold anyway and
    // `WhileSubscribed` would save nothing on them.
    //
    // It would also cost correctness. Each `stateIn` carries a placeholder initial value, and the
    // reconcilers in `init` cannot tell a placeholder empty list from a database that really is
    // empty. Made lazy, the placeholder is consumed at subscribe time — during startup — and the
    // reconciler nulls the selection out from under a workspace that `currentWorkspace()` had
    // already resolved. Observed on device: a cold start rendered Rp0 across a book holding
    // Rp27.050.000, and only a second launch showed the real figures. A book that looks empty is
    // the most alarming thing this app can show, so the selection chain is not the place to
    // economise (D-071).
    private val businesses = repository.businesses.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val projects = selectedBusinessId.flatMapLatest { id ->
        id?.let(repository::projects) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val books = selectedProjectId.flatMapLatest { id ->
        id?.let(repository::books) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val selectedBookId = combine(books, selectedBookMode) { items, mode ->
        items.firstOrNull { it.mode == mode.name }?.id
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ---- Everything below is a per-book live query, and does stop when nobody is looking. ------
    private val categories = selectedBusinessId.flatMapLatest { id ->
        id?.let(repository::categories) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, KeepWarm, emptyList())
    private val events = selectedBookId.flatMapLatest { id ->
        id?.let(repository::eventsForBook) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, KeepWarm, emptyList())
    private val eventCount = selectedBookId.flatMapLatest { id ->
        id?.let(repository::eventCount) ?: flowOf(0)
    }.stateIn(viewModelScope, KeepWarm, 0)
    private val cashFlowRecords = selectedBookId.flatMapLatest { id ->
        id?.let(repository::cashFlowRecords) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, KeepWarm, emptyList())
    private val categoryTotals = selectedBookId.flatMapLatest { id ->
        id?.let(repository::categoryTotals) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, KeepWarm, emptyList())
    private val budgetItems = selectedBookId.flatMapLatest { id ->
        id?.let(repository::budgetItems) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, KeepWarm, emptyList())
    private val stockItems = combine(selectedBusinessId, selectedProjectId) { business, project -> business to project }
        .flatMapLatest { (business, project) -> business?.let { repository.stockItems(it, project) } ?: flowOf(emptyList()) }.stateIn(viewModelScope, KeepWarm, emptyList())
    private val summary = selectedBookId.flatMapLatest { id ->
        id?.let(repository::accountingSummary) ?: flowOf(EMPTY_SUMMARY)
    }.stateIn(viewModelScope, KeepWarm, EMPTY_SUMMARY)
    private val products = selectedProjectId.flatMapLatest { id ->
        id?.let(repository::products) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, KeepWarm, emptyList())
    private val planItems = books.flatMapLatest { items ->
        items.firstOrNull { it.mode == BookMode.PROJECTION.name }?.id?.let(repository::budgetItems) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, KeepWarm, emptyList())
    private val actualCostRows = books.flatMapLatest { items ->
        items.firstOrNull { it.mode == BookMode.REALIZATION.name }?.id?.let(repository::costRows) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, KeepWarm, emptyList())

    /** Products with the material prices their HPP is costed at (Task 3 derives prices from purchases). */
    private val pricedProducts = combine(products, stockItems) { productRows, items ->
        productRows to items.associate { it.id to it.basePriceRupiah }
    }

    /** The unit's business plan and this month's actual BEP, recomputed whenever any input changes. */
    private val planContent = combine(projects, selectedProjectId, pricedProducts, planItems, categories) { projectRows, projectId, (productRows, prices), lines, categoryRows ->
        val project = projectRows.firstOrNull { it.id == projectId }
        val plan = if (productRows.isEmpty() && lines.isEmpty()) null
        else PlanCalculator.analyse(PlanAssembly.planInputs(project, productRows, lines, categoryRows, LocalDate.now(), prices))
        Triple(productRows to prices, lines, plan)
    }
    private val planAndActual = combine(planContent, actualCostRows) { (priced, lines, plan), rows ->
        val (productRows, prices) = priced
        val actual = if (rows.isEmpty()) null
        else ActualBepCalculator.analyse(PlanAssembly.products(productRows, prices), PlanAssembly.actualMonth(YearMonth.now(), rows, productRows))
        PlanContent(productRows, lines, plan, actual)
    }.stateIn(viewModelScope, KeepWarm, PlanContent(emptyList(), emptyList(), null, null))
    private val importBatches = selectedProjectId.flatMapLatest { id ->
        id?.let(repository::importBatches) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, KeepWarm, emptyList())

    private val projectionSummary = books.flatMapLatest { items ->
        items.firstOrNull { it.mode == BookMode.PROJECTION.name }?.id
            ?.let(repository::accountingSummary) ?: flowOf(EMPTY_SUMMARY)
    }.stateIn(viewModelScope, KeepWarm, EMPTY_SUMMARY)
    private val realizationSummary = books.flatMapLatest { items ->
        items.firstOrNull { it.mode == BookMode.REALIZATION.name }?.id
            ?.let(repository::accountingSummary) ?: flowOf(EMPTY_SUMMARY)
    }.stateIn(viewModelScope, KeepWarm, EMPTY_SUMMARY)
    private val realizationMonths = actualCostRows.map { rows ->
        rows.map { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) }.distinct().size.coerceAtLeast(1)
    }

    private val scopeContent = combine(businesses, projects, books, categories) {
            businessItems, projectItems, bookItems, categoryItems ->
        ScopeContent(businessItems, projectItems, bookItems, categoryItems)
    }
    private val financeContent = combine(events, budgetItems, stockItems, summary, planAndActual) {
            eventItems, budgetItemItems, stockItemItems, calculatedSummary, planState ->
        FinanceContent(eventItems, budgetItemItems, stockItemItems, calculatedSummary, planState)
    }
    // `combine` only has typed overloads up to five flows, and the top-level state needs six
    // groups, so the ledger aggregates ride along with the finance content rather than pushing the
    // whole thing onto the untyped vararg overload.
    private val ledgerAggregates = combine(eventCount, cashFlowRecords, categoryTotals) { count, records, totals ->
        LedgerAggregates(count, records, totals)
    }
    private val financeAndAggregates = combine(financeContent, ledgerAggregates) { finance, aggregates ->
        finance to aggregates
    }
    private val selections = combine(selectedBusinessId, selectedProjectId, selectedBookMode, selectedBookId) {
            businessId, projectId, mode, bookId ->
        SelectionContent(businessId, projectId, mode, bookId)
    }
    private val comparison = combine(projectionSummary, realizationSummary, realizationMonths) { projection, realization, months ->
        ComparisonContent(projection, realization, months)
    }
    // Import notices belong to the unit whose batch made them; system notices (migrations, no
    // batch) concern the whole device. Unfiltered, one unit's import showed on every unit's Beranda.
    private val importAudit = combine(repository.notices, importBatches) { notices, batches ->
        val ids = batches.map { it.id }.toSet()
        ImportAudit(notices.filter { it.batchId == null || it.batchId in ids }, batches)
    }

    val uiState = combine(scopeContent, financeAndAggregates, importAudit, selections, comparison) {
            scope, financePair, audit, selection, comparison ->
        val (finance, aggregates) = financePair
        AppUiState(
            businesses = scope.businesses,
            projects = scope.projects,
            books = scope.books,
            categories = scope.categories,
            budgetItems = finance.budgetItems,
            selectedBusinessId = selection.businessId,
            selectedProjectId = selection.projectId,
            selectedBookMode = selection.bookMode,
            selectedBookId = selection.bookId,
            events = finance.events,
            eventCount = aggregates.eventCount,
            cashFlowRecords = aggregates.cashFlowRecords,
            categoryTotals = aggregates.categoryTotals,
            stockItems = finance.stockItems,
            summary = finance.summary,
            realizationSummary = comparison.realization,
            realizationMonths = comparison.realizationMonths,
            products = finance.plan.products,
            planItems = finance.plan.planItems,
            plan = finance.plan.plan,
            actualMonth = finance.plan.actualMonth,
            // A unit with a plan reports Proyeksi from the plan, on every screen (F-04).
            projectionSummary = finance.plan.plan?.summary ?: comparison.projection,
            notices = audit.notices,
            importBatches = audit.batches,
            // Any emission from this combine means the underlying flows have produced real data.
            isLoading = false,
        )
    }.combine(priceAlert) { state, alert -> state.copy(priceAlert = alert) }
        .stateIn(viewModelScope, KeepWarm, AppUiState(isLoading = true))

    init {
        syncController?.let { sync -> viewModelScope.launch { sync.refresh(); scheduleSync(delayMillis = 0) } }
        // A preview was reviewed against the unit it was opened in (existing products, duplicate
        // file); it must not be committed into another one.
        viewModelScope.launch { selectedProjectId.drop(1).collect { importState.value = ImportUiState.Idle } }
        viewModelScope.launch {
            // Reads only. A first run now legitimately resolves to (null, null) and the shell
            // asks the owner to name their own usaha and unit, instead of the app writing
            // "Usaha Saya"/"Umum" behind their back and presenting it as their data.
            val selection = repository.currentWorkspace(rememberedSelection)
            selectedBusinessId.value = selection.businessId
            selectedProjectId.value = selection.projectId
            // Saved only after the remembered one is restored, so startup's (null, null) never overwrites it.
            combine(selectedBusinessId, selectedProjectId, ::WorkspaceSelection).drop(1).collect(onSelectionChanged)
        }
        viewModelScope.launch {
            businesses.collect { items ->
                if (items.isNotEmpty() && items.none { it.id == selectedBusinessId.value }) {
                    selectedBusinessId.value = items.first().id
                } else if (items.isEmpty()) {
                    selectedBusinessId.value = null
                }
            }
        }
        viewModelScope.launch {
            projects.collect { items ->
                if (items.isNotEmpty() && items.none { it.id == selectedProjectId.value }) {
                    selectedProjectId.value = items.first().id
                } else if (items.isEmpty()) {
                    selectedProjectId.value = null
                }
            }
        }
    }

    fun selectBusiness(id: String) {
        selectedProjectId.value = null
        selectedBusinessId.value = id
    }

    fun selectProject(id: String) {
        selectedProjectId.value = id
    }

    fun selectBookMode(mode: BookMode) {
        selectedBookMode.value = mode
    }

    /**
     * Runs a record-changing block with [isSaving] held, and drops the call entirely if one is
     * already running.
     *
     * Disabling the button is the visible half and it is not sufficient on its own: two taps
     * inside one frame both dispatch before any recomposition can disable anything. The
     * `compareAndSet` is what actually makes a double tap impossible; the flag is what makes it
     * look deliberate.
     */
    private fun mutating(block: suspend () -> Unit) = viewModelScope.launch {
        if (!isSaving.compareAndSet(expect = false, update = true)) return@launch
        try {
            block()
        } finally {
            isSaving.value = false
        }
        scheduleSync()
    }

    /**
     * Sync a few seconds after the last change (DEC-024), so a burst of records goes in one round.
     * Quiet: a failure shows in the sync card in Pengaturan, not as a message over the owner's work.
     */
    private fun scheduleSync(delayMillis: Long = 5_000) {
        val sync = syncController ?: return
        // Only the wait is restarted. The round itself runs in its own job, so a record saved while
        // a round is uploading never cancels it halfway (review 2026-10-22).
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            delay(delayMillis)
            viewModelScope.launch { runCatching { sync.syncNow(authUser.value?.email) } }
        }
    }

    fun syncNow() = viewModelScope.launch {
        val sync = syncController ?: return@launch
        runCatching { sync.syncNow(authUser.value?.email) }
            .onSuccess { report -> report?.let { message.value = UiText.of(R.string.msg_sync_done, it.applied) } }
            .onFailure { message.value = syncFailure(it) }
    }

    fun enableSync(passphrase: CharArray) = viewModelScope.launch {
        val sync = syncController ?: return@launch
        // A failed check switches nothing on. Once on, the first round runs like any other: a
        // network blip there leaves sync on and shows in the card, the passphrase is kept.
        runCatching { sync.enable(passphrase, authUser.value?.email) }
            .onSuccess { message.value = UiText.of(R.string.msg_sync_on); scheduleSync(delayMillis = 0) }
            .onFailure { message.value = syncFailure(it) }
        passphrase.fill(' ')
    }

    private fun syncFailure(error: Throwable): UiText = when (error) {
        is WrongSyncPassphrase -> UiText.of(R.string.sync_wrong_passphrase)
        is SyncPassphraseLost -> UiText.of(R.string.sync_passphrase_lost)
        else -> UiText.fromError(error, R.string.msg_sync_failed)
    }

    fun disableSync() = viewModelScope.launch {
        syncController?.disable()
        message.value = UiText.of(R.string.msg_sync_off)
    }

    /**
     * A new business starts with one unit, [firstUnitName], already selected: most owners have one
     * place, and making them create "a unit" before the first record was a step they did not
     * understand (owner decision after the audit walk, 2026-10-22). Branches are added later.
     */
    fun createBusiness(name: String, firstUnitName: String, onCreated: () -> Unit = {}) = mutating {
        runCatching { repository.createBusinessWithUnit(name, firstUnitName) }
            .onSuccess { (businessId, projectId) ->
                selectedBusinessId.value = businessId
                selectedProjectId.value = projectId
                message.value = UiText.of(R.string.msg_business_created)
                onCreated()
            }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_business_create_failed) }
    }

    fun createProject(name: String) = mutating {
        val businessId = selectedBusinessId.value ?: return@mutating
        runCatching { repository.createProject(businessId, name) }
            .onSuccess { projectId ->
                selectedProjectId.value = projectId
                message.value = UiText.of(R.string.msg_project_created)
            }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_project_create_failed) }
    }

    fun renameBusiness(name: String) = mutating {
        val id = selectedBusinessId.value ?: return@mutating
        runCatching { repository.renameBusiness(id, name) }
            .onSuccess { message.value = UiText.of(R.string.msg_business_renamed) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_business_rename_failed) }
    }

    fun renameProject(name: String) = mutating {
        val id = selectedProjectId.value ?: return@mutating
        runCatching { repository.renameProject(id, name) }
            .onSuccess { message.value = UiText.of(R.string.msg_project_renamed) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_project_rename_failed) }
    }

    fun updateProjectDetails(
        id: String,
        name: String,
        description: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ) = mutating {
        runCatching { repository.updateProjectDetails(id, name, description, startDate, endDate) }
            .onSuccess { message.value = UiText.of(R.string.msg_project_updated) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_project_update_failed) }
    }

    /**
     * Archiving the active scope leaves nothing selected; the `businesses`/`projects` collectors in
     * `init` notice the selection has gone and fall back to the first remaining row, or to null when
     * there is none — which is what puts the explicit "Tambah Usaha"/"Tambah Unit Usaha" state back on
     * screen rather than leaving a dangling id.
     */
    fun removeBusiness(id: String) = mutating {
        runCatching { repository.removeBusiness(id) }
            .onSuccess { outcome ->
                if (selectedBusinessId.value == id) selectedBusinessId.value = null
                message.value = UiText.of(
                    if (outcome == ScopeRemoval.DELETED) R.string.msg_business_deleted
                    else R.string.msg_business_archived,
                )
            }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_business_archive_failed) }
    }

    fun removeProject(id: String) = mutating {
        runCatching { repository.removeProject(id) }
            .onSuccess { outcome ->
                if (selectedProjectId.value == id) selectedProjectId.value = null
                message.value = UiText.of(
                    if (outcome == ScopeRemoval.DELETED) R.string.msg_project_deleted
                    else R.string.msg_project_archived,
                )
            }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_project_archive_failed) }
    }

    fun createCategory(name: String, role: CategoryRole) = mutating {
        val businessId = selectedBusinessId.value ?: return@mutating
        runCatching { repository.createCategory(businessId, name, role, aliases = listOf(name)) }
            .onSuccess { message.value = UiText.of(R.string.msg_category_added, name) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_category_add_failed) }
    }

    fun archiveCategory(id: String) = mutating {
        runCatching { repository.archiveCategory(id) }
            .onSuccess { message.value = UiText.of(R.string.msg_category_archived) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_category_archive_failed) }
    }

    fun record(
        type: FinancialEventType,
        amountRupiah: Long,
        description: String,
        categoryId: String?,
        category: String,
        date: LocalDate,
        reference: String? = null,
        quantityText: String? = null,
        unit: String? = null,
        unitPriceRupiah: Long? = null,
        budgetItemId: String? = null,
        costNature: CostNature? = null,
        productId: String? = null,
        /** The material (bahan) a purchase buys; its new unit price may move product costs. */
        stockItemId: String? = null,
        usefulLifeMonths: Int? = null,
        /** Set when the form was opened with Ubah: the record is corrected in place (DEC-019). */
        editingEventId: String? = null,
    ) = mutating {
        val state = uiState.value
        val businessId = state.selectedBusinessId
        val projectId = state.selectedProjectId
        val bookId = state.selectedBookId
        if (businessId == null || projectId == null || bookId == null) {
            message.value = UiText.of(R.string.entry_err_no_project)
            return@mutating
        }
        val material = stockItemId?.let { id -> stockItems.value.firstOrNull { it.id == id } }
        val oldPrices = stockItems.value.associate { it.id to it.basePriceRupiah }
        runCatching {
            val reconciled = TransactionCalculation.reconcile(
                amountRupiah = amountRupiah,
                quantityText = quantityText,
                unitPriceRupiah = unitPriceRupiah,
            )
            val draft =
                EventDraft(
                    businessId = businessId,
                    projectId = projectId,
                    bookId = bookId,
                    type = type,
                    amountRupiah = reconciled.totalRupiah,
                    date = date,
                    description = description,
                    category = category,
                    categoryId = categoryId,
                    reference = reference,
                    quantityText = reconciled.quantityText,
                    unit = unit,
                    unitPriceRupiah = reconciled.unitPriceRupiah,
                    budgetItemId = budgetItemId,
                    costNature = costNature,
                    productId = productId,
                    stockItemId = stockItemId,
                    usefulLifeMonths = usefulLifeMonths,
                )
            if (editingEventId != null) repository.updateEvent(editingEventId, draft) else repository.recordEvent(draft)
        }.onSuccess {
            material?.let { m ->
                val newPrice = repository.materialPrice(m.id) ?: return@let
                val changes = ProductCosting.priceImpact(products.value.map { it.spec }, oldPrices, m.id, newPrice)
                if (newPrice != m.basePriceRupiah && changes.isNotEmpty()) {
                    priceAlert.value = PriceAlert(m.name, m.unit, m.basePriceRupiah, newPrice, changes)
                }
            }
            message.value = UiText.of(if (editingEventId != null) R.string.msg_event_updated else R.string.msg_event_saved)
        }.onFailure {
            message.value = UiText.fromError(it, R.string.msg_event_save_failed)
        }
    }

    fun deleteTransaction(eventId: String) = mutating {
        runCatching {
            repository.deleteEvent(eventId)
        }.onSuccess {
            message.value = UiText.of(R.string.msg_event_deleted)
        }.onFailure {
            message.value = UiText.fromError(it, R.string.msg_event_save_failed)
        }
    }

    /**
     * Creates ([id] null) or updates a Proyeksi plan line: a fixed cost, a one-off cost or income,
     * an asset or planned capital (DEC-014). Always the Proyeksi book, whatever mode is showing.
     */
    fun savePlanLine(
        id: String?,
        categoryId: String,
        name: String,
        amountRupiah: Long,
        recurrence: Recurrence,
        costNature: CostNature?,
        month: YearMonth?,
        usefulLifeMonths: Int?,
    ) = mutating {
        val planBookId = books.value.firstOrNull { it.mode == BookMode.PROJECTION.name }?.id ?: return@mutating
        val draft = BudgetItemDraft(
            bookId = planBookId,
            categoryId = categoryId,
            name = name,
            plannedTotalRupiah = amountRupiah,
            plannedDate = month?.atDay(1),
            aliases = listOf(name),
            recurrence = recurrence,
            costNature = costNature,
            usefulLifeMonths = usefulLifeMonths,
        )
        runCatching { if (id == null) repository.createBudgetItem(draft) else repository.updateBudgetItem(id, draft) }
            .onSuccess { message.value = UiText.of(R.string.msg_plan_saved) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_plan_save_failed) }
    }

    fun archivePlanLine(id: String) = mutating {
        runCatching { repository.archiveBudgetItem(id) }
            .onSuccess { message.value = UiText.of(R.string.msg_plan_removed) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_plan_save_failed) }
    }

    /** Creates ([id] null) or replaces a product; [onSuccess] runs only once it is stored, so a refused form stays open. */
    fun saveProduct(id: String?, draft: ProductDraft, onSuccess: () -> Unit = {}) = mutating {
        val projectId = selectedProjectId.value ?: return@mutating
        runCatching { repository.saveProduct(projectId, id, draft) }
            .onSuccess {
                message.value = UiText.of(R.string.msg_product_saved, draft.name.trim())
                onSuccess()
            }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_plan_save_failed) }
    }

    fun recordProduction(productId: String, batches: BigDecimal, date: LocalDate) = mutating {
        runCatching { repository.recordProduction(productId, batches, date, "") }
            .onSuccess { message.value = UiText.of(R.string.msg_production_saved) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_plan_save_failed) }
    }

    fun countProductStock(productId: String, counted: BigDecimal, date: LocalDate) = mutating {
        runCatching { repository.countProductStock(productId, counted, date, "") }
            .onSuccess { message.value = UiText.of(R.string.msg_stock_count_saved) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_plan_save_failed) }
    }

    fun deleteProductMovement(id: String) = mutating {
        runCatching { repository.deleteProductMovement(id) }
            .onSuccess { message.value = UiText.of(R.string.msg_stock_entry_deleted) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_stock_entry_delete_failed) }
    }

    fun archiveProduct(id: String) = mutating {
        runCatching { repository.archiveProduct(id) }
            .onSuccess { message.value = UiText.of(R.string.msg_plan_removed) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_plan_save_failed) }
    }

    fun setCategoryCostNature(id: String, nature: CostNature?) = mutating {
        runCatching { repository.setCategoryCostNature(id, nature) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_category_add_failed) }
    }

    /** The plan period is the unit's start and end month (DEC-014 §3). */
    fun setPlanPeriod(start: YearMonth, end: YearMonth) = mutating {
        val project = projects.value.firstOrNull { it.id == selectedProjectId.value } ?: return@mutating
        runCatching { repository.updateProjectDetails(project.id, project.name, project.description, start.atDay(1), end.atEndOfMonth()) }
            .onSuccess { message.value = UiText.of(R.string.msg_plan_saved) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_plan_save_failed) }
    }

    fun createStockItem(
        name: String,
        unit: String,
        basePriceRupiah: Long,
        initialStock: BigDecimal,
        minAlertStock: BigDecimal,
        shared: Boolean = false,
    ) = mutating {
        val businessId = selectedBusinessId.value ?: return@mutating
        runCatching { repository.createStockItem(businessId, selectedProjectId.value, shared, name, unit, basePriceRupiah, initialStock, minAlertStock) }
            .onSuccess { message.value = UiText.of(R.string.msg_stock_item_added, name.trim()) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_stock_item_add_failed) }
    }

    fun recordStockMovement(itemId: String, type: StockMovementType, quantity: BigDecimal, notes: String) = mutating {
        runCatching { repository.recordStockMovement(itemId, type, quantity, LocalDate.now(), notes) }
            .onSuccess { message.value = UiText.of(R.string.msg_stock_movement_saved) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_stock_movement_failed) }
    }

    /** The running preview, so Batal really stops it instead of being overwritten by its result. */
    private var previewJob: Job? = null

    fun previewWorkbook(contentResolver: ContentResolver, uri: Uri) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch { preview(contentResolver, uri) }
    }

    private suspend fun preview(contentResolver: ContentResolver, uri: Uri) {
        importState.value = ImportUiState.Loading
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: "workbook.xlsx"
                val bytes = contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes()
                } ?: error("File tidak dapat dibuka.")
                require(bytes.isNotEmpty()) { "File Excel kosong (0 byte)." }
                require(bytes.size <= 25 * 1024 * 1024) { "Ukuran workbook melebihi batas aman 25 MB." }
                val preview = importer.preview(name, bytes)
                // Bulk entry into the selected unit (DEC-012): nothing to review without one.
                val businessId = selectedBusinessId.value
                if (preview.errors.isNotEmpty() || businessId == null || selectedProjectId.value == null) {
                    preview to null
                } else {
                    preview to repository.reviewImport(businessId, selectedProjectId.value!!, preview)
                }
            }
        }
        // runCatching also catches this job's cancellation; a superseded or cancelled preview must
        // not write its "failure" over the newer one (review 2026-10-22).
        currentCoroutineContext().ensureActive()
        if (importState.value != ImportUiState.Loading) return
        importState.value = result.fold(
            onSuccess = { (preview, review) ->
                when {
                    preview.errors.isNotEmpty() -> ImportUiState.Failure(UiText.Raw(preview.errors.take(20).joinToString("\n")))
                    review == null -> ImportUiState.Failure(UiText.of(R.string.import_need_unit))
                    else -> ImportUiState.Ready(preview, review)
                }
            },
            onFailure = { ImportUiState.Failure(UiText.fromError(it, R.string.msg_import_failed)) },
        )
    }

    fun commitImport() = mutating {
        val preview = (importState.value as? ImportUiState.Ready)?.preview ?: return@mutating
        val businessId = selectedBusinessId.value ?: return@mutating
        val projectId = selectedProjectId.value ?: return@mutating
        importState.value = ImportUiState.Committing(preview.rowCount)
        val result = runCatching { withContext(Dispatchers.IO) { repository.commitImport(businessId, projectId, preview) } }
        importState.value = result.fold(
            onSuccess = { ImportUiState.Committed(it) },
            onFailure = { ImportUiState.Failure(UiText.fromError(it, R.string.msg_import_commit_failed)) },
        )
    }

    fun resetImportState() {
        if (importState.value is ImportUiState.Committing) return
        previewJob?.cancel()
        importState.value = ImportUiState.Idle
    }

    fun undoImportBatch(id: String) = mutating {
        runCatching { repository.undoImportBatch(id) }
            .onSuccess { result ->
                message.value = if (result.kept.isEmpty()) {
                    UiText.of(if (result.periodCleared) R.string.msg_batch_undone_period else R.string.msg_batch_undone, result.records)
                } else {
                    UiText.of(if (result.periodCleared) R.string.msg_batch_undone_period_kept else R.string.msg_batch_undone_kept, result.records, result.kept.joinToString())
                }
            }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_batch_undo_failed) }
    }

    fun exportWorkbook(contentResolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        val projectId = selectedProjectId.value ?: return@launch
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_building_workbook))
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val bytes = repository.exportProjectWorkbook(projectId)
                contentResolver.openOutputStream(uri, "w")?.use { output ->
                    output.write(bytes)
                    output.flush()
                } ?: error("Tujuan ekspor tidak dapat dibuka.")
                bytes.size
            }
        }
        transferState.value = result.fold(
            onSuccess = { TransferUiState.Success(UiText.of(R.string.msg_export_xlsx_ok, it / 1024)) },
            onFailure = { TransferUiState.Failure(UiText.fromError(it, R.string.msg_export_xlsx_failed)) },
        )
    }

    fun exportTemplate(contentResolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_building_workbook))
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val bytes = repository.exportTemplate(selectedBusinessId.value)
                contentResolver.openOutputStream(uri, "w")?.use { output ->
                    output.write(bytes)
                    output.flush()
                } ?: error("Tujuan ekspor tidak dapat dibuka.")
                bytes.size
            }
        }
        transferState.value = result.fold(
            onSuccess = { TransferUiState.Success(UiText.of(R.string.msg_template_export_ok, it / 1024)) },
            onFailure = { TransferUiState.Failure(UiText.fromError(it, R.string.msg_export_xlsx_failed)) },
        )
    }

    fun exportPdf(contentResolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        val projectId = selectedProjectId.value ?: return@launch
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_building_pdf))
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val bytes = repository.exportProjectPdf(projectId)
                contentResolver.openOutputStream(uri, "w")?.use { output ->
                    output.write(bytes)
                    output.flush()
                } ?: error("Tujuan PDF tidak dapat dibuka.")
                bytes.size
            }
        }
        transferState.value = result.fold(
            onSuccess = { TransferUiState.Success(UiText.of(R.string.msg_export_pdf_ok, it / 1024)) },
            onFailure = { TransferUiState.Failure(UiText.fromError(it, R.string.msg_export_pdf_failed)) },
        )
    }

    fun createEncryptedBackup(contentResolver: ContentResolver, uri: Uri, password: CharArray) = viewModelScope.launch {
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_encrypting_backup))
        val result = runCatching {
            withContext(Dispatchers.IO) {
                try {
                    val bytes = backupManager.create(password)
                    contentResolver.openOutputStream(uri, "w")?.use { output ->
                        output.write(bytes)
                        output.flush()
                    } ?: error("Tujuan cadangan tidak dapat dibuka.")
                    bytes.size
                } finally {
                    password.fill('\u0000')
                }
            }
        }
        transferState.value = result.fold(
            onSuccess = { TransferUiState.Success(UiText.of(R.string.msg_backup_ok, it / 1024)) },
            onFailure = { TransferUiState.Failure(UiText.fromError(it, R.string.msg_backup_failed)) },
        )
    }

    fun prepareEncryptedBackupRestore(contentResolver: ContentResolver, uri: Uri, password: CharArray) = viewModelScope.launch {
        cancelPreparedRestore()
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_verifying_backup))
        val retainedPassword = password.copyOf()
        val result = runCatching {
            withContext(Dispatchers.IO) {
                try {
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Berkas cadangan tidak dapat dibuka.")
                    require(bytes.size <= 160 * 1024 * 1024) { "Berkas cadangan melebihi batas aman 160 MB." }
                    backupManager.prepareRestore(bytes, password)
                } finally {
                    password.fill('\u0000')
                }
            }
        }
        result.fold(
            onSuccess = { prepared ->
                preparedRestore = prepared
                recoveryPassword = retainedPassword
                restoreState.value = RestoreUiState.Ready(prepared.preview)
                transferState.value = TransferUiState.Idle
            },
            onFailure = {
                retainedPassword.fill('\u0000')
                transferState.value = TransferUiState.Failure(UiText.fromError(it, R.string.msg_backup_verify_failed))
            },
        )
    }

    fun confirmPreparedRestore() = viewModelScope.launch {
        val prepared = preparedRestore ?: return@launch
        val password = recoveryPassword ?: return@launch
        restoreState.value = RestoreUiState.Idle
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_restoring_backup))
        val result = runCatching {
            withContext(Dispatchers.IO) { backupManager.restore(prepared, password) }
        }
        password.fill('\u0000')
        preparedRestore = null
        recoveryPassword = null
        transferState.value = result.fold(
            onSuccess = { restored ->
                recoveryAvailable.value = restored.recoveryCreated
                val selection = repository.currentWorkspace()
                selectedBusinessId.value = selection.businessId
                selectedProjectId.value = selection.projectId
                // A restore replaces everything and asks for a full snapshot; send it (DEC-024).
                scheduleSync(delayMillis = 0)
                TransferUiState.Success(UiText.of(R.string.msg_restore_ok, restored.rowCount, restored.tableCount))
            },
            onFailure = { TransferUiState.Failure(UiText.fromError(it, R.string.msg_restore_failed)) },
        )
    }

    fun cancelPreparedRestore() {
        recoveryPassword?.fill('\u0000')
        recoveryPassword = null
        preparedRestore = null
        restoreState.value = RestoreUiState.Idle
    }

    fun exportLatestRecovery(contentResolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_copying_recovery))
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val bytes = backupManager.readLatestRecovery()
                contentResolver.openOutputStream(uri, "w")?.use { output ->
                    output.write(bytes)
                    output.flush()
                } ?: error("Tujuan salinan pemulihan tidak dapat dibuka.")
                bytes.size
            }
        }
        transferState.value = result.fold(
            onSuccess = { TransferUiState.Success(UiText.of(R.string.msg_recovery_export_ok, it / 1024)) },
            onFailure = { TransferUiState.Failure(UiText.fromError(it, R.string.msg_recovery_export_failed)) },
        )
    }

    fun onGoogleSignedIn() {
        identity.refresh()
        val user = identity.currentUser.value ?: return
        message.value = UiText.of(R.string.msg_sign_in_ok, user.displayName ?: user.email ?: "")
    }

    fun signOut() {
        identity.signOut()
        driveBackups.value = emptyList()
        message.value = UiText.of(R.string.msg_sign_out_ok)
    }

    /**
     * "Hapus semua data" (DEC-020). With [driveAccessToken], Drive goes first: if it fails, nothing
     * on the phone has been touched yet and the owner can retry. Then the
     * database and the recovery copy are erased and, when Drive was included, the Google account is
     * signed out of the app. [onErased] clears what the UI layer owns (local name).
     */
    fun eraseAllData(driveAccessToken: String?, onErased: () -> Unit) = mutating {
        cancelPreparedRestore()
        val result = runCatching {
            if (driveAccessToken != null) cloudBackupClient.deleteAllAppData(driveAccessToken)
            repository.eraseAllData()
            withContext(Dispatchers.IO) { backupManager.deleteLatestRecovery() }
        }
        result.fold(
            onSuccess = {
                // The database's sync flags are gone with sync_meta; the stored passphrase must go too.
                syncController?.disable()
                driveBackups.value = emptyList()
                importState.value = ImportUiState.Idle
                selectedProjectId.value = null
                selectedBusinessId.value = null
                // Revoke the Google grant (DEC-020) before the account state is cleared.
                if (driveAccessToken != null) identity.revokeAndSignOut()
                onErased()
                message.value = UiText.of(R.string.msg_erase_ok)
            },
            onFailure = { message.value = UiText.fromError(it, R.string.msg_erase_failed) },
        )
    }

    fun fetchDriveBackups(accessToken: String) = viewModelScope.launch {
        isDriveLoading.value = true
        val result = runCatching {
            cloudBackupClient.listBackups(accessToken)
        }
        isDriveLoading.value = false
        result.fold(
            onSuccess = { driveBackups.value = it },
            onFailure = { transferState.value = TransferUiState.Failure(UiText.fromError(it, R.string.msg_drive_list_failed)) }
        )
    }

    fun backupToDrive(password: CharArray, accessToken: String) = viewModelScope.launch {
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_uploading_backup))
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val bytes = backupManager.create(password)
                val sha256 = bytes.sha256Hex()
                val filename = "umkm-mankeu-backup-${LocalDate.now()}.aabk"
                cloudBackupClient.uploadBackup(accessToken, filename, bytes, sha256)
                bytes.size
            }
        }
        password.fill('\u0000')
        transferState.value = result.fold(
            onSuccess = { size ->
                fetchDriveBackups(accessToken)
                TransferUiState.Success(UiText.of(R.string.msg_drive_backup_ok, size / 1024))
            },
            onFailure = { TransferUiState.Failure(UiText.fromError(it, R.string.msg_drive_backup_failed)) }
        )
    }

    fun prepareDriveBackupRestore(fileId: String, password: CharArray, accessToken: String) = viewModelScope.launch {
        cancelPreparedRestore()
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_downloading_backup))
        val retainedPassword = password.copyOf()
        val result = runCatching {
            withContext(Dispatchers.IO) {
                try {
                    val bytes = cloudBackupClient.downloadBackup(accessToken, fileId)
                    require(bytes.size <= 160 * 1024 * 1024) { "Berkas cadangan melebihi batas aman 160 MB." }
                    backupManager.prepareRestore(bytes, password)
                } finally {
                    password.fill('\u0000')
                }
            }
        }
        result.fold(
            onSuccess = { prepared ->
                preparedRestore = prepared
                recoveryPassword = retainedPassword
                restoreState.value = RestoreUiState.Ready(prepared.preview)
                transferState.value = TransferUiState.Idle
            },
            onFailure = {
                retainedPassword.fill('\u0000')
                transferState.value = TransferUiState.Failure(UiText.fromError(it, R.string.msg_drive_restore_failed))
            }
        )
    }

    fun clearTransferState() {
        transferState.value = TransferUiState.Idle
    }

    override fun onCleared() {
        cancelPreparedRestore()
        super.onCleared()
    }

    fun clearMessage() {
        message.value = null
    }

    private data class ScopeContent(
        val businesses: List<BusinessEntity>,
        val projects: List<ProjectEntity>,
        val books: List<BookEntity>,
        val categories: List<CategoryEntity>,
    )

    private data class LedgerAggregates(
        val eventCount: Int,
        val cashFlowRecords: List<CashFlowRecord>,
        val categoryTotals: List<CategoryTotalRow>,
    )

    private data class FinanceContent(
        val events: List<MoneyEvent>,
        val budgetItems: List<BudgetItemEntity>,
        val stockItems: List<StockItem>,
        val summary: FinancialSummary,
        val plan: PlanContent,
    )

    private data class PlanContent(
        val products: List<ProductRecord>,
        val planItems: List<BudgetItemEntity>,
        val plan: PlanAnalysis?,
        val actualMonth: ActualMonthAnalysis?,
    )

    private data class SelectionContent(
        val businessId: String?,
        val projectId: String?,
        val bookMode: BookMode,
        val bookId: String?,
    )

    private data class ImportAudit(
        val notices: List<ImportNoticeEntity>,
        val batches: List<ImportBatchEntity>,
    )

    private data class ComparisonContent(
        val projection: FinancialSummary,
        val realization: FinancialSummary,
        val realizationMonths: Int,
    )
}

private fun BookMode.label() = if (this == BookMode.PROJECTION) "Proyeksi" else "Realisasi"
