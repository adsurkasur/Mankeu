package com.mankeu.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Just enough of an event to compute a cash-flow schedule.
 *
 * The screens used to derive the monthly chart from the full `List<MoneyEvent>`, which carries a
 * description, a category name and a reference for every row. A book with a few
 * thousand transactions therefore rebuilt several megabytes of strings on every database change,
 * to add up six numbers per month. These five columns are all the arithmetic ever touched.
 * Since the 2026-10-22 audit each row is already a month's total per type and role (summed by the
 * database), so a year is a few dozen rows however busy the book is.
 */
data class CashFlowRow(
    /** One of the month's events (the query keeps MIN(id)); it only names the row. */
    val id: String,
    val dateEpochDay: Long,
    val type: String,
    val cashFlowRole: String,
    val amountRupiah: Long,
)

/** One slice of the cost-composition chart, summed by the database rather than in memory. */
data class CategoryTotalRow(val category: String, val totalRupiah: Long)

/** See [FinanceDao.observeCostRows]. */
data class CostRow(
    val id: String,
    val type: String,
    val dateEpochDay: Long,
    val amountRupiah: Long,
    val costNature: String?,
    val productId: String?,
    val quantityText: String?,
    val unit: String?,
    val usefulLifeMonths: Int?,
)

/** A sale of a product as the stock count reads it; see [FinanceDao.observeProductSales]. */
data class ProductSaleRow(
    val id: String,
    val productId: String,
    val quantityText: String?,
    val unit: String?,
    val dateEpochDay: Long,
    val createdAtEpochMillis: Long,
)

/** A purchase that names a material; the newest one prices it (DEC-028 §2). */
data class MaterialPurchaseRow(val stockItemId: String, val unitPriceRupiah: Long)

/** Posting total per (cash-flow role, account, side); see [FinanceDao.observePostingTotals]. */
data class PostingTotalRow(val cashFlowRole: String, val account: String, val side: String, val amountRupiah: Long)

data class PostingWithEventRow(
    val eventId: String,
    val cashFlowRole: String,
    val account: String,
    val side: String,
    val amountRupiah: Long,
)

@Dao
interface FinanceDao {
    @Query("SELECT * FROM businesses WHERE archived = 0 ORDER BY createdAtEpochMillis")
    fun observeBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE archived = 0 ORDER BY createdAtEpochMillis")
    suspend fun getBusinesses(): List<BusinessEntity>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    suspend fun getBusiness(id: String): BusinessEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBusiness(business: BusinessEntity)

    @Update
    suspend fun updateBusiness(business: BusinessEntity)

    @Query("SELECT COUNT(*) FROM financial_events WHERE projectId = :projectId")
    suspend fun countEventsForProject(projectId: String): Int

    @Query("SELECT COUNT(*) FROM budget_items WHERE bookId IN (SELECT id FROM books WHERE projectId = :projectId)")
    suspend fun countBudgetItemsForProject(projectId: String): Int

