package com.mankeu.data

import com.mankeu.domain.CostNature
import com.mankeu.domain.PlanAnalysis
import com.mankeu.domain.PlanCalculator
import com.mankeu.domain.Recurrence
import com.mankeu.exporter.TemplateCategory
import com.mankeu.importer.ImportProduct
import com.mankeu.importer.ImportStockItem
import androidx.room.withTransaction
import com.mankeu.domain.BookMode
import com.mankeu.domain.DefaultMasterData
import com.mankeu.domain.CashFlowRecord
import com.mankeu.domain.CashFlowRole
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.JournalEntry
import com.mankeu.domain.LedgerAccount
import com.mankeu.domain.LedgerPolicy
import com.mankeu.domain.MoneyEvent
import com.mankeu.domain.Posting
import com.mankeu.domain.PostedRecord
import com.mankeu.domain.PostingSide
import com.mankeu.domain.PostingReportCalculator
import com.mankeu.domain.ProductMovementType
import com.mankeu.domain.RecordStockMovementUseCase
import com.mankeu.domain.StockItem
import com.mankeu.domain.StockItemEntity
import com.mankeu.domain.StockMovementType
import com.mankeu.domain.TransactionCalculation
import com.mankeu.domain.toDomain
import com.mankeu.exporter.FinanceExportBook
import com.mankeu.exporter.FinanceExportBundle
import com.mankeu.exporter.FinancePdfExporter
import com.mankeu.exporter.FinanceWorkbookExporter
import com.mankeu.importer.MankeuTemplate
import com.mankeu.importer.WorkbookImportPreview
import com.mankeu.sync.SyncSchema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

data class EventDraft(
    val businessId: String,
    val projectId: String,
    val bookId: String,
    val type: FinancialEventType,
    val amountRupiah: Long,
    val date: LocalDate,
    val description: String,
    val category: String,
    val categoryId: String? = null,
    val reference: String? = null,
    val quantityText: String? = null,
    val unit: String? = null,
    val unitPriceRupiah: Long? = null,
    val budgetItemId: String? = null,
    val importBatchId: String? = null,
    /** Overrides the category's cost nature (DEC-014 B-1). */
    val costNature: CostNature? = null,
    /** The product a sale was for, with units in [quantityText] (DEC-014 B-7). */
    val productId: String? = null,
    /** Asset purchases only (DEC-014 B-4). */
    val usefulLifeMonths: Int? = null,
    /** The material an expense bought (spec #20). */
    val stockItemId: String? = null,
)

data class BudgetItemDraft(
    val bookId: String,
    val categoryId: String,
    val name: String,
    val quantityText: String? = null,
    val unit: String? = null,
    val unitPriceRupiah: Long? = null,
    val plannedTotalRupiah: Long? = null,
    val plannedDate: LocalDate? = null,
    val aliases: List<String> = emptyList(),
    val recurrence: Recurrence = Recurrence.ONCE,
    val costNature: CostNature? = null,
    val usefulLifeMonths: Int? = null,
)

/** One sale unit as the product editor edits it; [id] keeps an existing unit's id. */
data class SaleUnitDraft(val id: String? = null, val name: String, val contentText: String, val priceRupiah: Long)

/** One recipe line: a material and its quantity per batch, or a free cost per batch. */
data class RecipeLineDraft(val name: String, val stockItemId: String? = null, val quantityText: String? = null, val amountRupiah: Long? = null)

data class ProductDraft(
    val name: String,
    val baseUnit: String,
    val batchYieldText: String,
    val units: List<SaleUnitDraft>,
    val recipe: List<RecipeLineDraft>,
    val targetPerMonthText: String? = null,
    val sellFromMonth: Int = 1,
)

/** See [FinanceRepository.reviewImport]. */
data class ImportReview(
    val newCategories: List<Pair<String, CategoryRole>>,
    val conflicts: List<String>,
    val existingStockNames: List<String>,
    val previouslyImported: Boolean,
    val existingProductNames: List<String> = emptyList(),
    /** Rows the commit will actually add: the file's rows minus stock and products skipped by name. */
    val addedRowCount: Int = 0,
)

/** What [FinanceRepository.removeProject]/[FinanceRepository.removeBusiness] actually did. */
enum class ScopeRemoval { DELETED, ARCHIVED }

/** Which business/project the shell should select on launch. Nulls mean "nothing exists yet". */
data class WorkspaceSelection(val businessId: String?, val projectId: String?)

class FinanceRepository(private val database: AppDatabase) {
    private val dao = database.financeDao()
    val stockDao = database.stockDao()

    val businesses: Flow<List<BusinessEntity>> = dao.observeBusinesses()
    val notices: Flow<List<ImportNoticeEntity>> = dao.observeNotices()

    fun projects(businessId: String): Flow<List<ProjectEntity>> = dao.observeProjects(businessId)
    fun books(projectId: String): Flow<List<BookEntity>> = dao.observeBooks(projectId)
    fun categories(businessId: String): Flow<List<CategoryEntity>> = dao.observeCategories(businessId)
    fun budgetItems(bookId: String): Flow<List<BudgetItemEntity>> = dao.observeBudgetItems(bookId)
    fun costRows(bookId: String): Flow<List<CostRow>> = dao.observeCostRows(bookId)

    /** Products of a business unit with sale units, recipe and stock movements (spec #5 #6 #9). */
    fun products(projectId: String): Flow<List<ProductRecord>> = combine(
        dao.observeProducts(projectId),
        dao.observeProductUnits(projectId),
        dao.observeRecipeLines(projectId),
        dao.observeProductMovements(projectId),
        dao.observeProductSales(projectId, FinancialEventType.CASH_INCOME.name, BookMode.REALIZATION.name),
    ) { products, units, recipe, movements, sales ->
        val unitsOf = units.groupBy { it.productId }
        val recipeOf = recipe.groupBy { it.productId }
        val movementsOf = movements.groupBy { it.productId }
        val salesOf = sales.groupBy { it.productId }
        products.map { ProductRecord(it, unitsOf[it.id].orEmpty(), recipeOf[it.id].orEmpty(), movementsOf[it.id].orEmpty(), salesOf[it.id].orEmpty()) }
    }
    fun importBatches(projectId: String): Flow<List<ImportBatchEntity>> = dao.observeImportBatches(projectId)

    /** Runs [block] as one database transaction, for a caller composing several repository writes. */
    suspend fun <T> inTransaction(block: suspend () -> T): T = database.withTransaction(block)

    /**
     * The newest [RECENT_EVENT_LIMIT] rows of a book, for the ledger and the recent list.
     * Aggregates never read this — see [cashFlowRecords] and [categoryTotals].
     */
    fun eventsForBook(bookId: String): Flow<List<MoneyEvent>> =
        dao.observeEventsForBook(bookId, RECENT_EVENT_LIMIT).map { entities -> entities.map { it.toDomain() } }

    fun eventCount(bookId: String): Flow<Int> = dao.observeEventCount(bookId)

    fun cashFlowRecords(bookId: String): Flow<List<CashFlowRecord>> =
        dao.observeCashFlowRows(bookId).map { rows ->
            rows.map { row ->
                CashFlowRecord(
                    id = row.id,
                    date = LocalDate.ofEpochDay(row.dateEpochDay),
                    type = FinancialEventType.valueOf(row.type),
                    cashFlowRole = CashFlowRole.valueOf(row.cashFlowRole),
                    amountRupiah = row.amountRupiah,
                )
            }
        }

    fun categoryTotals(bookId: String): Flow<List<CategoryTotalRow>> = dao.observeCategoryTotals(bookId)

    fun accountingSummary(bookId: String) = dao.observePostingTotals(bookId).map { it.summary() }

    private fun List<PostingTotalRow>.summary() = PostingReportCalculator.calculate(
        map {
            PostedRecord(
                eventId = "",
                cashFlowRole = CashFlowRole.valueOf(it.cashFlowRole),
                account = LedgerAccount.valueOf(it.account),
                side = PostingSide.valueOf(it.side),
                amountRupiah = it.amountRupiah,
            )
        },
    )

