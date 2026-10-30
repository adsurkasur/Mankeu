package com.mankeu.domain

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mankeu.data.BusinessEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Material stock (U-2, DEC-030 §3), derived on read like product stock: the opening stock, then every
 * movement oldest first (date, then time recorded); a count ([StockMovementType.ADJUSTMENT]) sets the level.
 * It may go below zero (recorded after the fact); screens then say "catat belanja atau hitung ulang".
 */

enum class StockMovementType {
    IN,         // Tambah: manual stock-in without money (own harvest, correction)
    OUT,        // Pakai: manual stock-out
    ADJUSTMENT, // Hitung ulang: the counted level from this date on
    /** Production use of a recipe material, stored with `referenceEventId` = the production row (deleted with it). */
    CONSUME,
    /** A linked purchase (CASH_EXPENSE / CREDIT_PURCHASE with a material and quantity); derived, never stored. */
    PURCHASE,
}

data class StockItem(
    val id: String,
    val businessId: String,
    val name: String,
    val unit: String,
    val basePriceRupiah: Long, // Current price: newest purchase, else the price entered with the item (exact integer rupiah)
    val currentStock: BigDecimal,
    val minAlertStock: BigDecimal,
    /** Shared by every unit of the business rather than held by one branch. */
    val shared: Boolean = false,
    /** What [currentStock] was derived from, oldest first (Riwayat stok bahan). */
    val history: List<StockHistoryLine> = emptyList(),
) {
    /** Audit L05: without a minimum there is no "menipis"; a new material at 0 is not a warning. */
    val isLowStock: Boolean get() = minAlertStock.signum() > 0 && currentStock <= minAlertStock
}

data class StockMovement(
    val id: String,
    val stockItemId: String,
    val type: StockMovementType,
    val quantity: BigDecimal,
    val date: LocalDate,
    val notes: String = "",
    val referenceEventId: String? = null,
    val createdAtEpochMillis: Long = 0,
)

/** One movement with the material stock after it. */
data class StockHistoryLine(val movement: StockMovement, val stockAfter: BigDecimal)

data class StockSummary(
    val totalItems: Int,
    val lowStockCount: Int,
    val totalInventoryValueRupiah: Long, // Total Nilai Persediaan = SUM(CurrentStock * BasePrice)
)

object StockCalculation {

    /** [movements] oldest first (date, then time recorded), each with the level after it; negative allowed. */
    fun history(initialStock: BigDecimal, movements: List<StockMovement>): List<StockHistoryLine> {
        var level = initialStock
        return movements.sortedWith(compareBy({ it.date }, { it.createdAtEpochMillis })).map { m ->
            level = when (m.type) {
                StockMovementType.IN, StockMovementType.PURCHASE -> level + m.quantity
                StockMovementType.OUT, StockMovementType.CONSUME -> level - m.quantity
                StockMovementType.ADJUSTMENT -> m.quantity
            }
            StockHistoryLine(m, level)
        }
    }

    fun calculateStockLevel(initialStock: BigDecimal, movements: List<StockMovement>): BigDecimal =
        (history(initialStock, movements).lastOrNull()?.stockAfter ?: initialStock).setScale(3, RoundingMode.HALF_UP)

    /** Total value on hand in exact integer rupiah; a material below zero adds nothing. */
    fun computeInventoryValue(items: List<StockItem>): Long {
        return items.sumOf { item ->
            TransactionCalculation.computeTotalRupiah(item.currentStock.coerceAtLeast(BigDecimal.ZERO), item.basePriceRupiah)
        }
    }
    fun summarize(items: List<StockItem>): StockSummary = StockSummary(
        totalItems = items.size,
        lowStockCount = items.count { it.isLowStock },
        totalInventoryValueRupiah = computeInventoryValue(items),
    )
}

// -------------------------------------------------------------------------------------------------
// Room Database Design (Entitas & DAO)
// -------------------------------------------------------------------------------------------------

