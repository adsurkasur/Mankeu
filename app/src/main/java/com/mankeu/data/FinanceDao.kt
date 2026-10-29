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

/** Notes of one month summed; see [FinanceDao.getSaleMonths]. */
data class SaleMonthRow(val dateEpochDay: Long, val revenueRupiah: Long, val hppRupiah: Long, val hppEstimated: Boolean)

/** Income, spend and assets that make profit (DEC-029 §4); see [FinanceDao.getProfitEventRows]. */
data class ProfitEventRow(val type: String, val dateEpochDay: Long, val amountRupiah: Long, val includedInHpp: Boolean, val variable: Boolean, val usefulLifeMonths: Int?)

/** A note line of a product as the stock count reads it; see [FinanceDao.observeProductSaleLines]. */
data class ProductSaleRow(
    /** The note line. */
    val id: String,
    val productId: String,
    val quantityText: String?,
    val unit: String?,
    val dateEpochDay: Long,
    val createdAtEpochMillis: Long,
    /** The line's content snapshot; null reads the product's unit. */
    val contentText: String? = null,
    /** Set on a consignment sell-through: already out of stock since it was sent (DEC-029 §8). */
    val consignmentId: String? = null,
)

/** A note line sold through a consignment, with that consignment; see [FinanceDao.observeConsignmentSoldLines]. */
data class ConsignmentSoldLine(
    @androidx.room.Embedded val line: SaleLineEntity,
    val consignmentId: String,
)

/** A purchase that names a material; the newest one prices it (DEC-028 §2). */
data class MaterialPurchaseRow(val stockItemId: String, val unitPriceRupiah: Long)

