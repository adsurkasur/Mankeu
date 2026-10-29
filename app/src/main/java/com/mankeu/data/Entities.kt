package com.mankeu.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey val id: String,
    val name: String,
    val currency: String = "IDR",
    val archived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)

@Entity(
    tableName = "projects",
    foreignKeys = [ForeignKey(
        entity = BusinessEntity::class,
        parentColumns = ["id"],
        childColumns = ["businessId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("businessId")],
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val isGeneral: Boolean,
    val archived: Boolean = false,
    val createdAtEpochMillis: Long,
    val description: String = "",
    val startDateEpochDay: Long? = null,
    val endDateEpochDay: Long? = null,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)

@Entity(
    tableName = "books",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], ForeignKey.CASCADE)],
    indices = [Index("projectId"), Index(value = ["projectId", "mode"], unique = true)],
)
data class BookEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val mode: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)

@Entity(
    tableName = "categories",
    foreignKeys = [ForeignKey(BusinessEntity::class, ["id"], ["businessId"], ForeignKey.CASCADE)],
    indices = [
        Index("businessId"),
        Index(value = ["businessId", "role", "normalizedName"], unique = true),
    ],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val role: String,
    val name: String,
    val normalizedName: String,
    val aliases: String = "",
    val templateOrigin: String? = null,
    val active: Boolean = true,
    val createdAtEpochMillis: Long,
    /** `FIXED` / `VARIABLE` for expense categories (DEC-014); null for income, capital, drawing, asset. */
    val costNature: String? = null,
    /**
     * "Biaya produksi (sudah dihitung di HPP)" (F2-8, schema 14): spend in this expense category is
     * represented by the HPP of what is sold, so it is production spend, not a cost of the month.
     */
    @androidx.room.ColumnInfo(defaultValue = "0") val includedInHpp: Boolean = false,
)

@Entity(
    tableName = "budget_items",
    foreignKeys = [
        ForeignKey(BookEntity::class, ["id"], ["bookId"], ForeignKey.CASCADE),
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], ForeignKey.RESTRICT),
    ],
    indices = [Index("bookId"), Index("categoryId"), Index(value = ["bookId", "sortOrder"])],
)
data class BudgetItemEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val categoryId: String,
    val name: String,
    val normalizedName: String,
    val quantityText: String? = null,
    val unit: String? = null,
    val unitPriceRupiah: Long? = null,
    val plannedTotalRupiah: Long,
    val plannedDateEpochDay: Long? = null,
    val aliases: String = "",
    val amountSource: String = "USER_TOTAL",
    val formulaDifferenceRupiah: Long = 0,
    val sortOrder: Int,
    val archived: Boolean = false,
    val importBatchId: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    /** Overrides the category's cost nature for this line; null inherits. */
    val costNature: String? = null,
    /** `MONTHLY` repeats every month of the plan period; null or `ONCE` is a single amount. */
    val recurrence: String? = null,
    /** Asset lines: straight-line depreciation period for BEP. */
    val usefulLifeMonths: Int? = null,
)

@Entity(
    tableName = "financial_events",
    foreignKeys = [
        ForeignKey(BusinessEntity::class, ["id"], ["businessId"], ForeignKey.CASCADE),
        ForeignKey(ProjectEntity::class, ["id"], ["projectId"], ForeignKey.CASCADE),
        ForeignKey(BookEntity::class, ["id"], ["bookId"], ForeignKey.CASCADE),
    ],
    indices = [
        Index("businessId"),
        Index("projectId"),
        Index("bookId"),
        Index("categoryId"),
        Index("budgetItemId"),
        Index("importBatchId"),
        Index("stockItemId"),
        Index("saleId"),
        Index("contactId"),
        Index("dueDateEpochDay"),
        Index("settlesEventId"),
    ],
)
data class FinancialEventEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val projectId: String,
    val bookId: String,
    val type: String,
    val amountRupiah: Long,
    val dateEpochDay: Long,
    val description: String,
    val category: String,
    val categoryId: String? = null,
    val reference: String? = null,
    val quantityText: String? = null,
    val unit: String? = null,
    val unitPriceRupiah: Long? = null,
    val budgetItemId: String? = null,
    val importBatchId: String? = null,
    val cashFlowRole: String,
    val createdAtEpochMillis: Long,
    /** Overrides the category's cost nature for this expense; null inherits. */
    val costNature: String? = null,
    /** The product a sale was for (optional, DEC-014 B-7); units sold use [quantityText]. */
    val productId: String? = null,
    /** Asset purchases: straight-line depreciation period for BEP. */
    val usefulLifeMonths: Int? = null,
    /** The material an expense bought (spec #20); the latest such purchase sets the material's price. */
    val stockItemId: String? = null,
    /** A payment of this sale note (DEC-029 §2): Kas / Piutang, not revenue. */
    val saleId: String? = null,
    /** Supplier of a credit purchase (DEC-029 §3). */
    val contactId: String? = null,
    /** When a credit purchase is due; null = no due date. */
    val dueDateEpochDay: Long? = null,
    /** The credit purchase this cash expense pays (DEC-029 §3): Utang / Kas. */
    val settlesEventId: String? = null,
)