    /** Books, budget items, events and journals hang off these by CASCADE. */
    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)

    @Query("DELETE FROM businesses WHERE id = :id")
    suspend fun deleteBusiness(id: String)

    @Query("UPDATE businesses SET archived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun archiveBusiness(id: String, updatedAt: Long)

    /** Archiving a business takes its fields with it, so neither can be reached without the other. */
    @Query("UPDATE projects SET archived = 1, updatedAtEpochMillis = :updatedAt WHERE businessId = :businessId")
    suspend fun archiveProjectsOfBusiness(businessId: String, updatedAt: Long)

    @Query("UPDATE projects SET archived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun archiveProject(id: String, updatedAt: Long)

    @Query("SELECT * FROM projects WHERE businessId = :businessId AND archived = 0 ORDER BY isGeneral DESC, createdAtEpochMillis")
    fun observeProjects(businessId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE businessId = :businessId AND archived = 0 ORDER BY isGeneral DESC, createdAtEpochMillis")
    suspend fun getProjects(businessId: String): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProject(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBooks(books: List<BookEntity>)

    @Query("SELECT * FROM books WHERE projectId = :projectId ORDER BY mode")
    fun observeBooks(projectId: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE projectId = :projectId ORDER BY mode")
    suspend fun getBooks(projectId: String): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("UPDATE categories SET active = 0 WHERE id = :id")
    suspend fun archiveCategory(id: String)

    @Query("UPDATE categories SET active = 1 WHERE id = :id")
    suspend fun reactivateCategory(id: String)

    /** Archived rows included: they still hold the (business, role, name) unique key. */
    @Query("SELECT * FROM categories WHERE businessId = :businessId AND role = :role AND normalizedName = :normalizedName LIMIT 1")
    suspend fun findCategory(businessId: String, role: String, normalizedName: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE businessId = :businessId AND active = 1 ORDER BY role, name COLLATE NOCASE")
    fun observeCategories(businessId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE businessId = :businessId AND active = 1 ORDER BY role, name COLLATE NOCASE")
    suspend fun getCategories(businessId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategory(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBudgetItem(item: BudgetItemEntity)

    @Update
    suspend fun updateBudgetItem(item: BudgetItemEntity)

    @Query("SELECT * FROM budget_items WHERE id = :id LIMIT 1")
    suspend fun getBudgetItem(id: String): BudgetItemEntity?

    @Query("SELECT * FROM budget_items WHERE bookId = :bookId AND archived = 0 ORDER BY sortOrder, createdAtEpochMillis")
    fun observeBudgetItems(bookId: String): Flow<List<BudgetItemEntity>>

    @Query("SELECT * FROM budget_items WHERE bookId = :bookId AND archived = 0 ORDER BY sortOrder, createdAtEpochMillis")
    suspend fun getBudgetItems(bookId: String): List<BudgetItemEntity>

    @Query("UPDATE budget_items SET archived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun archiveBudgetItem(id: String, updatedAt: Long)

    @Query("SELECT * FROM budget_items WHERE importBatchId = :batchId AND archived = 0")
    suspend fun getBudgetItemsForImportBatch(batchId: String): List<BudgetItemEntity>

    /**
     * The most recent [limit] rows, newest first — what Beranda and Catatan actually render.
     *
     * Unbounded before: every screen observing a book pulled its entire history into memory just
     * to show the newest handful and to filter a list nobody scrolls to the end of. Aggregates do
     * not come from here (see [observeCashFlowRows] and [observeCategoryTotals]), so limiting the
     * list cannot change a reported figure — only how much of the ledger is on screen at once.
     */
    @Query(
        """
        SELECT * FROM financial_events
        WHERE bookId = :bookId
        ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC
        LIMIT :limit
        """,
    )
    fun observeEventsForBook(bookId: String, limit: Int): Flow<List<FinancialEventEntity>>

    @Query("SELECT COUNT(*) FROM financial_events WHERE bookId = :bookId")
    fun observeEventCount(bookId: String): Flow<Int>

    @Query(
        """
        SELECT MIN(id) AS id, MIN(dateEpochDay) AS dateEpochDay, type, cashFlowRole, SUM(amountRupiah) AS amountRupiah
        FROM financial_events
        WHERE bookId = :bookId
        GROUP BY strftime('%Y-%m', dateEpochDay * 86400, 'unixepoch'), type, cashFlowRole
        """,
    )
    fun observeCashFlowRows(bookId: String): Flow<List<CashFlowRow>>

    /**
     * Expense totals per category. Grouping in SQL keeps the chart correct on a book of any size without loading it.
     */
    @Query(
        """
        SELECT category, SUM(amountRupiah) AS totalRupiah
        FROM financial_events
        WHERE bookId = :bookId
          AND type = 'CASH_EXPENSE'
        GROUP BY category
        HAVING SUM(amountRupiah) > 0
        ORDER BY totalRupiah DESC
        """,
    )
    fun observeCategoryTotals(bookId: String): Flow<List<CategoryTotalRow>>

    @Query("SELECT * FROM financial_events WHERE bookId = :bookId ORDER BY dateEpochDay, createdAtEpochMillis")
    suspend fun getEventsForBook(bookId: String): List<FinancialEventEntity>

    @Query("SELECT * FROM financial_events WHERE id = :id")
    suspend fun getEvent(id: String): FinancialEventEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(event: FinancialEventEntity)

    @Query("DELETE FROM financial_events WHERE id = :id")
    suspend fun deleteEvent(id: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournal(entry: JournalEntryEntity)

    @Query("SELECT * FROM journal_entries WHERE sourceEventId = :eventId LIMIT 1")
    suspend fun getJournalForEvent(eventId: String): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPostings(postings: List<PostingEntity>)

    @Query("SELECT * FROM postings WHERE journalId = :journalId ORDER BY id")
    suspend fun getPostings(journalId: String): List<PostingEntity>

    /**
     * Posting totals per (cash-flow role, account, side): all `PostingReportCalculator` ever sums.
     * Returning every posting instead put a year of a medium business (tens of thousands of rows)
     * into memory on each database change, twice, for six numbers (audit 2026-10-22).
     */
    @Query(
        """
        SELECT e.cashFlowRole, p.account, p.side, SUM(p.amountRupiah) AS amountRupiah
        FROM financial_events e
        INNER JOIN journal_entries j ON j.sourceEventId = e.id
        INNER JOIN postings p ON p.journalId = j.id
        WHERE e.bookId = :bookId
        GROUP BY e.cashFlowRole, p.account, p.side
        """,
    )
    fun observePostingTotals(bookId: String): Flow<List<PostingTotalRow>>

    @Query(
        """
        SELECT e.id AS eventId, e.cashFlowRole, p.account, p.side, p.amountRupiah
        FROM financial_events e
        INNER JOIN journal_entries j ON j.sourceEventId = e.id
        INNER JOIN postings p ON p.journalId = j.id
        WHERE e.bookId = :bookId
        ORDER BY e.dateEpochDay, e.createdAtEpochMillis, p.id
        """,
    )
    suspend fun getPostingRows(bookId: String): List<PostingWithEventRow>

    // ---- Products & recipes (DEC-014) -------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProduct(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE projectId = :projectId AND archived = 0 ORDER BY sortOrder, createdAtEpochMillis")
    fun observeProducts(projectId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE projectId = :projectId AND archived = 0 ORDER BY sortOrder, createdAtEpochMillis")
    suspend fun getProducts(projectId: String): List<ProductEntity>

    @Query("UPDATE products SET archived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun archiveProduct(id: String, updatedAt: Long)

    @Query("DELETE FROM products WHERE importBatchId = :batchId")
    suspend fun deleteImportedProducts(batchId: String): Int

    @Query(
        """
        SELECT u.* FROM product_units u INNER JOIN products p ON p.id = u.productId
        WHERE p.projectId = :projectId AND p.archived = 0 ORDER BY u.productId, u.sortOrder
        """,
    )
    fun observeProductUnits(projectId: String): Flow<List<ProductUnitEntity>>

    @Query("SELECT * FROM product_units WHERE productId = :productId ORDER BY sortOrder")
    suspend fun getProductUnits(productId: String): List<ProductUnitEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProductUnits(units: List<ProductUnitEntity>)

    @Query("DELETE FROM product_units WHERE productId = :productId")
    suspend fun deleteProductUnits(productId: String)

    @Query(
        """
        SELECT r.* FROM product_recipe_lines r INNER JOIN products p ON p.id = r.productId
        WHERE p.projectId = :projectId AND p.archived = 0 ORDER BY r.productId, r.sortOrder
        """,
    )
    fun observeRecipeLines(projectId: String): Flow<List<ProductRecipeLineEntity>>

    @Query("SELECT * FROM product_recipe_lines WHERE productId = :productId ORDER BY sortOrder")
    suspend fun getRecipeLines(productId: String): List<ProductRecipeLineEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecipeLines(lines: List<ProductRecipeLineEntity>)

    @Query("DELETE FROM product_recipe_lines WHERE productId = :productId")
    suspend fun deleteRecipeLines(productId: String)

    @Query(
        """
        SELECT m.* FROM product_movements m INNER JOIN products p ON p.id = m.productId
        WHERE p.projectId = :projectId AND p.archived = 0 ORDER BY m.dateEpochDay, m.createdAtEpochMillis
        """,
    )
    fun observeProductMovements(projectId: String): Flow<List<ProductMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProductMovement(movement: ProductMovementEntity)

    @Query("UPDATE categories SET costNature = :costNature WHERE id = :id")
    suspend fun setCategoryCostNature(id: String, costNature: String?)

    /**
     * Cash rows of a book with what BEP needs: the effective cost nature (line override, else
     * category), product and units, and asset life.
     */
    @Query(
        """
        SELECT e.id, e.type, e.dateEpochDay, e.amountRupiah,
               COALESCE(e.costNature, c.costNature) AS costNature,
               e.productId, e.quantityText, e.unit, e.usefulLifeMonths
        FROM financial_events e
        LEFT JOIN categories c ON c.id = e.categoryId
        WHERE e.bookId = :bookId
        ORDER BY e.dateEpochDay
        """,
    )
    fun observeCostRows(bookId: String): Flow<List<CostRow>>

    /** Realisasi sales that name a product: product stock subtracts these (DEC-028 §4). */
    @Query(
        """
        SELECT e.id, e.productId, e.quantityText, e.unit, e.dateEpochDay, e.createdAtEpochMillis
        FROM financial_events e INNER JOIN books b ON b.id = e.bookId
        WHERE e.projectId = :projectId AND e.productId IS NOT NULL AND e.type = :saleType AND b.mode = :mode
        """,
    )
    fun observeProductSales(projectId: String, saleType: String, mode: String): Flow<List<ProductSaleRow>>

    /**
     * Oldest first, so the last row per material is its current price. Same order as
     * [latestPurchasePrice], reversed, with rowid breaking ties (an import inserts many rows in one
     * millisecond); a Rp0 price never prices a material.
     */
    @Query(
        """
        SELECT stockItemId, unitPriceRupiah FROM financial_events
        WHERE businessId = :businessId AND stockItemId IS NOT NULL AND unitPriceRupiah > 0
        ORDER BY dateEpochDay, createdAtEpochMillis, rowid
        """,
    )
    fun observeMaterialPurchases(businessId: String): Flow<List<MaterialPurchaseRow>>

    @Query(
        """
        SELECT unitPriceRupiah FROM financial_events
        WHERE stockItemId = :stockItemId AND unitPriceRupiah > 0
        ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC, rowid DESC LIMIT 1
        """,
    )
    suspend fun latestPurchasePrice(stockItemId: String): Long?

    @Query("SELECT * FROM import_batches WHERE projectId = :projectId ORDER BY committedAtEpochMillis")
    suspend fun getImportBatches(projectId: String): List<ImportBatchEntity>

    @Query("SELECT * FROM import_batches WHERE id = :id")
    suspend fun getImportBatch(id: String): ImportBatchEntity?

    @Query("SELECT * FROM import_batches WHERE projectId = :projectId ORDER BY committedAtEpochMillis DESC")
    fun observeImportBatches(projectId: String): Flow<List<ImportBatchEntity>>

    @Query("DELETE FROM financial_events WHERE importBatchId = :batchId")
    suspend fun deleteEventsForImportBatch(batchId: String): Int

    @Query("SELECT n.* FROM import_notices n INNER JOIN import_batches b ON b.id = n.batchId WHERE b.projectId = :projectId ORDER BY n.createdAtEpochMillis")
    suspend fun getImportNotices(projectId: String): List<ImportNoticeEntity>

    @Query("SELECT p.* FROM provenance_records p INNER JOIN import_batches b ON b.id = p.batchId WHERE b.projectId = :projectId ORDER BY p.batchId, p.id")
    suspend fun getProvenance(projectId: String): List<ProvenanceEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertImportBatch(batch: ImportBatchEntity)

    @Query("UPDATE import_batches SET state = 'REVERSED', reversedAtEpochMillis = :undoneAt WHERE id = :batchId AND state = 'COMMITTED'")
    suspend fun markImportBatchUndone(batchId: String, undoneAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNotice(notice: ImportNoticeEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProvenance(provenance: ProvenanceEntity)

    @Query("SELECT * FROM import_notices ORDER BY reviewed ASC, createdAtEpochMillis DESC")
    fun observeNotices(): Flow<List<ImportNoticeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM import_batches WHERE sourceSha256 = :sourceSha256 AND state = 'COMMITTED')")
    suspend fun hasImported(sourceSha256: String): Boolean
}