/** Posting total per (cash-flow role, account, side); see [FinanceDao.getPostingTotals]. */
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

    /** Months with any record in the book, notes of its unit included: Komparasi's per-month divisor. */
    @Query(
        """
        SELECT COUNT(*) FROM (
            SELECT strftime('%Y-%m', dateEpochDay * 86400, 'unixepoch') FROM financial_events WHERE bookId = :bookId
            UNION
            SELECT strftime('%Y-%m', s.dateEpochDay * 86400, 'unixepoch') FROM sales s INNER JOIN books b ON b.projectId = s.projectId
            WHERE b.id = :bookId AND b.mode = 'REALIZATION'
        )
        """,
    )
    fun observeRecordedMonthCount(bookId: String): Flow<Int>

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
     * Expense totals per category: cash and credit purchases at purchase date; paying a supplier is
     * not a cost again (DEC-029 §4). Grouping in SQL keeps the chart correct on a book of any size without loading it.
     */
    @Query(
        """
        SELECT category, SUM(amountRupiah) AS totalRupiah
        FROM financial_events
        WHERE bookId = :bookId
          AND type IN ('CASH_EXPENSE', 'CREDIT_PURCHASE')
          AND settlesEventId IS NULL
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
    suspend fun getPostingTotals(bookId: String): List<PostingTotalRow>

    /** Notes of a unit summed per month: revenue after discount, HPP snapshots (DEC-029 §4). */
    @Query(
        """
        SELECT MIN(s.dateEpochDay) AS dateEpochDay, SUM(t.subtotal - s.discountRupiah) AS revenueRupiah,
               SUM(t.hpp) AS hppRupiah, MAX(s.hppEstimated) AS hppEstimated
        FROM sales s
        INNER JOIN (SELECT saleId, SUM(totalRupiah) AS subtotal, SUM(hppRupiah) AS hpp FROM sale_lines GROUP BY saleId) t ON t.saleId = s.id
        WHERE s.projectId = :projectId
        GROUP BY strftime('%Y-%m', s.dateEpochDay * 86400, 'unixepoch')
        """,
    )
    suspend fun getSaleMonths(projectId: String): List<SaleMonthRow>

    /**
     * Profit rows of a book summed per month, type, HPP category (F2-8) and effective cost nature: income without a note,
     * cash spend that pays no supplier, credit purchases at purchase date, and every asset on its own
     * (depreciation divides each asset, so assets are not summed). Note and supplier payments are absent.
     */
    @Query(
        """
        SELECT e.type, MIN(e.dateEpochDay) AS dateEpochDay, SUM(e.amountRupiah) AS amountRupiah,
               COALESCE(c.includedInHpp, 0) AS includedInHpp,
               COALESCE(COALESCE(e.costNature, c.costNature) = 'VARIABLE', 0) AS variable,
               MAX(e.usefulLifeMonths) AS usefulLifeMonths
        FROM financial_events e
        LEFT JOIN categories c ON c.id = e.categoryId
        WHERE e.bookId = :bookId
          AND ((e.type = 'CASH_INCOME' AND e.saleId IS NULL)
            OR (e.type = 'CASH_EXPENSE' AND e.settlesEventId IS NULL)
            OR e.type IN ('CREDIT_PURCHASE', 'ASSET_PURCHASE_CASH'))
        GROUP BY strftime('%Y-%m', e.dateEpochDay * 86400, 'unixepoch'), e.type, includedInHpp, variable,
                 CASE WHEN e.type = 'ASSET_PURCHASE_CASH' THEN e.id END
        """,
    )
    suspend fun getProfitEventRows(bookId: String): List<ProfitEventRow>

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

    /** Only production and counts are deleted from Riwayat stok; consignment returns belong to their consignment. */
    @Query("DELETE FROM product_movements WHERE id = :id AND movementType IN ('PRODUCTION', 'ADJUSTMENT')")
    suspend fun deleteProductionOrCount(id: String): Int

    /** Rows an import added, by their id prefix (`<batchId>:`), so undo removes them and nothing recorded later. */
    @Query("DELETE FROM product_movements WHERE substr(id, 1, length(:batchId) + 1) = :batchId || ':'")
    suspend fun deleteImportedProductMovements(batchId: String): Int

    /** Payments recorded later on the notes of an import batch (review m3); they have no FK to the note. */
    @Query("DELETE FROM financial_events WHERE saleId IN (SELECT id FROM sales WHERE importBatchId = :batchId)")
    suspend fun deletePaymentsOfImportedNotes(batchId: String): Int

    /** Notes an import's sales became (Migration13To14 keeps the batch id); their lines cascade. */
    @Query("DELETE FROM sales WHERE importBatchId = :batchId")
    suspend fun deleteSalesForImportBatch(batchId: String): Int

    /** Imported products a sale, note line, consignment or production/count outside the batch still uses (cleanup P3). */
    @Query(
        """
        SELECT * FROM products WHERE importBatchId = :batchId AND (
            EXISTS (SELECT 1 FROM financial_events e WHERE e.productId = products.id AND (e.importBatchId IS NULL OR e.importBatchId != :batchId))
            OR EXISTS (SELECT 1 FROM product_movements m WHERE m.productId = products.id AND substr(m.id, 1, length(:batchId) + 1) != :batchId || ':')
            OR EXISTS (SELECT 1 FROM sale_lines l INNER JOIN sales s ON s.id = l.saleId WHERE l.productId = products.id AND (s.importBatchId IS NULL OR s.importBatchId != :batchId))
            OR EXISTS (SELECT 1 FROM consignment_lines l INNER JOIN consignments c ON c.id = l.consignmentId WHERE l.productId = products.id AND (c.importBatchId IS NULL OR c.importBatchId != :batchId))
        ) ORDER BY name COLLATE NOCASE
        """,
    )
    suspend fun getUsedImportedProducts(batchId: String): List<ProductEntity>

    @Query("UPDATE products SET importBatchId = NULL WHERE id IN (:ids)")
    suspend fun releaseFromImport(ids: List<String>)

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

    @Query("SELECT COUNT(*) FROM financial_events WHERE productId = :productId AND unit = :unit COLLATE NOCASE")
    suspend fun countSalesWithUnit(productId: String, unit: String): Int

    @Query("UPDATE financial_events SET unit = :newName WHERE productId = :productId AND unit = :oldName COLLATE NOCASE")
    suspend fun renameSaleUnit(productId: String, oldName: String, newName: String): Int

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

    @Query("SELECT * FROM product_movements WHERE productId = :productId ORDER BY dateEpochDay, createdAtEpochMillis")
    suspend fun getProductMovements(productId: String): List<ProductMovementEntity>

    @Query("UPDATE categories SET costNature = :costNature WHERE id = :id")
    suspend fun setCategoryCostNature(id: String, costNature: String?)

    @Query("UPDATE categories SET includedInHpp = :included WHERE id = :id")
    suspend fun setCategoryIncludedInHpp(id: String, included: Boolean)

    /** Note lines of a unit's products: product stock subtracts these unless sold through a consignment (DEC-029 §8). */
    @Query(
        """
        SELECT l.id, l.productId, l.quantityText, l.unitName AS unit, s.dateEpochDay, s.createdAtEpochMillis, l.contentText, s.consignmentId
        FROM sale_lines l INNER JOIN sales s ON s.id = l.saleId
        WHERE s.projectId = :projectId
        """,
    )
    fun observeProductSaleLines(projectId: String): Flow<List<ProductSaleRow>>

    @Query(
        """
        SELECT l.id, l.productId, l.quantityText, l.contentText, c.dateEpochDay, c.createdAtEpochMillis
        FROM consignment_lines l INNER JOIN consignments c ON c.id = l.consignmentId
        WHERE c.projectId = :projectId
        """,
    )
    fun observeConsignmentSends(projectId: String): Flow<List<ConsignmentSendRow>>

    // ---- Contacts, notes, consignments, supplier credit (DEC-029) ------------------------------

    @Query("SELECT * FROM contacts WHERE businessId = :businessId AND archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeContacts(businessId: String): Flow<List<ContactEntity>>

    /** Archived included: an old note or purchase still names its contact. */
    @Query("SELECT * FROM contacts WHERE businessId = :businessId")
    fun observeAllContacts(businessId: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE businessId IN (SELECT businessId FROM projects WHERE id = :projectId)")
    fun observeAllContactsOfProject(projectId: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContact(id: String): ContactEntity?

    /** Archived rows included: they still hold the (business, name) unique key. */
    @Query("SELECT * FROM contacts WHERE businessId = :businessId AND normalizedName = :normalizedName LIMIT 1")
    suspend fun findContact(businessId: String, normalizedName: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertContact(contact: ContactEntity)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("UPDATE contacts SET archived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun archiveContact(id: String, updatedAt: Long): Int

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSale(id: String): SaleEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSale(sale: SaleEntity)

    @Update
    suspend fun updateSale(sale: SaleEntity)

    /** Lines cascade; payments have no FK and are deleted by the caller. */
    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteSale(id: String): Int

    @Query("SELECT * FROM sales WHERE projectId = :projectId ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeSales(projectId: String): Flow<List<SaleEntity>>

    @Query("SELECT l.* FROM sale_lines l INNER JOIN sales s ON s.id = l.saleId WHERE s.projectId = :projectId ORDER BY l.saleId, l.sortOrder")
    fun observeSaleLinesOfProject(projectId: String): Flow<List<SaleLineEntity>>

    @Query("SELECT * FROM financial_events WHERE projectId = :projectId AND saleId IS NOT NULL ORDER BY dateEpochDay, createdAtEpochMillis")
    fun observeSalePayments(projectId: String): Flow<List<FinancialEventEntity>>

    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId ORDER BY sortOrder")
    suspend fun getSaleLines(saleId: String): List<SaleLineEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSaleLines(lines: List<SaleLineEntity>)

    @Query("DELETE FROM sale_lines WHERE saleId = :saleId")
    suspend fun deleteSaleLines(saleId: String)

    @Query("SELECT * FROM financial_events WHERE saleId = :saleId ORDER BY dateEpochDay, createdAtEpochMillis")
    suspend fun getSalePayments(saleId: String): List<FinancialEventEntity>

    /** A note edited in the note form: its payments stop carrying one product and quantity (DEC-029 M4). */
    @Query("UPDATE financial_events SET productId = NULL, quantityText = NULL, unit = NULL, unitPriceRupiah = NULL WHERE saleId = :saleId")
    suspend fun clearSaleFieldsOfPayments(saleId: String)

    @Query("SELECT COUNT(*) FROM sales WHERE projectId = :projectId")
    suspend fun countSalesForProject(projectId: String): Int

    @Query("SELECT COUNT(*) FROM consignments WHERE projectId = :projectId")
    suspend fun countConsignmentsForProject(projectId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertConsignment(consignment: ConsignmentEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertConsignmentLines(lines: List<ConsignmentLineEntity>)

    @Query("SELECT * FROM consignments WHERE id = :id")
    suspend fun getConsignment(id: String): ConsignmentEntity?

    @Query("SELECT * FROM consignment_lines WHERE consignmentId = :consignmentId ORDER BY sortOrder")
    suspend fun getConsignmentLines(consignmentId: String): List<ConsignmentLineEntity>

    /** Lines of the notes that sold through [consignmentId], the note [exceptSaleId] (being edited) left out. */
    @Query("SELECT l.* FROM sale_lines l INNER JOIN sales s ON s.id = l.saleId WHERE s.consignmentId = :consignmentId AND s.id != :exceptSaleId")
    suspend fun getConsignmentSoldLines(consignmentId: String, exceptSaleId: String): List<SaleLineEntity>

    @Query("SELECT * FROM product_movements WHERE consignmentId = :consignmentId")
    suspend fun getConsignmentReturns(consignmentId: String): List<ProductMovementEntity>

    @Query("SELECT * FROM consignments WHERE projectId = :projectId ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeConsignments(projectId: String): Flow<List<ConsignmentEntity>>

    @Query("SELECT l.* FROM consignment_lines l INNER JOIN consignments c ON c.id = l.consignmentId WHERE c.projectId = :projectId ORDER BY l.consignmentId, l.sortOrder")
    fun observeConsignmentLinesOfProject(projectId: String): Flow<List<ConsignmentLineEntity>>

    @Query("SELECT l.*, s.consignmentId AS consignmentId FROM sale_lines l INNER JOIN sales s ON s.id = l.saleId WHERE s.projectId = :projectId AND s.consignmentId IS NOT NULL")
    fun observeConsignmentSoldLines(projectId: String): Flow<List<ConsignmentSoldLine>>

    @Query("SELECT m.* FROM product_movements m INNER JOIN consignments c ON c.id = m.consignmentId WHERE c.projectId = :projectId")
    fun observeConsignmentReturns(projectId: String): Flow<List<ProductMovementEntity>>

    @Query("SELECT * FROM financial_events WHERE businessId = :businessId AND type = 'CREDIT_PURCHASE' ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeCreditPurchases(businessId: String): Flow<List<FinancialEventEntity>>

    @Query("SELECT * FROM financial_events WHERE businessId = :businessId AND settlesEventId IS NOT NULL ORDER BY dateEpochDay, createdAtEpochMillis")
    fun observeSupplierPayments(businessId: String): Flow<List<FinancialEventEntity>>

    @Query("SELECT * FROM financial_events WHERE settlesEventId = :purchaseId")
    suspend fun getSupplierPayments(purchaseId: String): List<FinancialEventEntity>

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