@Entity(
    tableName = "journal_entries",
    foreignKeys = [ForeignKey(FinancialEventEntity::class, ["id"], ["sourceEventId"], ForeignKey.CASCADE)],
    indices = [Index(value = ["sourceEventId"], unique = true)],
)
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val sourceEventId: String,
    val dateEpochDay: Long,
    val description: String,
)

@Entity(
    tableName = "postings",
    foreignKeys = [ForeignKey(JournalEntryEntity::class, ["id"], ["journalId"], ForeignKey.CASCADE)],
    indices = [Index("journalId"), Index("account")],
)
data class PostingEntity(
    @PrimaryKey val id: String,
    val journalId: String,
    val account: String,
    val side: String,
    val amountRupiah: Long,
)

/** Something the business sells (DEC-014 B-5, spec #5 #6), planned per business unit. */
@Entity(
    tableName = "products",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], ForeignKey.CASCADE)],
    indices = [Index("projectId")],
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    /** The base unit (spec #5): stock, batch yield and HPP are counted in it. */
    val unit: String,
    /** Base units one batch of the recipe yields (spec #6). */
    @ColumnInfo(defaultValue = "1") val batchYieldText: String = "1",
    /** Planned sales per month in the main (first) sale unit; null leaves the sales mix undefined. */
    val targetPerMonthText: String? = null,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    /** Set when the product came from a template import, so undoing that import removes it. */
    val importBatchId: String? = null,
    /** Month of the plan period (1 = first) from which it sells; seasonal products start later. */
    @ColumnInfo(defaultValue = "1") val sellFromMonth: Int = 1,
)

/** A way a product is sold (spec #5): [contentText] base units per one, at its own price. */
@Entity(
    tableName = "product_units",
    foreignKeys = [ForeignKey(ProductEntity::class, ["id"], ["productId"], ForeignKey.CASCADE)],
    indices = [Index("productId")],
)
data class ProductUnitEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val name: String,
    val contentText: String,
    val priceRupiah: Long,
    val sortOrder: Int = 0,
)

/**
 * One line of a product's per-batch recipe (spec #6, #20): [quantityText] of material
 * [stockItemId], priced at its last purchase, or a free [amountRupiah]. No foreign key to
 * stock_items: a removed material leaves its line, and the HPP shows as incomplete instead of
 * silently getting cheaper.
 */
@Entity(
    tableName = "product_recipe_lines",
    foreignKeys = [ForeignKey(ProductEntity::class, ["id"], ["productId"], ForeignKey.CASCADE)],
    indices = [Index("productId"), Index("stockItemId")],
)
data class ProductRecipeLineEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val name: String,
    val stockItemId: String? = null,
    val quantityText: String? = null,
    val amountRupiah: Long? = null,
    val sortOrder: Int = 0,
)

/**
 * Production and stock counts of a product, in base units (spec #9). Sales are not copied here:
 * the stock reads them from the sales themselves, so editing or deleting a sale is always reflected.
 */
@Entity(
    tableName = "product_movements",
    foreignKeys = [ForeignKey(ProductEntity::class, ["id"], ["productId"], ForeignKey.CASCADE)],
    indices = [Index("productId")],
)
data class ProductMovementEntity(
    @PrimaryKey val id: String,
    val productId: String,
    /** [com.mankeu.domain.ProductMovementType.PRODUCTION], `ADJUSTMENT` (a count) or `CONSIGN_RETURN` (DEC-029 §8). */
    val movementType: String,
    val quantityText: String,
    /** Production only: how many batches made [quantityText]. */
    val batchesText: String? = null,
    val dateEpochDay: Long,
    val notes: String = "",
    val createdAtEpochMillis: Long,
    /** `CONSIGN_RETURN` only: the consignment the goods came back from, so the store's balance drops (DEC-029 §8). */
    val consignmentId: String? = null,
)

/** A customer and/or supplier of a business (F2-2, DEC-029 §9); archived, not deleted, once used. */
@Entity(
    tableName = "contacts",
    foreignKeys = [ForeignKey(BusinessEntity::class, ["id"], ["businessId"], ForeignKey.CASCADE)],
    indices = [Index("businessId"), Index(value = ["businessId", "normalizedName"], unique = true)],
)
data class ContactEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val normalizedName: String,
    val phone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val isCustomer: Boolean = true,
    val isSupplier: Boolean = false,
    val archived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val importBatchId: String? = null,
)

