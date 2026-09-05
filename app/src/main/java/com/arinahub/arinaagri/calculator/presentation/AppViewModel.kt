package com.arinahub.arinaagri.calculator.presentation

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinahub.arinaagri.calculator.backup.EncryptedBackupManager
import com.arinahub.arinaagri.calculator.backup.BackupPreview
import com.arinahub.arinaagri.calculator.backup.PreparedRestore
import com.arinahub.arinaagri.calculator.data.BookEntity
import com.arinahub.arinaagri.calculator.data.BudgetItemDraft
import com.arinahub.arinaagri.calculator.data.BudgetItemEntity
import com.arinahub.arinaagri.calculator.data.BusinessEntity
import com.arinahub.arinaagri.calculator.data.CategoryEntity
import com.arinahub.arinaagri.calculator.data.EventDraft
import com.arinahub.arinaagri.calculator.data.FinanceRepository
import com.arinahub.arinaagri.calculator.data.ImportNoticeEntity
import com.arinahub.arinaagri.calculator.data.ImportBatchEntity
import com.arinahub.arinaagri.calculator.data.ObligationView
import com.arinahub.arinaagri.calculator.data.ProjectEntity
import com.arinahub.arinaagri.calculator.data.ScopeRemoval
import com.arinahub.arinaagri.calculator.data.ScenarioAssumptionEntity
import com.arinahub.arinaagri.calculator.data.UnitEntity
import com.arinahub.arinaagri.calculator.domain.BookMode
import com.arinahub.arinaagri.calculator.domain.BusinessSector
import com.arinahub.arinaagri.calculator.domain.CategoryRole
import com.arinahub.arinaagri.calculator.domain.FinancialEventType
import com.arinahub.arinaagri.calculator.domain.FinancialSummary
import com.arinahub.arinaagri.calculator.domain.MoneyEvent
import com.arinahub.arinaagri.calculator.domain.PostingReportCalculator
import com.arinahub.arinaagri.calculator.domain.TransactionCalculation
import com.arinahub.arinaagri.calculator.importer.FinanceWorkbookImporter
import com.arinahub.arinaagri.calculator.importer.WorkbookImportPreview
import com.arinahub.arinaagri.calculator.R
import com.arinahub.arinaagri.calculator.auth.AuthService
import com.arinahub.arinaagri.calculator.auth.AuthUser
import com.arinahub.arinaagri.calculator.backup.cloud.CloudBackupClient
import com.arinahub.arinaagri.calculator.backup.cloud.DriveBackupFile
import com.google.firebase.auth.AuthCredential
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

private val EMPTY_SUMMARY = PostingReportCalculator.calculate(emptyList())