    suspend fun buildProjectExportBundle(projectId: String): FinanceExportBundle = database.withTransaction {
            val project = dao.getProject(projectId) ?: error("Proyek tidak ditemukan.")
            val business = dao.getBusiness(project.businessId) ?: error("Usaha tidak ditemukan.")
            val categories = dao.getCategories(business.id)
            val plan = planAnalysis(project, categories)
            val products = productsOf(project.id)
            val exportBooks = ensureBookPair(project.id).values.map { book ->
                // Proyeksi reports from the business plan when there is one, as on every screen.
                val summary = plan?.summary?.takeIf { book.mode == BookMode.PROJECTION.name } ?: dao.getPostingRows(book.id).map { PostingTotalRow(it.cashFlowRole, it.account, it.side, it.amountRupiah) }.summary()
                FinanceExportBook(
                    book = book,
                    budgets = dao.getBudgetItems(book.id),
                    events = dao.getEventsForBook(book.id),
                    summary = summary,
                )
            }
            FinanceExportBundle(
                business = business,
                project = project,
                categories = categories,
                books = exportBooks,
                importBatches = dao.getImportBatches(project.id),
                notices = dao.getImportNotices(project.id),
                provenance = dao.getProvenance(project.id),
                stockItems = exportedStock(business.id, project.id, products, exportBooks),
                products = products,
                plan = plan,
            )
        }

    /**
     * What the unit sees, plus archived items a recipe line or a transaction still names, so a re-import can resolve them.
     * Each carries its current price (DEC-028 §2), which for a shared material may come from another unit's purchase
     * the export does not hold, so the copy costs the same after import (cleanup P10).
     */
    private suspend fun exportedStock(businessId: String, projectId: String, products: List<ProductRecord>, books: List<FinanceExportBook>): List<StockItemEntity> {
        val visible = stockDao.getStockItems(businessId, projectId)
        val referenced = (products.flatMap { p -> p.recipe.mapNotNull { it.stockItemId } } + books.flatMap { b -> b.events.mapNotNull { it.stockItemId } }).toSet()
        val missing = (referenced - visible.map { it.id }.toSet()).mapNotNull { stockDao.getStockItem(it) }.filter { it.businessId == businessId }
        return (visible + missing).map { it.copy(basePriceRupiah = dao.latestPurchasePrice(it.id) ?: it.basePriceRupiah) }
    }

    suspend fun exportProjectWorkbook(projectId: String): ByteArray =
        FinanceWorkbookExporter().export(buildProjectExportBundle(projectId))

    /** The blank template, listing this business's active categories (or the defaults). */
    suspend fun exportTemplate(businessId: String?): ByteArray {
        val categories = businessId?.let { dao.getCategories(it).filter(CategoryEntity::active) }.orEmpty()
            .map { TemplateCategory(it.name, CategoryRole.parse(it.role), PlanAssembly.nature(it.costNature)) }
            .ifEmpty { DefaultMasterData.categories.map { TemplateCategory(it.name, it.role, it.costNature) } }
        return FinanceWorkbookExporter().template(categories)
    }

    private suspend fun productsOf(projectId: String): List<ProductRecord> =
        dao.getProducts(projectId).map { ProductRecord(it, dao.getProductUnits(it.id), dao.getRecipeLines(it.id), dao.getProductMovements(it.id)) }

    /** Material prices for HPP: the newest purchase's unit price, else the price entered with the item (DEC-028 §2). */
    private suspend fun materialPrices(businessId: String): Map<String, Long> =
        stockDao.getAllStockItems(businessId).associate { it.id to (dao.latestPurchasePrice(it.id) ?: it.basePriceRupiah) }

    /** The business plan of a unit, from the same rows the screens read (DEC-014). */
    /** Null when the unit has no plan yet, exactly as the screens decide (a legacy posted Proyeksi stays). */
    private suspend fun planAnalysis(project: ProjectEntity, categories: List<CategoryEntity>): PlanAnalysis? {
        val plan = ensureBookPair(project.id).getValue(BookMode.PROJECTION)
        val products = productsOf(project.id)
        val lines = dao.getBudgetItems(plan.id).filterNot { it.archived }
        if (products.isEmpty() && lines.isEmpty()) return null
        return PlanCalculator.analyse(PlanAssembly.planInputs(project, products, lines, categories, LocalDate.now(), materialPrices(project.businessId)))
    }

    /**
     * Creates or replaces a product with its sale units and per-batch recipe in one transaction
     * (spec #5 #6 #20). Every sale unit needs a positive content and price; the base unit, when sold
     * as such, holds exactly 1. Material lines must name a material of this business.
     */
    suspend fun saveProduct(projectId: String, id: String?, draft: ProductDraft): String = database.withTransaction {
        val name = draft.name.trim().also { require(it.isNotBlank()) { "Nama produk wajib diisi." } }
        val baseUnit = draft.baseUnit.trim().also { require(it.isNotBlank()) { "Satuan dasar wajib diisi." } }
        val batchYield = requireNotNull(TransactionCalculation.parseQuantity(draft.batchYieldText)) { "Hasil satu batch harus angka lebih dari nol." }
        require(draft.sellFromMonth >= 1) { "Bulan mulai dijual minimal bulan ke-1." }
        val target = draft.targetPerMonthText?.trim()?.ifBlank { null }
        require(target == null || TransactionCalculation.parseQuantity(target) != null) { "Target per bulan harus angka lebih dari nol." }
        val units = draft.units.filter { it.name.isNotBlank() }
        require(units.isNotEmpty()) { "Isi minimal satu satuan jual." }
        require(units.map { it.name.trim().lowercase() }.distinct().size == units.size) { "Nama satuan jual tidak boleh kembar." }
        units.forEach { u ->
            val content = requireNotNull(TransactionCalculation.parseQuantity(u.contentText)) { "Isi satuan \"${u.name.trim()}\" harus angka lebih dari nol." }
            require(u.priceRupiah > 0) { "Harga jual per ${u.name.trim()} harus lebih dari nol." }
            require(!u.name.trim().equals(baseUnit, ignoreCase = true) || content.compareTo(BigDecimal.ONE) == 0) {
                "\"$baseUnit\" adalah satuan dasar, isinya harus 1."
            }
        }
        val project = dao.getProject(projectId) ?: error("Unit usaha tidak ditemukan.")
        // Only a line with nothing in it is dropped; a cost with an amount but no name is refused, so the
        // saved HPP is the one the preview showed (final review I-3).
        val recipe = draft.recipe.filter { it.stockItemId != null || it.name.isNotBlank() || (it.amountRupiah ?: 0) > 0 }.map { line ->
            if (line.stockItemId != null) {
                val material = stockDao.getStockItem(line.stockItemId)
                require(material != null && material.businessId == project.businessId) { "Bahan tidak ditemukan di usaha ini." }
                require(TransactionCalculation.parseQuantity(line.quantityText) != null) { "Jumlah ${material.name} per batch harus angka lebih dari nol." }
                // The material's name is kept on the line, so a removed material still shows what was used.
                line.copy(name = material.name, amountRupiah = null)
            } else {
                require(line.name.isNotBlank()) { "Isi nama biaya Rp${java.text.NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(line.amountRupiah)}." }
                require((line.amountRupiah ?: 0) > 0) { "Biaya \"${line.name.trim()}\" harus lebih dari nol." }
                line.copy(name = line.name.trim(), quantityText = null)
            }
        }
        val now = System.currentTimeMillis()
        val existing = id?.let { dao.getProduct(it) }
        val productId = existing?.id ?: UUID.randomUUID().toString()
        val yieldText = batchYield.stripTrailingZeros().toPlainString()
        if (existing == null) {
            dao.insertProduct(
                ProductEntity(
                    id = productId,
                    projectId = projectId,
                    name = name,
                    unit = baseUnit,
                    batchYieldText = yieldText,
                    targetPerMonthText = target,
                    sortOrder = dao.getProducts(projectId).size,
                    createdAtEpochMillis = now,
                    sellFromMonth = draft.sellFromMonth,
                ),
            )
        } else {
            require(existing.projectId == projectId) { "Produk berasal dari unit usaha lain." }
            dao.updateProduct(existing.copy(name = name, unit = baseUnit, batchYieldText = yieldText, targetPerMonthText = target, sellFromMonth = draft.sellFromMonth, updatedAtEpochMillis = now))
        }
        if (existing != null) carrySaleUnitsToPastSales(productId, units)
        dao.deleteProductUnits(productId)
        dao.insertProductUnits(
            units.mapIndexed { index, u ->
                ProductUnitEntity(
                    id = u.id ?: UUID.randomUUID().toString(),
                    productId = productId,
                    name = u.name.trim(),
                    contentText = requireNotNull(TransactionCalculation.parseQuantity(u.contentText)).stripTrailingZeros().toPlainString(),
                    priceRupiah = u.priceRupiah,
                    sortOrder = index,
                )
            },
        )
        dao.deleteRecipeLines(productId)
        dao.insertRecipeLines(
            recipe.mapIndexed { index, l ->
                ProductRecipeLineEntity(
                    id = UUID.randomUUID().toString(),
                    productId = productId,
                    name = l.name,
                    stockItemId = l.stockItemId,
                    quantityText = l.quantityText?.let { TransactionCalculation.parseQuantity(it)!!.stripTrailingZeros().toPlainString() },
                    amountRupiah = l.amountRupiah,
                    sortOrder = index,
                )
            },
        )
        productId
    }