/** A sale note (DEC-029): revenue is recognised here; payments are `CASH_INCOME` events with [id] as saleId. */
@Entity(
    tableName = "sales",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], ForeignKey.CASCADE)],
    indices = [Index("projectId"), Index("contactId"), Index("consignmentId"), Index("importBatchId")],
)
data class SaleEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val projectId: String,
    val contactId: String? = null,
    val dateEpochDay: Long,
    val dueDateEpochDay: Long? = null,
    val discountRupiah: Long = 0,
    val notes: String = "",
    /** Set when the sale is a consignment sell-through (F2-6); such lines do not take stock again (DEC-029 §8). */
    val consignmentId: String? = null,
    /** Migrated from a schema-13 cash sale (F2-4): the HPP is the recipe cost at migration time. */
    val hppEstimated: Boolean = false,
    val importBatchId: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)

@Entity(
    tableName = "sale_lines",
    foreignKeys = [ForeignKey(SaleEntity::class, ["id"], ["saleId"], ForeignKey.CASCADE)],
    indices = [Index("saleId"), Index("productId")],
)
data class SaleLineEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val productId: String,
    val unitName: String,
    val contentText: String,
    val quantityText: String,
    val unitPriceRupiah: Long,
    val totalRupiah: Long,
    /** HPP of this line when sold (spec #6). */
    val hppRupiah: Long,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "consignments",
    foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], ForeignKey.CASCADE)],
    indices = [Index("projectId"), Index("contactId")],
)
data class ConsignmentEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val projectId: String,
    val contactId: String,
    val dateEpochDay: Long,
    val notes: String = "",
    val importBatchId: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
)

/** One product sent on consignment, priced per sale unit at sending (F2-6). */
@Entity(
    tableName = "consignment_lines",
    foreignKeys = [ForeignKey(ConsignmentEntity::class, ["id"], ["consignmentId"], ForeignKey.CASCADE)],
    indices = [Index("consignmentId"), Index("productId")],
)
data class ConsignmentLineEntity(
    @PrimaryKey val id: String,
    val consignmentId: String,
    val productId: String,
    val unitName: String,
    val contentText: String,
    val quantityText: String,
    val priceRupiah: Long,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "import_batches",
    // Not unique: an undone batch keeps its row as audit history, and re-importing the same file
    // afterwards is a legitimate correction. `hasImported` is the
    // duplicate guard, and it only counts batches still COMMITTED (D-076).
    indices = [Index("sourceSha256"), Index("bookId")],
)
data class ImportBatchEntity(
    @PrimaryKey val id: String,
    val sourceName: String,
    val sourceSha256: String,
    val importerVersion: String,
    val state: String,
    val committedAtEpochMillis: Long,
    val businessId: String? = null,
    val projectId: String? = null,
    val bookId: String? = null,
    val mappingVersion: String? = null,
    val recordCount: Int = 0,
    val totalAmountRupiah: Long = 0,
    val reversedAtEpochMillis: Long? = null,
    /** The plan period this import gave a unit that had none; undoing the batch clears it again. */
    val appliedStartEpochDay: Long? = null,
    val appliedEndEpochDay: Long? = null,
    /** Money in among [totalAmountRupiah]; null on batches from before schema 10, which only kept the total. */
    val incomingRupiah: Long? = null,
)

@Entity(
    tableName = "import_notices",
    foreignKeys = [ForeignKey(ImportBatchEntity::class, ["id"], ["batchId"], ForeignKey.CASCADE)],
    indices = [Index("batchId")],
)
data class ImportNoticeEntity(
    @PrimaryKey val id: String,
    /** Null for notices the app raises itself, e.g. the schema-8 strict-cash cleanup. */
    val batchId: String?,
    val severity: String,
    val title: String,
    val explanation: String,
    val amountRupiah: Long?,
    val sourceEvidence: String,
    val reviewed: Boolean = false,
    val action: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "provenance_records",
    foreignKeys = [ForeignKey(ImportBatchEntity::class, ["id"], ["batchId"], ForeignKey.CASCADE)],
    indices = [Index("batchId"), Index("targetEventId"), Index("targetBudgetItemId")],
)
data class ProvenanceEntity(
    @PrimaryKey val id: String,
    val batchId: String,
    val targetEventId: String? = null,
    val targetBudgetItemId: String? = null,
    val targetField: String? = null,
    val classification: String,
    val sourceEvidence: String,
    val sourceSheet: String? = null,
    val sourceCell: String? = null,
    val sourceValue: String? = null,
    val rationale: String,
    val inferredFields: String,
    val confidenceBasisPoints: Int? = null,
    val ruleVersion: String = "v2",
)
