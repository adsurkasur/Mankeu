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
 * Manajemen Persediaan (Stok) Minimal UMKM.
 * Sesuai prinsip: Stok Akhir = Stok Awal + Masuk - Keluar (ditambah Opname Manual).
 */

enum class StockMovementType {
    IN,         // Barang Masuk (Pembelian bahan/produk)
    OUT,        // Barang Keluar (Penjualan / Pemakaian operasional)
    ADJUSTMENT, // Penyesuaian stok fisik (Stock Opname)
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
) {
    val isLowStock: Boolean get() = currentStock <= minAlertStock
}

data class StockMovement(
    val id: String,
    val stockItemId: String,
    val type: StockMovementType,
    val quantity: BigDecimal,
    val date: LocalDate,
    val notes: String = "",
    val referenceEventId: String? = null,
)

data class StockSummary(
    val totalItems: Int,
    val lowStockCount: Int,
    val totalInventoryValueRupiah: Long, // Total Nilai Persediaan = SUM(CurrentStock * BasePrice)
)

object StockCalculation {

    /**
     * Menghitung stok akhir dari stok awal dan urutan mutasi:
     * Stok Akhir = Stok Awal + Total Masuk - Total Keluar (dengan penyesuaian opname jika ada).
     */
    fun calculateStockLevel(
        initialStock: BigDecimal,
        movements: List<StockMovement>,
    ): BigDecimal {
        var current = initialStock.coerceAtLeast(BigDecimal.ZERO)
        for (m in movements) {
            current = when (m.type) {
                StockMovementType.IN -> current.add(m.quantity)
                StockMovementType.OUT -> current.subtract(m.quantity).coerceAtLeast(BigDecimal.ZERO)
                StockMovementType.ADJUSTMENT -> m.quantity.coerceAtLeast(BigDecimal.ZERO)
            }
        }
        return current.setScale(3, RoundingMode.HALF_UP)
    }

    /**
     * Menghitung total nilai persediaan dalam exact integer rupiah.
     */
    fun computeInventoryValue(items: List<StockItem>): Long {
        return items.sumOf { item ->
            TransactionCalculation.computeTotalRupiah(item.currentStock, item.basePriceRupiah)
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
    val movementType: String, // "IN", "OUT", "ADJUSTMENT"
    val quantityText: String,
    val dateEpochDay: Long,
    val notes: String = "",
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

    @Query("SELECT * FROM stock_movements WHERE stockItemId = :stockItemId ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeMovements(stockItemId: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE stockItemId = :stockItemId ORDER BY dateEpochDay ASC, createdAtEpochMillis ASC")
    suspend fun getMovements(stockItemId: String): List<StockMovementEntity>

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

fun StockItemEntity.toDomain() = StockItem(
    id = id,
    businessId = businessId,
    name = name,
    unit = unit,
    basePriceRupiah = basePriceRupiah,
    currentStock = TransactionCalculation.parseQuantity(currentStockText) ?: BigDecimal.ZERO,
    minAlertStock = TransactionCalculation.parseQuantity(minAlertStockText) ?: BigDecimal.ZERO,
    shared = projectId == null,
)

// -------------------------------------------------------------------------------------------------
// Use Cases
// -------------------------------------------------------------------------------------------------

/**
 * Use Case: Mencatat mutasi alur stok (Masuk / Keluar / Penyesuaian Opname).
 * Eksekusi mutasi langsung memperbarui stok terkini pada item secara atomik.
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
        referenceEventId: String? = null,
    ): Result<BigDecimal> {
        // Opname may count zero ("habis"); a movement in or out of nothing is meaningless.
        val allowsZero = type == StockMovementType.ADJUSTMENT
        if (quantity.signum() < 0 || (quantity.signum() == 0 && !allowsZero)) {
            return Result.failure(IllegalArgumentException("Kuantitas mutasi harus lebih besar dari nol."))
        }

        val item = stockDao.getStockItem(stockItemId)
            ?: return Result.failure(IllegalStateException("Barang stok tidak ditemukan."))

        val currentStock = TransactionCalculation.parseQuantity(item.currentStockText) ?: BigDecimal.ZERO
        val newStock = when (type) {
            StockMovementType.IN -> currentStock.add(quantity)
            StockMovementType.OUT -> {
                if (currentStock < quantity) {
                    return Result.failure(IllegalStateException("Stok tidak mencukupi (Tersedia: $currentStock, Diminta: $quantity)."))
                }
                currentStock.subtract(quantity)
            }
            StockMovementType.ADJUSTMENT -> quantity
        }

        val now = System.currentTimeMillis()
        val movement = StockMovementEntity(
            id = java.util.UUID.randomUUID().toString(),
            stockItemId = stockItemId,
            movementType = type.name,
            quantityText = quantity.stripTrailingZeros().toPlainString(),
            dateEpochDay = date.toEpochDay(),
            notes = notes,
            referenceEventId = referenceEventId,
            createdAtEpochMillis = now,
        )

        stockDao.insertStockMovement(movement)
        stockDao.updateStockItem(
            item.copy(
                currentStockText = newStock.stripTrailingZeros().toPlainString(),
                updatedAtEpochMillis = now,
            ),
        )

        return Result.success(newStock)
    }
}