    /**
     * Sales name their unit, not its id (final review I-2). A unit renamed in the editor (same
     * [SaleUnitDraft.id]) renames it on the product's past sales; removing a unit that sales still
     * name is refused, since those sales would silently count as the main unit.
     */
    private suspend fun carrySaleUnitsToPastSales(productId: String, units: List<SaleUnitDraft>) {
        val kept = units.filter { it.id != null }.associateBy { it.id }
        val old = dao.getProductUnits(productId)
        old.filter { it.id !in kept }.forEach { removed ->
            val used = dao.countSalesWithUnit(productId, removed.name)
            require(used == 0) { "Satuan \"${removed.name}\" masih dipakai di $used penjualan. Ubah penjualan itu dulu, atau ganti nama satuannya saja." }
        }
        val renamed = old.mapNotNull { unit -> kept[unit.id]?.name?.trim()?.takeIf { it != unit.name }?.let { unit to it } }
        // Through a placeholder, so two units swapping names do not merge their sales.
        renamed.forEach { (unit, _) -> dao.renameSaleUnit(productId, unit.name, "${unit.id}#") }
        renamed.forEach { (unit, name) -> dao.renameSaleUnit(productId, "${unit.id}#", name) }
    }

    /** Produksi (spec #9): [batches] batches add batches × yield base units to stock. */
    suspend fun recordProduction(productId: String, batches: BigDecimal, date: LocalDate, notes: String) = database.withTransaction {
        require(batches.signum() > 0) { "Jumlah batch harus lebih dari nol." }
        val product = dao.getProduct(productId) ?: error("Produk tidak ditemukan.")
        val yield = TransactionCalculation.parseQuantity(product.batchYieldText) ?: BigDecimal.ONE
        dao.insertProductMovement(
            ProductMovementEntity(
                id = UUID.randomUUID().toString(),
                productId = productId,
                movementType = ProductMovementType.PRODUCTION.name,
                quantityText = (batches * yield).stripTrailingZeros().toPlainString(),
                batchesText = batches.stripTrailingZeros().toPlainString(),
                dateEpochDay = date.toEpochDay(),
                notes = notes.trim(),
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    /** Hitung stok: the counted base units become the stock from [date] on; zero ("habis") is allowed. */
    suspend fun countProductStock(productId: String, counted: BigDecimal, date: LocalDate, notes: String) = database.withTransaction {
        require(counted.signum() >= 0) { "Jumlah stok tidak boleh negatif." }
        requireNotNull(dao.getProduct(productId)) { "Produk tidak ditemukan." }
        dao.insertProductMovement(
            ProductMovementEntity(
                id = UUID.randomUUID().toString(),
                productId = productId,
                movementType = ProductMovementType.ADJUSTMENT.name,
                quantityText = counted.stripTrailingZeros().toPlainString(),
                dateEpochDay = date.toEpochDay(),
                notes = notes.trim(),
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun archiveProduct(id: String) = database.withTransaction { dao.archiveProduct(id, System.currentTimeMillis()) }

    /** Sets an expense category's default cost nature (DEC-014 B-1). */
    suspend fun setCategoryCostNature(id: String, nature: CostNature?) = database.withTransaction {
        val category = dao.getCategory(id) ?: error("Kategori tidak ditemukan.")
        require(nature == null || CategoryRole.parse(category.role) == CategoryRole.EXPENSE) { "Sifat biaya hanya untuk kategori pengeluaran." }
        dao.setCategoryCostNature(id, nature?.name)
    }

    suspend fun exportProjectPdf(projectId: String): ByteArray =
        FinancePdfExporter().export(buildProjectExportBundle(projectId))

    /**
     * The workspace the app should open into, or nulls when the database is genuinely empty.
     *
     * Replaces `ensureStarterWorkspace()`, which used to *write* a business named "Usaha Saya" and
     * a project named "Umum" during startup so the shell always had something selected. That made
     * a first run look like an account someone else had already set up, and left the owner
     * renaming placeholders instead of naming their own farm. Deciding what exists is the owner's
     * call; this function only reads.
     *
     * The two `ensure...` calls below are deliberately kept: default categories and the
     * Proyeksi/Realisasi book pair are structural rows the domain requires for a business or
     * project that the owner *did* create, not named entities that appear in the scope selectors.
     */
    /** The [remembered] selection when it still exists, otherwise the first business and unit. */
    suspend fun currentWorkspace(remembered: WorkspaceSelection? = null): WorkspaceSelection = database.withTransaction {
        val business = remembered?.businessId?.let { dao.getBusiness(it) }?.takeUnless { it.archived }
            ?: dao.getBusinesses().firstOrNull()
            ?: return@withTransaction WorkspaceSelection(null, null)
        ensureDefaultCategories(business.id)
        val projects = dao.getProjects(business.id)
        val project = projects.firstOrNull { it.id == remembered?.projectId } ?: projects.firstOrNull()
        if (project != null) ensureBookPair(project.id)
        WorkspaceSelection(business.id, project?.id)
    }

    /**
     * Creates only the business. It no longer also inserts a general "Umum" project: a unit
     * (branch, field, season) is a real accounting scope with its own two books, and inventing one on the owner's
     * behalf is the same failure as the old starter workspace, one level down. The caller shows an
     * explicit "Tambah Unit Usaha" step instead.
     */
    suspend fun createBusiness(name: String): String = database.withTransaction {
        val cleanName = name.trim().also { require(it.isNotBlank()) { "Nama usaha wajib diisi." } }
        val now = System.currentTimeMillis()
        val business = BusinessEntity(UUID.randomUUID().toString(), cleanName, createdAtEpochMillis = now)
        dao.insertBusiness(business)
        ensureDefaultCategories(business.id)
        business.id
    }

    /** A business and its first unit in one transaction: never a business without a unit (DEC-022). */
    suspend fun createBusinessWithUnit(name: String, unitName: String): Pair<String, String> = database.withTransaction {
        val businessId = createBusiness(name)
        businessId to createProject(businessId, unitName)
    }

    suspend fun createProject(businessId: String, name: String): String = database.withTransaction {
        createProjectInternal(
            businessId = businessId,
            name = name.trim().also { require(it.isNotBlank()) { "Nama proyek wajib diisi." } },
            isGeneral = false,
        ).id
    }

    suspend fun renameBusiness(id: String, name: String) = database.withTransaction {
        val cleanName = name.trim().also { require(it.isNotBlank()) { "Nama usaha wajib diisi." } }
        val business = dao.getBusiness(id) ?: error("Usaha tidak ditemukan.")
        dao.updateBusiness(business.copy(name = cleanName, updatedAtEpochMillis = System.currentTimeMillis()))
    }

    suspend fun renameProject(id: String, name: String) = database.withTransaction {
        val cleanName = name.trim().also { require(it.isNotBlank()) { "Nama unit wajib diisi." } }
        val project = dao.getProject(id) ?: error("Unit tidak ditemukan.")
        dao.updateProject(project.copy(name = cleanName, updatedAtEpochMillis = System.currentTimeMillis()))
    }

    /**
     * Edits the whole field, not just its name.
     *
     * `description` and the two season dates were columns the schema has always carried and the UI
     * never wrote — a field could be named but not described or dated. The web equivalent
     * (`FinanceProject`) has always had a season label and a start/end date, so this closes a gap
     * rather than inventing a new concept.
     */
    suspend fun updateProjectDetails(
        id: String,
        name: String,
        description: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ) = database.withTransaction {
        val cleanName = name.trim().also { require(it.isNotBlank()) { "Nama unit wajib diisi." } }
        require(startDate == null || endDate == null || !endDate.isBefore(startDate)) {
            "Tanggal selesai tidak boleh mendahului tanggal mulai."
        }
        val project = dao.getProject(id) ?: error("Unit tidak ditemukan.")
        dao.updateProject(
            project.copy(
                name = cleanName,
                description = description.trim(),
                startDateEpochDay = startDate?.toEpochDay(),
                endDateEpochDay = endDate?.toEpochDay(),
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * Removes a scope in the only way that is safe for each case, and says which it did.
     *
     * A scope the owner created by mistake and never used carries no history, so deleting it
     * outright is what they mean by "remove" and leaves nothing behind. A scope with records is a
     * different thing: every posted journal entry hangs off a book under a field under a business,
     * and deleting those rows would tear a hole in the audit trail the product is built on — so it
     * is archived instead, and the caller tells the owner that is what happened. Deciding here
     * rather than in the UI keeps the rule in one place and means no screen has to count rows to
     * know which button to draw.
     */
    suspend fun removeProject(id: String): ScopeRemoval = database.withTransaction {
        val project = dao.getProject(id) ?: error("Unit tidak ditemukan.")
        if (projectIsEmpty(project.id)) {
            dao.deleteProject(project.id)
            ScopeRemoval.DELETED
        } else {
            dao.archiveProject(project.id, System.currentTimeMillis())
            ScopeRemoval.ARCHIVED
        }
    }

    suspend fun removeBusiness(id: String): ScopeRemoval = database.withTransaction {
        require(dao.getBusinesses().size > 1) { "Usaha terakhir tidak dapat dihapus." }
        val projects = dao.getProjects(id)
        return@withTransaction if (projects.all { projectIsEmpty(it.id) }) {
            dao.deleteBusiness(id)
            ScopeRemoval.DELETED
        } else {
            val now = System.currentTimeMillis()
            dao.archiveProjectsOfBusiness(id, now)
            dao.archiveBusiness(id, now)
            ScopeRemoval.ARCHIVED
        }
    }

    private suspend fun projectIsEmpty(projectId: String): Boolean =
        dao.countEventsForProject(projectId) == 0 && dao.countBudgetItemsForProject(projectId) == 0 &&
            stockDao.countStockItemsOfProject(projectId) == 0

    suspend fun createCategory(
        businessId: String,
        name: String,
        role: CategoryRole,
        aliases: List<String> = emptyList(),
        costNature: CostNature? = null,
    ): String = database.withTransaction {
        val cleanName = name.trim().also { require(it.isNotBlank()) { "Nama kategori wajib diisi." } }
        // (business, role, normalizedName) is unique and archiving keeps the row, so re-adding an
        // archived name — by hand or from a workbook RAB — used to die on a raw constraint error.
        dao.findCategory(businessId, role.name, normalize(cleanName))?.let { existing ->
            require(!existing.active) { "Kategori \"$cleanName\" sudah ada." }
            dao.reactivateCategory(existing.id)
            return@withTransaction existing.id
        }
        val id = UUID.randomUUID().toString()
        dao.insertCategory(
            CategoryEntity(
                id = id,
                businessId = businessId,
                role = role.name,
                name = cleanName,
                normalizedName = normalize(cleanName),
                aliases = aliases.map(String::trim).filter(String::isNotBlank).distinct().joinToString("|"),
                createdAtEpochMillis = System.currentTimeMillis(),
                // Expenses default to fixed until the owner says otherwise (overheads are the safe BEP default).
                costNature = (costNature ?: CostNature.FIXED.takeIf { role == CategoryRole.EXPENSE })?.name,
            ),
        )
        id
    }

    suspend fun archiveCategory(id: String) = database.withTransaction { dao.archiveCategory(id) }

    suspend fun createBudgetItem(draft: BudgetItemDraft): String = database.withTransaction {
        val cleanName = draft.name.trim().also { require(it.isNotBlank()) { "Nama anggaran wajib diisi." } }
        require(draft.plannedTotalRupiah == null || draft.plannedTotalRupiah > 0) { "Total anggaran harus lebih dari nol." }
        require(draft.unitPriceRupiah == null || draft.unitPriceRupiah >= 0) { "Harga satuan tidak boleh negatif." }

        val book = dao.getBookById(draft.bookId) ?: error("Buku keuangan tidak ditemukan.")
        val project = dao.getProject(book.projectId) ?: error("Proyek tidak ditemukan.")
        val category = dao.getCategory(draft.categoryId) ?: error("Kategori tidak ditemukan.")
        require(category.businessId == project.businessId) { "Kategori dan buku berasal dari usaha yang berbeda." }

        val formulaTotal = calculateFormulaTotal(draft.quantityText, draft.unitPriceRupiah)
        val authoritativeTotal = draft.plannedTotalRupiah ?: formulaTotal
            ?: error("Isi total anggaran atau jumlah dan harga satuan.")
        val difference = if (formulaTotal == null) 0 else authoritativeTotal - formulaTotal
        val id = UUID.randomUUID().toString()
        val sortOrder = dao.getBudgetItems(draft.bookId).size + 1
        dao.insertBudgetItem(
            BudgetItemEntity(
                id = id,
                bookId = draft.bookId,
                categoryId = draft.categoryId,
                name = cleanName,
                normalizedName = normalize(cleanName),
                quantityText = draft.quantityText?.trim()?.ifBlank { null },
                unit = draft.unit?.trim()?.ifBlank { null },
                unitPriceRupiah = draft.unitPriceRupiah,
                plannedTotalRupiah = authoritativeTotal,
                plannedDateEpochDay = draft.plannedDate?.toEpochDay(),
                aliases = draft.aliases.map(String::trim).filter(String::isNotBlank).distinct().joinToString("|"),
                amountSource = if (draft.plannedTotalRupiah != null) "USER_TOTAL" else "QUANTITY_X_UNIT_PRICE",
                formulaDifferenceRupiah = difference,
                sortOrder = sortOrder,
                createdAtEpochMillis = System.currentTimeMillis(),
                costNature = draft.costNature?.name,
                recurrence = draft.recurrence.name,
                usefulLifeMonths = draft.usefulLifeMonths?.also { require(it > 0) { "Umur pakai harus lebih dari nol bulan." } },
            ),
        )
        id
    }

    suspend fun updateBudgetItem(id: String, draft: BudgetItemDraft) = database.withTransaction {
        val current = dao.getBudgetItem(id) ?: error("Item anggaran tidak ditemukan.")
        require(current.bookId == draft.bookId) { "Item anggaran berasal dari buku lain." }
        val cleanName = draft.name.trim().also { require(it.isNotBlank()) { "Nama anggaran wajib diisi." } }
        val category = dao.getCategory(draft.categoryId) ?: error("Kategori tidak ditemukan.")
        val book = dao.getBookById(draft.bookId) ?: error("Buku keuangan tidak ditemukan.")
        val project = dao.getProject(book.projectId) ?: error("Proyek tidak ditemukan.")
        require(category.businessId == project.businessId) { "Kategori dan buku berasal dari usaha yang berbeda." }
        val formulaTotal = calculateFormulaTotal(draft.quantityText, draft.unitPriceRupiah)
        val authoritativeTotal = draft.plannedTotalRupiah ?: formulaTotal
            ?: error("Isi total anggaran atau jumlah dan harga satuan.")
        require(authoritativeTotal > 0) { "Total anggaran harus lebih dari nol." }
        dao.updateBudgetItem(
            current.copy(
                categoryId = draft.categoryId,
                name = cleanName,
                normalizedName = normalize(cleanName),
                quantityText = draft.quantityText?.trim()?.ifBlank { null },
                unit = draft.unit?.trim()?.ifBlank { null },
                unitPriceRupiah = draft.unitPriceRupiah,
                plannedTotalRupiah = authoritativeTotal,
                plannedDateEpochDay = draft.plannedDate?.toEpochDay(),
                aliases = (current.aliases.split('|') + draft.aliases).map(String::trim).filter(String::isNotBlank).distinct().joinToString("|"),
                amountSource = if (draft.plannedTotalRupiah != null) "USER_TOTAL" else "QUANTITY_X_UNIT_PRICE",
                formulaDifferenceRupiah = if (formulaTotal == null) 0 else authoritativeTotal - formulaTotal,
                updatedAtEpochMillis = System.currentTimeMillis(),
                costNature = draft.costNature?.name,
                recurrence = draft.recurrence.name,
                usefulLifeMonths = draft.usefulLifeMonths,
            ),
        )
    }

    suspend fun archiveBudgetItem(id: String) = database.withTransaction {
        dao.archiveBudgetItem(id, System.currentTimeMillis())
    }

    suspend fun recordEvent(draft: EventDraft): String = database.withTransaction {
        requireValid(draft)
        val event = draft.toEvent().withPurchasePrice()
        insertEventWithJournal(event)
        event.id
    }

    /**
     * Corrects a recorded transaction in place (DEC-019): the same id, import batch and creation
     * time, with a journal rebuilt from the corrected values. Only the journal references an
     * event, and it cascades, so this is one atomic swap.
     */
    suspend fun updateEvent(eventId: String, draft: EventDraft) = database.withTransaction {
        val old = dao.getEvent(eventId) ?: error("Transaksi tidak ditemukan.")
        requireValid(draft)
        val event = draft.copy(importBatchId = old.importBatchId).toEvent().withPurchasePrice().copy(id = old.id)
        dao.deleteEvent(old.id)
        insertEventAndJournal(event, LedgerPolicy.journalFor(event), createdAtEpochMillis = old.createdAtEpochMillis)
    }

    private suspend fun requireValid(draft: EventDraft) {
        require(draft.amountRupiah > 0) { "Nominal harus lebih dari nol." }
        val book = dao.getBookById(draft.bookId) ?: error("Buku keuangan tidak ditemukan.")
        val project = dao.getProject(draft.projectId) ?: error("Proyek tidak ditemukan.")
        require(book.projectId == project.id && project.businessId == draft.businessId) {
            "Lingkup usaha, proyek, dan buku tidak konsisten."
        }
        if (draft.categoryId != null) {
            val category = dao.getCategory(draft.categoryId) ?: error("Kategori tidak ditemukan.")
            require(category.businessId == draft.businessId) { "Kategori berasal dari usaha yang berbeda." }
        }
        if (draft.stockItemId != null) {
            require(draft.type == FinancialEventType.CASH_EXPENSE) { "Bahan hanya bisa dipilih untuk belanja (uang keluar)." }
            val material = stockDao.getStockItem(draft.stockItemId)
            require(material != null && material.businessId == draft.businessId) { "Bahan tidak ditemukan di usaha ini." }
            require(TransactionCalculation.parseQuantity(draft.quantityText) != null) { "Isi jumlah ${material.name} yang dibeli." }
        }
        val soldUnit = draft.unit?.trim()?.ifBlank { null }
        if (draft.productId != null && soldUnit != null) {
            val product = dao.getProduct(draft.productId) ?: error("Produk tidak ditemukan.")
            val known = listOf(product.unit) + dao.getProductUnits(product.id).map { it.name }
            require(known.any { it.equals(soldUnit, ignoreCase = true) }) {
                "\"$soldUnit\" bukan satuan jual ${product.name}. Pilih: ${known.distinct().joinToString()}."
            }
        }
    }

    /** A material purchase without a unit price takes amount ÷ quantity, so it can price the material (spec #20). */
    private fun MoneyEvent.withPurchasePrice(): MoneyEvent {
        if (stockItemId == null || unitPriceRupiah != null) return this
        val quantity = TransactionCalculation.parseQuantity(quantityText) ?: return this
        return copy(unitPriceRupiah = TransactionCalculation.derivedUnitPriceRupiah(amountRupiah, quantity))
    }

    /**
     * Materials and goods of a unit. [StockItem.basePriceRupiah] is the newest purchase's unit price
     * (spec #20, DEC-028 §2), else the price entered with the item; deleting that purchase restores it.
     */
    fun stockItems(businessId: String, projectId: String?): Flow<List<StockItem>> =
        combine(stockDao.observeStockItems(businessId, projectId), dao.observeMaterialPurchases(businessId)) { items, purchases ->
            val latest = purchases.associate { it.stockItemId to it.unitPriceRupiah }
            items.map { item -> item.toDomain().let { it.copy(basePriceRupiah = latest[it.id] ?: it.basePriceRupiah) } }
        }

    suspend fun materialPrice(stockItemId: String): Long? =
        dao.latestPurchasePrice(stockItemId) ?: stockDao.getStockItem(stockItemId)?.basePriceRupiah

    /**
     * A new item belongs to [projectId] (a branch keeps its own stock) unless [shared], in which
     * case every unit of the business sees it. Names are unique among what one unit can see.
     */
    suspend fun createStockItem(
        businessId: String,
        projectId: String?,
        shared: Boolean,
        name: String,
        unit: String,
        basePriceRupiah: Long,
        initialStock: BigDecimal,
        minAlertStock: BigDecimal,
    ): String = database.withTransaction {
        val cleanName = name.trim().also { require(it.isNotBlank()) { "Nama barang wajib diisi." } }
        val cleanUnit = unit.trim().also { require(it.isNotBlank()) { "Satuan barang wajib diisi." } }
        require(basePriceRupiah >= 0) { "Harga beli tidak boleh negatif." }
        require(initialStock.signum() >= 0 && minAlertStock.signum() >= 0) { "Jumlah stok tidak boleh negatif." }
        val owner = projectId.takeUnless { shared }
        val visible = if (owner == null) stockDao.getAllStockItems(businessId) else stockDao.getStockItems(businessId, owner)
        require(visible.none { it.name.equals(cleanName, ignoreCase = true) }) {
            "Barang \"$cleanName\" sudah ada."
        }
        val now = System.currentTimeMillis()
        val initialText = initialStock.stripTrailingZeros().toPlainString()
        val item = StockItemEntity(
            id = UUID.randomUUID().toString(),
            businessId = businessId,
            name = cleanName,
            unit = cleanUnit,
            basePriceRupiah = basePriceRupiah,
            initialStockText = initialText,
            currentStockText = initialText,
            minAlertStockText = minAlertStock.stripTrailingZeros().toPlainString(),
            createdAtEpochMillis = now,
            projectId = owner,
        )
        stockDao.insertStockItem(item)
        item.id
    }

    /** Insert-movement and update-level happen together or not at all. */
    suspend fun recordStockMovement(
        stockItemId: String,
        type: StockMovementType,
        quantity: BigDecimal,
        date: LocalDate,
        notes: String,
    ): BigDecimal = database.withTransaction {
        RecordStockMovementUseCase(stockDao).execute(stockItemId, type, quantity, date, notes).getOrThrow()
    }

    /**
     * "Hapus semua data" (DEC-020): every user table emptied in one transaction, children first.
     * Change capture is off while deleting, then the outbox and sync state are cleared and the
     * device gets a new sync identity, so nothing of the old data is left to push anywhere.
     */
    suspend fun eraseAllData() = database.withTransaction {
        val db = database.openHelper.writableDatabase
        SyncSchema.quietly(db) {
            AppDatabase.DATA_TABLES.asReversed().forEach { table -> db.execSQL("DELETE FROM `$table`") }
        }
        db.execSQL("DELETE FROM sync_outbox")
        db.execSQL("DELETE FROM sync_meta")
        SyncSchema.ensureDeviceId(db)
    }

    /**
     * Hard deletes a transaction (D-042, DEC-019). Journal and postings follow by CASCADE.
     */
    suspend fun deleteEvent(eventId: String) = database.withTransaction {
        dao.deleteEvent(eventId)
    }

    /**
     * What committing [preview] into [businessId] would do that the preview alone cannot know:
     * which categories are new, which rows clash with an existing category's role, which stock
     * names already exist, and whether this exact file was imported before (allowed, DEC-012).
     */
    suspend fun reviewImport(businessId: String, projectId: String, preview: WorkbookImportPreview): ImportReview = database.withTransaction {
        val plan = planCategories(businessId, preview)
        val existingStock = stockDao.getStockItems(businessId, projectId).map { normalize(it.name) }.toSet()
        val products = importProductPlan(businessId, projectId, preview)
        ImportReview(
            newCategories = plan.newCategories.map { it.name to it.role },
            conflicts = plan.conflicts + products.conflicts,
            existingStockNames = preview.stockItems.filter { normalize(it.name) in existingStock }.map { it.name },
            previouslyImported = dao.hasImported(preview.sourceSha256),
            existingProductNames = products.skipped,
            addedRowCount = preview.rowCount - preview.stockItems.size + addableStock(businessId, projectId, preview).size - products.skipped.size,
        )
    }

    /**
     * Commits a template import into the selected unit (DEC-012): transactions to Realisasi, RAB
     * lines to Proyeksi, products with recipes to the unit, stock items to the business. One transaction,
     * one undoable batch, one notice saying what was added.
     */
    suspend fun commitImport(businessId: String, projectId: String, preview: WorkbookImportPreview): Int = database.withTransaction {
        require(preview.canCommit) { "Berkas impor belum bisa disimpan." }
        val plan = planCategories(businessId, preview)
        val productPlan = importProductPlan(businessId, projectId, preview)
        (plan.conflicts + productPlan.conflicts).let { conflicts -> require(conflicts.isEmpty()) { conflicts.joinToString("\n") } }
        val books = ensureBookPair(projectId)
        val actual = books.getValue(BookMode.REALIZATION)
        val planBook = books.getValue(BookMode.PROJECTION)
        val batchId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        // The file's plan period fills a unit that has none; a unit's own period is never overwritten.
        val project = requireNotNull(dao.getProject(projectId))
        val periodApplied = preview.planPeriod?.takeIf { project.startDateEpochDay == null || project.endDateEpochDay == null }
        val addedStock = addableStock(businessId, projectId, preview)
        // What the batch, its notice and the caller report: skipped rows were not added.
        val addedRows = preview.rowCount - preview.stockItems.size + addedStock.size - productPlan.skipped.size
        periodApplied?.let { (start, end) ->
            dao.updateProject(
                project.copy(startDateEpochDay = start.atDay(1).toEpochDay(), endDateEpochDay = end.atEndOfMonth().toEpochDay(), updatedAtEpochMillis = now),
            )
        }
        dao.insertImportBatch(
            ImportBatchEntity(
                id = batchId,
                sourceName = preview.sourceName,
                sourceSha256 = preview.sourceSha256,
                importerVersion = "template-${MankeuTemplate.VERSION}",
                state = "COMMITTED",
                committedAtEpochMillis = now,
                businessId = businessId,
                projectId = projectId,
                bookId = actual.id,
                mappingVersion = MankeuTemplate.MARKER,
                recordCount = addedRows,
                totalAmountRupiah = preview.transactions.sumOf { it.amountRupiah },
                incomingRupiah = preview.totalIncomeRupiah,
                appliedStartEpochDay = periodApplied?.first?.atDay(1)?.toEpochDay(),
                appliedEndEpochDay = periodApplied?.second?.atEndOfMonth()?.toEpochDay(),
            ),
        )
        val categories = plan.newCategories.associate { draft ->
            draft.normalized to requireNotNull(dao.getCategory(createCategory(businessId, draft.name, draft.role)))
        } + plan.existing
        val materialIds = productPlan.materials.toMutableMap()
        addedStock.forEach { item ->
            val stockId = UUID.randomUUID().toString()
            materialIds[normalize(item.name)] = stockId
            stockDao.insertStockItem(
                StockItemEntity(
                    id = stockId,
                    businessId = businessId,
                    name = item.name.trim(),
                    unit = item.unit.trim(),
                    basePriceRupiah = item.priceRupiah,
                    initialStockText = item.quantityText,
                    currentStockText = item.quantityText,
                    minAlertStockText = item.minAlertText,
                    createdAtEpochMillis = now,
                    importBatchId = batchId,
                    // Import is bulk entry into the selected unit (DEC-012), stock included.
                    projectId = projectId,
                    archived = item.archived,
                ),
            )
        }
        val productIds = productPlan.existing.toMutableMap()
        productPlan.added.forEachIndexed { index, item ->
            val productId = UUID.randomUUID().toString()
            dao.insertProduct(
                ProductEntity(
                    id = productId,
                    projectId = projectId,
                    name = item.name.trim(),
                    unit = item.baseUnit.trim(),
                    batchYieldText = item.batchYieldText,
                    targetPerMonthText = item.targetPerMonthText,
                    sellFromMonth = item.sellFromMonth,
                    sortOrder = productIds.size + index,
                    createdAtEpochMillis = now,
                    importBatchId = batchId,
                ),
            )
            dao.insertProductUnits(
                item.units.mapIndexed { i, u ->
                    ProductUnitEntity(UUID.randomUUID().toString(), productId, u.name.trim(), u.contentText, u.priceRupiah, i)
                },
            )
            dao.insertRecipeLines(
                item.recipe.mapIndexed { i, line ->
                    ProductRecipeLineEntity(
                        id = UUID.randomUUID().toString(),
                        productId = productId,
                        name = line.materialName?.trim() ?: line.name.trim(),
                        stockItemId = line.materialName?.let { materialIds.getValue(normalize(it)) },
                        quantityText = line.quantityText,
                        amountRupiah = line.amountRupiah,
                        sortOrder = i,
                    )
                },
            )
            productIds[normalize(item.name)] = productId
        }

        preview.transactions.forEach { row ->
            val category = categories.getValue(normalize(row.categoryName))
            insertEventWithJournal(
                MoneyEvent(
                    id = UUID.randomUUID().toString(),
                    businessId = businessId,
                    projectId = projectId,
                    bookId = actual.id,
                    type = requireNotNull(CategoryRole.parse(category.role).cashTypeFor(row.incoming)),
                    amountRupiah = row.amountRupiah,
                    date = row.date,
                    description = row.description,
                    category = category.name,
                    categoryId = category.id,
                    importBatchId = batchId,
                    quantityText = row.quantityText,
                    costNature = row.costNature,
                    productId = row.productName?.let { productIds.getValue(normalize(it)) },
                    usefulLifeMonths = row.usefulLifeMonths,
                    unit = row.unit,
                    stockItemId = row.materialName?.let { materialIds.getValue(normalize(it)) },
                    unitPriceRupiah = row.unitPriceRupiah,
                ).withPurchasePrice(),
            )
        }
        // Production and counts of the added products, after the transactions: on one date a row from
        // Produksi & Stok counts after that day's sales, in file order (DEC-028 §9). Undo removes them
        // with their product (cascade); rows of a product skipped by name are not imported.
        val movementsFrom = System.currentTimeMillis() + 1
        val addedMovements = productPlan.added.flatMap { p -> p.movements.map { productIds.getValue(normalize(p.name)) to it } }
        addedMovements.forEachIndexed { index, (productId, m) ->
            dao.insertProductMovement(
                ProductMovementEntity(
                    id = UUID.randomUUID().toString(),
                    productId = productId,
                    movementType = m.type.name,
                    quantityText = m.quantityText,
                    batchesText = m.batchesText,
                    dateEpochDay = m.date.toEpochDay(),
                    notes = m.notes,
                    createdAtEpochMillis = movementsFrom + index,
                ),
            )
        }
        var sortOrder = dao.getBudgetItems(planBook.id).size
        preview.budgetLines.forEach { line ->
            val category = categories.getValue(normalize(line.categoryName))
            dao.insertBudgetItem(
                BudgetItemEntity(
                    id = UUID.randomUUID().toString(),
                    bookId = planBook.id,
                    categoryId = category.id,
                    name = line.name,
                    normalizedName = normalize(line.name),
                    quantityText = line.quantityText,
                    unit = line.unit,
                    unitPriceRupiah = line.unitPriceRupiah,
                    plannedTotalRupiah = line.totalRupiah,
                    plannedDateEpochDay = line.month?.atDay(1)?.toEpochDay(),
                    sortOrder = ++sortOrder,
                    importBatchId = batchId,
                    createdAtEpochMillis = now,
                    costNature = line.costNature?.name,
                    recurrence = line.recurrence.name,
                    usefulLifeMonths = line.usefulLifeMonths,
                ),
            )
        }
        dao.insertNotice(
            ImportNoticeEntity(
                id = UUID.randomUUID().toString(),
                batchId = batchId,
                severity = "IMPORT",
                title = "Impor ${preview.sourceName} tersimpan",
                explanation = buildList {
                    add("${preview.transactions.size} transaksi, ${preview.budgetLines.size} baris rencana anggaran, ${productPlan.added.size} produk, ${addedMovements.size} catatan produksi & hitung stok, ${addedStock.size} barang stok.")
                    if (productPlan.skipped.isNotEmpty()) add("Produk dilewati karena namanya sudah ada, beserta produksi dan hitung stoknya: ${productPlan.skipped.joinToString()}.")
                    preview.planPeriod?.let { (start, end) ->
                        add(
                            if (periodApplied != null) "Periode rencana diatur $start s.d. $end."
                            else "Periode rencana di berkas ($start s.d. $end) tidak dipakai karena unit ini sudah punya periode sendiri.",
                        )
                    }
                    if (plan.newCategories.isNotEmpty()) add("Kategori baru: ${plan.newCategories.joinToString { it.name }}.")
                    val skipped = preview.stockItems.size - addedStock.size
                    if (skipped > 0) add("$skipped barang dilewati karena namanya sudah ada.")
                }.joinToString(" "),
                amountRupiah = preview.transactions.sumOf { it.amountRupiah },
                sourceEvidence = preview.sourceName,
            ),
        )
        addedRows
    }

    /** Stock rows that would be added: names the unit cannot see yet, first occurrence in the file only. */
    private suspend fun addableStock(businessId: String, projectId: String, preview: WorkbookImportPreview): List<ImportStockItem> {
        val seen = stockDao.getStockItems(businessId, projectId).map { normalize(it.name) }.toMutableSet()
        return preview.stockItems.filter { seen.add(normalize(it.name)) }
    }

    private data class ProductPlan(
        /** Normalized name → id, for products already in the unit. */
        val existing: Map<String, String>,
        /** Normalized name → id, for materials the unit already sees. */
        val materials: Map<String, String>,
        val added: List<ImportProduct>,
        val skipped: List<String>,
        val conflicts: List<String>,
    )

    /**
     * Products in the file are added unless the unit already has one of that name; sales may name either.
     * A material named by a recipe or a purchase must be in the file's Stok Barang or already in the
     * unit; a sale unit must be one of the product's own (an existing product keeps its units).
     */
    private suspend fun importProductPlan(businessId: String, projectId: String, preview: WorkbookImportPreview): ProductPlan {
        val existingProducts = dao.getProducts(projectId)
        val existing = existingProducts.associate { normalize(it.name) to it.id }
        val seen = existing.keys.toMutableSet()
        val added = preview.products.filter { seen.add(normalize(it.name)) }
        val known = existing.keys + added.map { normalize(it.name) }
        val materials = stockDao.getStockItems(businessId, projectId).associate { normalize(it.name) to it.id }
        val materialNames = materials.keys + preview.stockItems.map { normalize(it.name) }
        val unitsOf = added.associate { normalize(it.name) to listOf(it.baseUnit) + it.units.map(com.mankeu.importer.ImportSaleUnit::name) } +
            existingProducts.associate { it.name.let(::normalize) to listOf(it.unit) + dao.getProductUnits(it.id).map { u -> u.name } }
        val conflicts = buildList {
            preview.transactions.forEach { row ->
                if (row.productName != null && normalize(row.productName) !in known) {
                    add("${row.source}: produk \"${row.productName}\" tidak ada di unit usaha ini maupun di lembar Produk.")
                } else if (row.productName != null && row.unit != null && unitsOf[normalize(row.productName)].orEmpty().none { normalize(it) == normalize(row.unit) }) {
                    add("${row.source}: \"${row.unit}\" bukan satuan jual ${row.productName}.")
                }
                if (row.materialName != null && normalize(row.materialName) !in materialNames) add(missingMaterial(row.source, row.materialName))
                if (row.materialName != null && row.incoming) add("${row.source}: kolom Bahan hanya untuk Pengeluaran.")
                if (row.materialName != null && row.quantityText == null) add("${row.source}: isi Jumlah untuk belanja bahan \"${row.materialName}\".")
            }
            added.forEach { p -> p.recipe.forEach { l -> if (l.materialName != null && normalize(l.materialName) !in materialNames) add(missingMaterial(l.source, l.materialName)) } }
        }
        return ProductPlan(existing, materials, added, (preview.products - added.toSet()).map { it.name }, conflicts)
    }

    private fun missingMaterial(source: String, name: String) =
        "$source: bahan \"$name\" tidak ada di lembar Stok Barang maupun di usaha ini."

    private data class CategoryDraft(val name: String, val normalized: String, val role: CategoryRole)
    private data class CategoryPlan(
        val existing: Map<String, CategoryEntity>,
        val newCategories: List<CategoryDraft>,
        val conflicts: List<String>,
    )

    /**
     * Resolves every category name in [preview] against the business. A new name takes the role of
     * the column it first appears in (Pemasukan → REVENUE, Pengeluaran/RAB → EXPENSE); a row whose
     * money direction does not fit its category's role is a conflict, named by sheet and cell.
     */
    private suspend fun planCategories(businessId: String, preview: WorkbookImportPreview): CategoryPlan {
        val existing = dao.getCategories(businessId).filter(CategoryEntity::active).associateBy { it.normalizedName }
        val drafts = linkedMapOf<String, CategoryDraft>()
        val conflicts = mutableListOf<String>()
        val basicRoles = DefaultMasterData.categories.associate { normalize(it.name) to it.role }
        fun roleOf(name: String, fallback: CategoryRole): CategoryRole {
            val key = normalize(name)
            existing[key]?.let { return CategoryRole.parse(it.role) }
            // A basic category name means what the template says it means ("Ambil untuk Pribadi
            // (Prive)" is a drawing even though it sits in the Pengeluaran column).
            return drafts.getOrPut(key) { CategoryDraft(name.trim(), key, basicRoles[key] ?: fallback) }.role
        }
        preview.transactions.forEach { row ->
            val role = roleOf(row.categoryName, if (row.incoming) CategoryRole.REVENUE else CategoryRole.EXPENSE)
            if (role.cashTypeFor(row.incoming) == null) {
                conflicts += "${row.source}: kategori \"${row.categoryName}\" bukan untuk ${if (row.incoming) "Pemasukan" else "Pengeluaran"}."
            }
        }
        preview.budgetLines.forEach { roleOf(it.categoryName, CategoryRole.EXPENSE) }
        return CategoryPlan(existing, drafts.values.toList(), conflicts)
    }

    /**
     * Undoes an import (DEC-019): its transactions are deleted (journals cascade), its plan lines
     * archived, its stock and products removed. The batch row stays as history, marked undone.
     */
    suspend fun undoImportBatch(batchId: String): ImportUndo = database.withTransaction {
        val deletedEvents = dao.deleteEventsForImportBatch(batchId)
        val now = System.currentTimeMillis()
        val archivedBudgets = dao.getBudgetItemsForImportBatch(batchId)
        archivedBudgets.forEach { item -> dao.archiveBudgetItem(item.id, now) }
        val removedStock = stockDao.deleteImportedItems(batchId)
        val removedProducts = dao.deleteImportedProducts(batchId)
        // Undo the plan period the import filled in, unless the owner has changed it since.
        val periodCleared = dao.getImportBatch(batchId)?.takeIf { it.appliedStartEpochDay != null }?.let { batch ->
            val unit = batch.projectId?.let { dao.getProject(it) }
            (unit != null && unit.startDateEpochDay == batch.appliedStartEpochDay && unit.endDateEpochDay == batch.appliedEndEpochDay).also { matches ->
                if (matches) dao.updateProject(unit!!.copy(startDateEpochDay = null, endDateEpochDay = null, updatedAtEpochMillis = now))
            }
        } ?: false
        check(dao.markImportBatchUndone(batchId, now) == 1) {
            "Impor ini sudah dibatalkan."
        }
        ImportUndo(deletedEvents + archivedBudgets.size + removedStock + removedProducts, periodCleared)
    }

    private suspend fun createProjectInternal(businessId: String, name: String, isGeneral: Boolean): ProjectEntity {
        val now = System.currentTimeMillis()
        val project = ProjectEntity(
            id = UUID.randomUUID().toString(),
            businessId = businessId,
            name = name,
            isGeneral = isGeneral,
            createdAtEpochMillis = now,
        )
        dao.insertProject(project)
        ensureBookPair(project.id)
        return project
    }

    private suspend fun ensureBookPair(projectId: String): Map<BookMode, BookEntity> {
        val existing = dao.getBooks(projectId).associateBy { BookMode.valueOf(it.mode) }.toMutableMap()
        val now = System.currentTimeMillis()
        val missing = BookMode.entries.filterNot(existing::containsKey).map { mode ->
            BookEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                mode = mode.name,
                createdAtEpochMillis = now,
            )
        }
        if (missing.isNotEmpty()) dao.insertBooks(missing)
        missing.forEach { existing[BookMode.valueOf(it.mode)] = it }
        return existing
    }

    /**
     * A new business gets the whole basic list. An existing one only gets a default for each
     * non-operating role it has no active category for: a business created before DEC-011 has no
     * "Prive" or asset category, and without one the owner cannot record those at all.
     */
    private suspend fun ensureDefaultCategories(businessId: String) {
        val existing = dao.getCategories(businessId)
        val activeRoles = existing.filter { it.active }.map { CategoryRole.parse(it.role) }.toSet()
        val missing = if (existing.isEmpty()) {
            DefaultMasterData.categories
        } else {
            DefaultMasterData.categories.filter {
                it.role in NON_OPERATING_ROLES && it.role !in activeRoles &&
                    dao.findCategory(businessId, it.role.name, normalize(it.name)) == null
            }
        }
        val now = System.currentTimeMillis()
        missing.forEach { category ->
            dao.insertCategory(
                CategoryEntity(
                    id = UUID.randomUUID().toString(),
                    businessId = businessId,
                    role = category.role.name,
                    name = category.name,
                    normalizedName = normalize(category.name),
                    templateOrigin = "DEFAULT",
                    createdAtEpochMillis = now,
                    costNature = category.costNature?.name,
                ),
            )
        }
    }

    private suspend fun insertEventWithJournal(event: MoneyEvent) {
        insertEventAndJournal(event, LedgerPolicy.journalFor(event))
    }

    private suspend fun insertEventAndJournal(event: MoneyEvent, journal: JournalEntry, createdAtEpochMillis: Long = System.currentTimeMillis()) {
        // Fail fast, at the last point before the rows are written.
        //
        // `JournalEntry.requireBalanced()` exists and every construction path is supposed to call
        // it, but "supposed to" is not a guarantee: this is the single funnel every posted event
        // goes through, and an unbalanced pair written here is silent, permanent, and only shows
        // up much later as a report that does not add up. The whole call is inside
        // `database.withTransaction`, so throwing here rolls back the event with it.
        val debitTotal = journal.postings.filter { it.side == PostingSide.DEBIT }.sumOf { it.amountRupiah }
        val creditTotal = journal.postings.filter { it.side == PostingSide.CREDIT }.sumOf { it.amountRupiah }
        require(journal.postings.isNotEmpty()) {
            "Jurnal untuk \"${event.description}\" tidak memiliki posting."
        }
        require(journal.postings.all { it.amountRupiah > 0 }) {
            "Setiap posting harus bernilai rupiah bulat positif."
        }
        require(debitTotal == creditTotal) {
            "Jurnal tidak seimbang untuk \"${event.description}\": debit Rp$debitTotal berbeda dari kredit Rp$creditTotal."
        }

        dao.insertEvent(event.toEntity(createdAtEpochMillis))
        dao.insertJournal(
            JournalEntryEntity(
                id = journal.id,
                sourceEventId = journal.sourceEventId,
                dateEpochDay = journal.date.toEpochDay(),
                description = journal.description,
            ),
        )
        dao.insertPostings(journal.postings.mapIndexed { index, posting ->
            PostingEntity(
                id = "${journal.id}-$index",
                journalId = journal.id,
                account = posting.account.name,
                side = posting.side.name,
                amountRupiah = posting.amountRupiah,
            )
        })
    }

    private fun EventDraft.toEvent(): MoneyEvent = MoneyEvent(
        id = UUID.randomUUID().toString(),
        businessId = businessId,
        projectId = projectId,
        bookId = bookId,
        type = type,
        amountRupiah = amountRupiah,
        date = date,
        // A blank description takes the category's name, so "jualan hari ini" can be one tap (2026-10-22).
        description = description.trim().ifBlank { category.trim().ifBlank { defaultCategory(type) } },
        category = category.trim().ifBlank { defaultCategory(type) },
        categoryId = categoryId,
        reference = reference?.trim()?.ifBlank { null },
        quantityText = quantityText?.trim()?.ifBlank { null },
        unit = unit?.trim()?.ifBlank { null },
        unitPriceRupiah = unitPriceRupiah,
        budgetItemId = budgetItemId,
        importBatchId = importBatchId,
        costNature = costNature,
        productId = productId,
        usefulLifeMonths = usefulLifeMonths,
        stockItemId = stockItemId,
    )

    private fun FinancialEventEntity.toDomain() = MoneyEvent(
        id = id,
        businessId = businessId,
        projectId = projectId,
        bookId = bookId,
        type = FinancialEventType.valueOf(type),
        amountRupiah = amountRupiah,
        date = LocalDate.ofEpochDay(dateEpochDay),
        description = description,
        category = category,
        categoryId = categoryId,
        reference = reference,
        quantityText = quantityText,
        unit = unit,
        unitPriceRupiah = unitPriceRupiah,
        budgetItemId = budgetItemId,
        importBatchId = importBatchId,
        cashFlowRole = CashFlowRole.valueOf(cashFlowRole),
        costNature = PlanAssembly.nature(costNature),
        productId = productId,
        usefulLifeMonths = usefulLifeMonths,
        stockItemId = stockItemId,
    )

    private fun MoneyEvent.toEntity(createdAtEpochMillis: Long = System.currentTimeMillis()) = FinancialEventEntity(
        id = id,
        businessId = businessId,
        projectId = projectId,
        bookId = bookId,
        type = type.name,
        amountRupiah = amountRupiah,
        dateEpochDay = date.toEpochDay(),
        description = description,
        category = category,
        categoryId = categoryId,
        reference = reference,
        quantityText = quantityText,
        unit = unit,
        unitPriceRupiah = unitPriceRupiah,
        budgetItemId = budgetItemId,
        importBatchId = importBatchId,
        cashFlowRole = cashFlowRole.name,
        createdAtEpochMillis = createdAtEpochMillis,
        costNature = costNature?.name,
        productId = productId,
        usefulLifeMonths = usefulLifeMonths,
        stockItemId = stockItemId,
    )

    private fun calculateFormulaTotal(quantityText: String?, unitPriceRupiah: Long?): Long? {
        if (quantityText.isNullOrBlank() || unitPriceRupiah == null || unitPriceRupiah <= 0) return null
        val quantity = TransactionCalculation.parseQuantity(quantityText) ?: error("Jumlah harus berupa angka yang valid dan lebih dari nol.")
        return TransactionCalculation.computeTotalRupiah(quantity, unitPriceRupiah)
    }

    private fun defaultCategory(type: FinancialEventType) = when (type) {
        FinancialEventType.CASH_INCOME -> "Pendapatan"
        FinancialEventType.CASH_EXPENSE -> "Operasional"
        FinancialEventType.ASSET_PURCHASE_CASH -> "Peralatan dan aset"
        FinancialEventType.OWNER_CAPITAL -> "Modal pemilik"
        FinancialEventType.OWNER_DRAWING -> "Pengambilan pemilik"
    }

    private fun normalize(value: String): String = value.lowercase(Locale.forLanguageTag("id-ID"))
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()


    companion object {
        /**
         * How many ledger rows a screen keeps in memory. Two hundred is several screens of
         * scrolling and a small fraction of a busy book; every reported total is computed by the
         * database or from a narrow projection, so this bounds memory without bounding accuracy.
         */
        const val RECENT_EVENT_LIMIT = 200

        private val NON_OPERATING_ROLES = setOf(CategoryRole.CAPITAL_IN, CategoryRole.DRAWING, CategoryRole.ASSET)
    }
}

/** What undoing an import batch removed: [records] rows, and whether the unit's plan period was cleared. */
data class ImportUndo(val records: Int, val periodCleared: Boolean)