@Entity(
    tableName = "stock_items",
    foreignKeys = [
        ForeignKey(
            entity = BusinessEntity::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("businessId"),
        Index(value = ["businessId", "projectId", "name"], unique = true),
    ],
)
data class StockItemEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val unit: String,
    val basePriceRupiah: Long,
    val initialStockText: String = "0",
    /** Not read or written since U3 (DEC-030 §3): stock is derived on read. Kept so schema 14 stays as it is. */
    val currentStockText: String = "0",
    val minAlertStockText: String = "0",
    val archived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    /** Set when the item came from a template import, so undoing that import removes it. */
    val importBatchId: String? = null,
    /** The business unit (branch) holding it; null means shared by every unit of the business. */
    val projectId: String? = null,
)

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = StockItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["stockItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("stockItemId"),
        Index("dateEpochDay"),
        Index("referenceEventId"),
    ],
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val stockItemId: String,
    val movementType: String, // "IN", "OUT", "ADJUSTMENT", "CONSUME" (production use)
    val quantityText: String,
    val dateEpochDay: Long,
    val notes: String = "",
    /** CONSUME: the product_movements row (production) this use belongs to. */
    val referenceEventId: String? = null,
    val createdAtEpochMillis: Long,
)

@Dao
interface StockDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockItem(item: StockItemEntity)

    @Update
    suspend fun updateStockItem(item: StockItemEntity)

    @Query("SELECT * FROM stock_items WHERE id = :id LIMIT 1")
    suspend fun getStockItem(id: String): StockItemEntity?

    /** What a unit sees: its own items plus the business's shared ones. */
    @Query("SELECT * FROM stock_items WHERE businessId = :businessId AND (projectId IS NULL OR projectId = :projectId) AND archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeStockItems(businessId: String, projectId: String?): Flow<List<StockItemEntity>>

    @Query("SELECT * FROM stock_items WHERE businessId = :businessId AND (projectId IS NULL OR projectId = :projectId) AND archived = 0 ORDER BY name COLLATE NOCASE")
    suspend fun getStockItems(businessId: String, projectId: String?): List<StockItemEntity>

    @Query("SELECT * FROM stock_items WHERE businessId = :businessId AND archived = 0")
    suspend fun getAllStockItems(businessId: String): List<StockItemEntity>

    /** Stock a unit holds itself; such a unit is archived, never deleted, so its stock is not orphaned. */
    @Query("SELECT COUNT(*) FROM stock_items WHERE projectId = :projectId")
    suspend fun countStockItemsOfProject(projectId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockMovement(movement: StockMovementEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockMovements(movements: List<StockMovementEntity>)

    /** Manual moves and counts of the business's materials (export, U3). */
    @Query("SELECT m.* FROM stock_movements m INNER JOIN stock_items i ON i.id = m.stockItemId WHERE i.businessId = :businessId AND m.movementType != 'CONSUME' ORDER BY m.dateEpochDay, m.createdAtEpochMillis")
    suspend fun getManualMovements(businessId: String): List<StockMovementEntity>

    /** Production use by the active products of [projectId] (export, U3). */
    @Query(
        """
        SELECT s.* FROM stock_movements s INNER JOIN product_movements m ON m.id = s.referenceEventId
        INNER JOIN products p ON p.id = m.productId WHERE s.movementType = 'CONSUME' AND p.projectId = :projectId AND p.archived = 0
        """,
    )
    suspend fun getConsumptionOfProject(projectId: String): List<StockMovementEntity>

    /** Every movement of the business's materials, for the derived stock. */
    @Query("SELECT m.* FROM stock_movements m INNER JOIN stock_items i ON i.id = m.stockItemId WHERE i.businessId = :businessId")
    fun observeBusinessMovements(businessId: String): Flow<List<StockMovementEntity>>

    /**
     * Production use whose production is gone. Every path that deletes production (Riwayat stok, undo, a
     * removed unit's cascade) calls this after, so the use goes with its production.
     */
    @Query("DELETE FROM stock_movements WHERE movementType = 'CONSUME' AND (referenceEventId IS NULL OR referenceEventId NOT IN (SELECT id FROM product_movements))")
    suspend fun deleteOrphanConsumption(): Int

    /** Rows an import added, by their id prefix (`<batchId>:`), like product movements. */
    @Query("DELETE FROM stock_movements WHERE substr(id, 1, length(:batchId) + 1) = :batchId || ':'")
    suspend fun deleteImportedMovements(batchId: String): Int

    @Query("DELETE FROM stock_items WHERE importBatchId = :batchId")
    suspend fun deleteImportedItems(batchId: String): Int

    /**
     * Imported items that something outside the batch still uses: a purchase, a recipe line of a
     * product that is not (or no longer) part of the batch, or a stock movement (cleanup P3). Call
     * after the used products are released, so a kept product's recipe keeps its materials.
     */
    @Query(
        """
        SELECT * FROM stock_items WHERE importBatchId = :batchId AND (
            EXISTS (SELECT 1 FROM financial_events e WHERE e.stockItemId = stock_items.id AND (e.importBatchId IS NULL OR e.importBatchId != :batchId))
            OR EXISTS (SELECT 1 FROM product_recipe_lines l INNER JOIN products p ON p.id = l.productId
                       WHERE l.stockItemId = stock_items.id AND (p.importBatchId IS NULL OR p.importBatchId != :batchId))
            OR EXISTS (SELECT 1 FROM stock_movements m WHERE m.stockItemId = stock_items.id)
        ) ORDER BY name COLLATE NOCASE
        """,
    )
    suspend fun getUsedImportedItems(batchId: String): List<StockItemEntity>

    @Query("UPDATE stock_items SET importBatchId = NULL WHERE id IN (:ids)")
    suspend fun releaseFromImport(ids: List<String>)

    @Query("UPDATE stock_items SET archived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun archiveStockItem(id: String, updatedAt: Long)
}

/** The item with its stock derived from [movements] (stored ones and linked purchases) on top of the opening stock. */
fun StockItemEntity.toDomain(movements: List<StockMovement> = emptyList()): StockItem {
    val opening = TransactionCalculation.parseQuantity(initialStockText) ?: BigDecimal.ZERO
    val history = StockCalculation.history(opening, movements)
    return StockItem(
        id = id,
        businessId = businessId,
        name = name,
        unit = unit,
        basePriceRupiah = basePriceRupiah,
        currentStock = history.lastOrNull()?.stockAfter ?: opening,
        minAlertStock = TransactionCalculation.parseQuantity(minAlertStockText) ?: BigDecimal.ZERO,
        shared = projectId == null,
        history = history,
    )
}

/** A stored movement as the stock reads it; null for a kind this version does not know (a newer phone or backup). */
fun StockMovementEntity.toDomain(): StockMovement? = StockMovementType.entries.firstOrNull { it.name == movementType }?.let { type ->
    // parseQuantity returns null for "0", which a count may legitimately be.
    StockMovement(id, stockItemId, type, quantityText.toBigDecimalOrNull() ?: BigDecimal.ZERO, LocalDate.ofEpochDay(dateEpochDay), notes, referenceEventId, createdAtEpochMillis)
}

// -------------------------------------------------------------------------------------------------
// Use Cases
// -------------------------------------------------------------------------------------------------

/**
 * Use Case: a manual material movement (Tambah / Pakai / Hitung ulang). Only the movement is stored; the
 * stock is derived. Taking out more than recorded is allowed (recorded after the fact); the screen warns.
 */
class RecordStockMovementUseCase(
    private val stockDao: StockDao,
) {
    suspend fun execute(
        stockItemId: String,
        type: StockMovementType,
        quantity: BigDecimal,
        date: LocalDate,
        notes: String = "",
    ): Result<Unit> {
        if (type !in MANUAL) return Result.failure(IllegalArgumentException("Jenis mutasi ini dicatat lewat belanja atau produksi."))
        // A count may be zero ("habis"); a movement in or out of nothing is meaningless.
        val allowsZero = type == StockMovementType.ADJUSTMENT
        if (quantity.signum() < 0 || (quantity.signum() == 0 && !allowsZero)) {
            return Result.failure(IllegalArgumentException("Kuantitas mutasi harus lebih besar dari nol."))
        }
        stockDao.getStockItem(stockItemId) ?: return Result.failure(IllegalStateException("Barang stok tidak ditemukan."))
        stockDao.insertStockMovement(
            StockMovementEntity(
                id = java.util.UUID.randomUUID().toString(),
                stockItemId = stockItemId,
                movementType = type.name,
                quantityText = quantity.stripTrailingZeros().toPlainString(),
                dateEpochDay = date.toEpochDay(),
                notes = notes,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        return Result.success(Unit)
    }

    private companion object {
        val MANUAL = setOf(StockMovementType.IN, StockMovementType.OUT, StockMovementType.ADJUSTMENT)
    }
}