data class AppUiState(
    val businesses: List<BusinessEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val books: List<BookEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val units: List<UnitEntity> = emptyList(),
    val budgetItems: List<BudgetItemEntity> = emptyList(),
    val selectedBusinessId: String? = null,
    val selectedProjectId: String? = null,
    val selectedBookMode: BookMode = BookMode.REALIZATION,
    val selectedBookId: String? = null,
    val events: List<MoneyEvent> = emptyList(),
    val obligations: List<ObligationView> = emptyList(),
    val summary: FinancialSummary = EMPTY_SUMMARY,
    val projectionSummary: FinancialSummary = EMPTY_SUMMARY,
    val realizationSummary: FinancialSummary = EMPTY_SUMMARY,
    val projectionBudgetTotalRupiah: Long = 0,
    val realizationBudgetTotalRupiah: Long = 0,
    val assumptions: ScenarioAssumptionEntity? = null,
    val notices: List<ImportNoticeEntity> = emptyList(),
    val importBatches: List<ImportBatchEntity> = emptyList(),
    /**
     * True only for the initial placeholder emitted before the first database read completes.
     * Screens use it to show skeleton placeholders instead of briefly rendering a real-looking
     * but empty state (which would read as "you have no records" during a cold start).
     */
    val isLoading: Boolean = true,
)

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Loading : ImportUiState
    data class Ready(val preview: WorkbookImportPreview) : ImportUiState
    data class Committed(val count: Int, val enrichmentCount: Int) : ImportUiState
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
    private val authService: AuthService,
    private val cloudBackupClient: CloudBackupClient,
) : ViewModel() {
    private val selectedBusinessId = MutableStateFlow<String?>(null)
    private val selectedProjectId = MutableStateFlow<String?>(null)
    private val selectedBookMode = MutableStateFlow(BookMode.REALIZATION)

    val authUser: StateFlow<AuthUser?> = authService.currentUser
    val driveBackups = MutableStateFlow<List<DriveBackupFile>>(emptyList())
    val isDriveLoading = MutableStateFlow(false)

    val importState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val transferState = MutableStateFlow<TransferUiState>(TransferUiState.Idle)
    val restoreState = MutableStateFlow<RestoreUiState>(RestoreUiState.Idle)
    val recoveryAvailable = MutableStateFlow(backupManager.hasLatestRecovery())
    val message = MutableStateFlow<UiText?>(null)
    private var preparedRestore: PreparedRestore? = null
    private var recoveryPassword: CharArray? = null

    private val businesses = repository.businesses.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val projects = selectedBusinessId.flatMapLatest { id ->
        id?.let(repository::projects) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val books = selectedProjectId.flatMapLatest { id ->
        id?.let(repository::books) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val categories = selectedBusinessId.flatMapLatest { id ->
        id?.let(repository::categories) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val units = selectedBusinessId.flatMapLatest { id ->
        id?.let(repository::units) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val selectedBookId = combine(books, selectedBookMode) { items, mode ->
        items.firstOrNull { it.mode == mode.name }?.id
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val events = selectedBookId.flatMapLatest { id ->
        id?.let(repository::eventsForBook) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val budgetItems = selectedBookId.flatMapLatest { id ->
        id?.let(repository::budgetItems) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val obligations = selectedBookId.flatMapLatest { id ->
        id?.let(repository::obligations) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val summary = selectedBookId.flatMapLatest { id ->
        id?.let(repository::accountingSummary) ?: flowOf(EMPTY_SUMMARY)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EMPTY_SUMMARY)
    private val assumptions = selectedBookId.flatMapLatest { id ->
        id?.let(repository::assumptions) ?: flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val importBatches = selectedProjectId.flatMapLatest { id ->
        id?.let(repository::importBatches) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val projectionSummary = books.flatMapLatest { items ->
        items.firstOrNull { it.mode == BookMode.PROJECTION.name }?.id
            ?.let(repository::accountingSummary) ?: flowOf(EMPTY_SUMMARY)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EMPTY_SUMMARY)
    private val realizationSummary = books.flatMapLatest { items ->
        items.firstOrNull { it.mode == BookMode.REALIZATION.name }?.id
            ?.let(repository::accountingSummary) ?: flowOf(EMPTY_SUMMARY)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EMPTY_SUMMARY)
    private val projectionBudgetTotal = books.flatMapLatest { items ->
        items.firstOrNull { it.mode == BookMode.PROJECTION.name }?.id
            ?.let(repository::budgetItems)?.map { budgets -> budgets.sumOf { it.plannedTotalRupiah } }
            ?: flowOf(0L)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    private val realizationBudgetTotal = books.flatMapLatest { items ->
        items.firstOrNull { it.mode == BookMode.REALIZATION.name }?.id
            ?.let(repository::budgetItems)?.map { budgets -> budgets.sumOf { it.plannedTotalRupiah } }
            ?: flowOf(0L)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    private val scopeContent = combine(businesses, projects, books, categories, units) {
            businessItems, projectItems, bookItems, categoryItems, unitItems ->
        ScopeContent(businessItems, projectItems, bookItems, categoryItems, unitItems)
    }
    private val financeContent = combine(events, budgetItems, obligations, summary, assumptions) {
            eventItems, budgetItemItems, obligationItems, calculatedSummary, scenarioAssumptions ->
        FinanceContent(eventItems, budgetItemItems, obligationItems, calculatedSummary, scenarioAssumptions)
    }
    private val selections = combine(selectedBusinessId, selectedProjectId, selectedBookMode, selectedBookId) {
            businessId, projectId, mode, bookId ->
        SelectionContent(businessId, projectId, mode, bookId)
    }
    private val comparison = combine(projectionSummary, realizationSummary, projectionBudgetTotal, realizationBudgetTotal) {
            projection, realization, projectionBudget, realizationBudget ->
        ComparisonContent(projection, realization, projectionBudget, realizationBudget)
    }
    private val importAudit = combine(repository.notices, importBatches) { notices, batches ->
        ImportAudit(notices, batches)
    }

    val uiState = combine(scopeContent, financeContent, importAudit, selections, comparison) {
            scope, finance, audit, selection, comparison ->
        AppUiState(
            businesses = scope.businesses,
            projects = scope.projects,
            books = scope.books,
            categories = scope.categories,
            units = scope.units,
            budgetItems = finance.budgetItems,
            selectedBusinessId = selection.businessId,
            selectedProjectId = selection.projectId,
            selectedBookMode = selection.bookMode,
            selectedBookId = selection.bookId,
            events = finance.events,
            obligations = finance.obligations,
            summary = finance.summary,
            projectionSummary = comparison.projection,
            realizationSummary = comparison.realization,
            projectionBudgetTotalRupiah = comparison.projectionBudget,
            realizationBudgetTotalRupiah = comparison.realizationBudget,
            assumptions = finance.assumptions,
            notices = audit.notices,
            importBatches = audit.batches,
            // Any emission from this combine means the underlying flows have produced real data.
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState(isLoading = true))

    init {
        viewModelScope.launch {
            // Reads only. A first run now legitimately resolves to (null, null) and the shell
            // asks the owner to name their own usaha and lahan, instead of the app writing
            // "Usaha Saya"/"Umum" behind their back and presenting it as their data.
            val selection = repository.currentWorkspace()
            selectedBusinessId.value = selection.businessId
            selectedProjectId.value = selection.projectId
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

    fun createBusiness(
        name: String,
        sector: BusinessSector = BusinessSector.GENERAL_MSME,
    ) = viewModelScope.launch {
        runCatching { repository.createBusiness(name, sector) }
            .onSuccess { businessId ->
                selectedBusinessId.value = businessId
                selectedProjectId.value = null
                message.value = UiText.of(R.string.msg_business_created)
            }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_business_create_failed) }
    }

    fun createProject(name: String) = viewModelScope.launch {
        val businessId = selectedBusinessId.value ?: return@launch
        runCatching { repository.createProject(businessId, name) }
            .onSuccess { projectId ->
                selectedProjectId.value = projectId
                message.value = UiText.of(R.string.msg_project_created)
            }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_project_create_failed) }
    }

    fun renameBusiness(name: String) = viewModelScope.launch {
        val id = selectedBusinessId.value ?: return@launch
        runCatching { repository.renameBusiness(id, name) }
            .onSuccess { message.value = UiText.of(R.string.msg_business_renamed) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_business_rename_failed) }
    }

    fun renameProject(name: String) = viewModelScope.launch {
        val id = selectedProjectId.value ?: return@launch
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
    ) = viewModelScope.launch {
        runCatching { repository.updateProjectDetails(id, name, description, startDate, endDate) }
            .onSuccess { message.value = UiText.of(R.string.msg_project_updated) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_project_update_failed) }
    }

    /**
     * Archiving the active scope leaves nothing selected; the `businesses`/`projects` collectors in
     * `init` notice the selection has gone and fall back to the first remaining row, or to null when
     * there is none — which is what puts the explicit "Tambah Usaha"/"Tambah Lahan" state back on
     * screen rather than leaving a dangling id.
     */
    fun removeBusiness(id: String) = viewModelScope.launch {
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

    fun removeProject(id: String) = viewModelScope.launch {
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

    fun createCategory(name: String, role: CategoryRole) = viewModelScope.launch {
        val businessId = selectedBusinessId.value ?: return@launch
        runCatching { repository.createCategory(businessId, name, role, aliases = listOf(name)) }
            .onSuccess { message.value = UiText.of(R.string.msg_category_added, name) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_category_add_failed) }
    }

    fun archiveCategory(id: String) = viewModelScope.launch {
        runCatching { repository.archiveCategory(id) }
            .onSuccess { message.value = UiText.of(R.string.msg_category_archived) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_category_archive_failed) }
    }

    fun createUnit(name: String, symbol: String) = viewModelScope.launch {
        val businessId = selectedBusinessId.value ?: return@launch
        runCatching { repository.createUnit(businessId, name, symbol) }
            .onSuccess { message.value = UiText.of(R.string.msg_unit_added, name) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_unit_add_failed) }
    }

    fun archiveUnit(id: String) = viewModelScope.launch {
        runCatching { repository.archiveUnit(id) }
            .onSuccess { message.value = UiText.of(R.string.msg_unit_archived) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_unit_archive_failed) }
    }

    fun record(
        type: FinancialEventType,
        amountRupiah: Long,
        description: String,
        categoryId: String?,
        category: String,
        date: LocalDate,
        counterparty: String? = null,
        dueDate: LocalDate? = null,
        reference: String? = null,
        quantityText: String? = null,
        unit: String? = null,
        unitPriceRupiah: Long? = null,
        budgetItemId: String? = null,
    ) = viewModelScope.launch {
        val state = uiState.value
        val businessId = state.selectedBusinessId
        val projectId = state.selectedProjectId
        val bookId = state.selectedBookId
        if (businessId == null || projectId == null || bookId == null) {
            message.value = UiText.of(R.string.entry_err_no_project)
            return@launch
        }
        runCatching {
            val reconciled = TransactionCalculation.reconcile(
                amountRupiah = amountRupiah,
                quantityText = quantityText,
                unitPriceRupiah = unitPriceRupiah,
            )
            repository.recordEvent(
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
                    counterparty = counterparty,
                    dueDate = dueDate,
                    reference = reference,
                    quantityText = reconciled.quantityText,
                    unit = unit,
                    unitPriceRupiah = reconciled.unitPriceRupiah,
                    budgetItemId = budgetItemId,
                ),
            )
        }.onSuccess {
            message.value = UiText.of(
                when (type) {
                    FinancialEventType.CREDIT_SALE -> R.string.msg_credit_sale
                    FinancialEventType.CREDIT_PURCHASE_EXPENSE,
                    FinancialEventType.CREDIT_PURCHASE_ASSET,
                    -> R.string.msg_credit_purchase
                    else -> R.string.msg_event_saved
                },
            )
        }.onFailure {
            message.value = UiText.fromError(it, R.string.msg_event_save_failed)
        }
    }

    fun deleteTransaction(eventId: String) = viewModelScope.launch {
        runCatching {
            repository.deleteEvent(eventId)
        }.onSuccess {
            message.value = UiText.of(R.string.msg_event_deleted)
        }.onFailure {
            message.value = UiText.fromError(it, R.string.msg_event_save_failed)
        }
    }

    fun createBudgetItem(
        categoryId: String,
        name: String,
        quantityText: String?,
        unit: String?,
        unitPriceRupiah: Long?,
        plannedTotalRupiah: Long?,
        plannedDate: LocalDate?,
    ) = viewModelScope.launch {
        val bookId = selectedBookId.value ?: return@launch
        val reconciled = TransactionCalculation.reconcile(
            amountRupiah = plannedTotalRupiah ?: 0L,
            quantityText = quantityText,
            unitPriceRupiah = unitPriceRupiah,
        )
        runCatching {
            repository.createBudgetItem(
                BudgetItemDraft(
                    bookId = bookId,
                    categoryId = categoryId,
                    name = name,
                    quantityText = reconciled.quantityText,
                    unit = unit,
                    unitPriceRupiah = reconciled.unitPriceRupiah,
                    plannedTotalRupiah = reconciled.totalRupiah.takeIf { it > 0 } ?: plannedTotalRupiah,
                    plannedDate = plannedDate,
                    aliases = listOf(name),
                ),
            )
        }.onSuccess {
            message.value = UiText.of(R.string.msg_budget_added, selectedBookMode.value.label())
        }.onFailure {
            message.value = UiText.fromError(it, R.string.msg_budget_add_failed)
        }
    }

    fun archiveBudgetItem(id: String) = viewModelScope.launch {
        runCatching { repository.archiveBudgetItem(id) }
            .onSuccess { message.value = UiText.of(R.string.msg_budget_archived) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_budget_archive_failed) }
    }

    fun updateBudgetItem(
        id: String,
        categoryId: String,
        name: String,
        quantityText: String?,
        unit: String?,
        unitPriceRupiah: Long?,
        plannedTotalRupiah: Long?,
        plannedDate: LocalDate?,
    ) = viewModelScope.launch {
        val bookId = selectedBookId.value ?: return@launch
        val reconciled = TransactionCalculation.reconcile(
            amountRupiah = plannedTotalRupiah ?: 0L,
            quantityText = quantityText,
            unitPriceRupiah = unitPriceRupiah,
        )
        runCatching {
            repository.updateBudgetItem(
                id,
                BudgetItemDraft(
                    bookId = bookId,
                    categoryId = categoryId,
                    name = name,
                    quantityText = reconciled.quantityText,
                    unit = unit,
                    unitPriceRupiah = reconciled.unitPriceRupiah,
                    plannedTotalRupiah = reconciled.totalRupiah.takeIf { it > 0 } ?: plannedTotalRupiah,
                    plannedDate = plannedDate,
                    aliases = listOf(name),
                ),
            )
        }.onSuccess { message.value = UiText.of(R.string.msg_budget_updated) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_budget_update_failed) }
    }

    fun settleObligation(id: String, amountRupiah: Long, date: LocalDate, description: String) = viewModelScope.launch {
        runCatching { repository.settleObligation(id, amountRupiah, date, description) }
            .onSuccess { message.value = UiText.of(R.string.msg_settlement_saved) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_settlement_failed) }
    }

    fun reverseEvent(id: String, reason: String) = viewModelScope.launch {
        runCatching { repository.reverseEvent(id, LocalDate.now(), reason) }
            .onSuccess { message.value = UiText.of(R.string.msg_event_reversed) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_event_reverse_failed) }
    }

    fun saveAssumptions(
        openingCashRupiah: Long,
        ownerCapitalRupiah: Long,
        loanAmountRupiah: Long,
        interestRateBasisPoints: Int,
        otherFinancingCostRupiah: Long,
        outputQuantityText: String?,
        outputUnit: String?,
        outputPriceRupiah: Long?,
    ) = viewModelScope.launch {
        val bookId = selectedBookId.value ?: return@launch
        val existing = uiState.value.assumptions
        runCatching {
            repository.saveAssumptions(
                ScenarioAssumptionEntity(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    bookId = bookId,
                    openingCashRupiah = openingCashRupiah,
                    ownerCapitalRupiah = ownerCapitalRupiah,
                    loanAmountRupiah = loanAmountRupiah,
                    interestRateBasisPoints = interestRateBasisPoints,
                    loanDisbursementEpochDay = existing?.loanDisbursementEpochDay,
                    loanPaymentEpochDay = existing?.loanPaymentEpochDay,
                    otherFinancingCostRupiah = otherFinancingCostRupiah,
                    outputQuantityText = outputQuantityText,
                    outputUnit = outputUnit,
                    outputPriceRupiah = outputPriceRupiah,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }.onSuccess { message.value = UiText.of(R.string.msg_assumptions_saved) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_assumptions_failed) }
    }

    fun previewWorkbook(contentResolver: ContentResolver, uri: Uri) = viewModelScope.launch {
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
                importer.preview(name, bytes)
            }
        }
        importState.value = result.fold(
            onSuccess = { preview ->
                if (preview.errors.isNotEmpty() && !preview.canCommit) {
                    ImportUiState.Failure(UiText.Raw(preview.errors.joinToString("\n")))
                } else {
                    ImportUiState.Ready(preview)
                }
            },
            onFailure = { ImportUiState.Failure(UiText.fromError(it, R.string.msg_import_failed)) },
        )
    }

    /**
     * @param targetBook which book the rows land in. Null keeps whatever the file says, which only
     * an Arina export does; for everything else the screen passes the book the user is looking at.
     */
    fun commitImport(targetBook: BookMode? = null) = viewModelScope.launch {
        val preview = (importState.value as? ImportUiState.Ready)?.preview ?: return@launch
        importState.value = ImportUiState.Loading
        val result = runCatching {
            withContext(Dispatchers.IO) {
                var businessId = selectedBusinessId.value
                if (businessId == null) {
                    val rawName = preview.sourceName.substringBeforeLast(".").trim()
                    val businessName = if (rawName.isBlank() || rawName.equals("workbook", ignoreCase = true)) {
                        "Usaha Tani"
                    } else {
                        rawName
                    }
                    businessId = repository.createBusiness(businessName)
                    selectedBusinessId.value = businessId
                }

                var projectId = selectedProjectId.value
                if (projectId == null) {
                    val newProjectId = repository.createProject(businessId, "Lahan Utama")
                    selectedProjectId.value = newProjectId
                    projectId = newProjectId
                }

                val destination = if (preview.carriesBookMode) null else (targetBook ?: selectedBookMode.value)
                repository.commitImport(businessId, projectId, preview, destination)
            }
        }
        importState.value = result.fold(
            onSuccess = { ImportUiState.Committed(it, preview.enrichmentCount) },
            onFailure = { ImportUiState.Failure(UiText.fromError(it, R.string.msg_import_commit_failed)) },
        )
    }

    fun resetImportState() {
        importState.value = ImportUiState.Idle
    }

    fun reverseImportBatch(id: String) = viewModelScope.launch {
        runCatching { repository.reverseImportBatch(id) }
            .onSuccess { count -> message.value = UiText.of(R.string.msg_batch_reversed, count) }
            .onFailure { message.value = UiText.fromError(it, R.string.msg_batch_reverse_failed) }
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

    fun signInWithGoogleIdToken(idToken: String) = viewModelScope.launch {
        transferState.value = TransferUiState.Working(UiText.of(R.string.work_restoring_backup))
        val result = authService.signInWithGoogleIdToken(idToken)
        transferState.value = result.fold(
            onSuccess = { user ->
                message.value = UiText.of(R.string.msg_sign_in_ok, user.displayName ?: user.email ?: "")
                TransferUiState.Idle
            },
            onFailure = {
                TransferUiState.Failure(UiText.fromError(it, R.string.msg_sign_in_failed))
            }
        )
    }

    fun signInWithCredential(credential: AuthCredential) = viewModelScope.launch {
        val result = authService.signInWithCredential(credential)
        transferState.value = result.fold(
            onSuccess = { user ->
                message.value = UiText.of(R.string.msg_sign_in_ok, user.displayName ?: user.email ?: "")
                TransferUiState.Idle
            },
            onFailure = {
                TransferUiState.Failure(UiText.fromError(it, R.string.msg_sign_in_failed))
            }
        )
    }

    fun setAuthenticatedUser(user: AuthUser?) {
        authService.setCustomUser(user)
    }

    fun signOut() {
        authService.signOut()
        driveBackups.value = emptyList()
        message.value = UiText.of(R.string.msg_sign_out_ok)
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
                val sha256 = computeSha256Hex(bytes)
                val filename = "mankeu-backup-${LocalDate.now()}.aabk"
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

    private fun computeSha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes).joinToString("") { "%02x".format(it) }
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

    class Factory(
        private val repository: FinanceRepository,
        private val importer: FinanceWorkbookImporter,
        private val backupManager: EncryptedBackupManager,
        private val authService: AuthService,
        private val cloudBackupClient: CloudBackupClient,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AppViewModel(repository, importer, backupManager, authService, cloudBackupClient) as T
    }

    private data class ScopeContent(
        val businesses: List<BusinessEntity>,
        val projects: List<ProjectEntity>,
        val books: List<BookEntity>,
        val categories: List<CategoryEntity>,
        val units: List<UnitEntity>,
    )

    private data class FinanceContent(
        val events: List<MoneyEvent>,
        val budgetItems: List<BudgetItemEntity>,
        val obligations: List<ObligationView>,
        val summary: FinancialSummary,
        val assumptions: ScenarioAssumptionEntity?,
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
        val projectionBudget: Long,
        val realizationBudget: Long,
    )
}

private fun BookMode.label() = if (this == BookMode.PROJECTION) "Proyeksi" else "Realisasi"
