# Fase 1: Produk, Satuan Jual, Resep per Batch, HPP, Stok Produk — Rencana Implementasi

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Pengusaha mengisi satu resep per batch dan satuan jualnya sekali, lalu Mankeu menampilkan HPP dan untung per
bungkus, per dus, dan per satuan lain tanpa hitungan manual. Stok produk ikut bertambah lewat Produksi dan berkurang
lewat penjualan. Belanja bahan dengan harga baru langsung memperingatkan dampaknya ke HPP.

**Architecture:** Hitungan HPP dan stok berupa fungsi murni di `domain/ProductCosting.kt`. Schema naik ke 13 dengan
tiga tabel baru (`product_units`, `product_recipe_lines`, `product_movements`); migrasi 12→13 mengubah bentuk produk
lama lewat satu adaptor (`LegacyProductShape`) yang juga dipakai untuk memulihkan backup schema 12. Harga bahan dan
stok produk **diturunkan saat dibaca**: harga = harga satuan belanja terakhir yang menautkan bahan itu, dan stok =
produksi + hitung stok − penjualan. Tidak ada angka cache yang bisa melenceng saat transaksi diubah atau dihapus. Tab
"Stok" menjadi "Produk", dan editor produk berupa halaman penuh (#13).

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Room 2.8.4 (schema 12 → 13), JUnit4, Room `MigrationTestHelper`
(sudah ada di `androidTestImplementation(libs.androidx.room.testing)`).

**Spec:** `docs/superpowers/specs/2026-10-29-redesign-spec.md`, keputusan #5, #6, #9, #20 (juga #13, #19).
Peta fase: `docs/superpowers/plans/2026-10-29-redesign-roadmap.md` (kriteria F1).

## Global Constraints

- Uang selalu `Long` rupiah bulat; tidak ada floating point untuk uang tersimpan. Pembagian HPP dibulatkan ke rupiah
  dengan aturan yang sama di semua tempat (setengah ke atas) dan diuji.
- Kuantitas boleh pecahan (kg, liter) dan memakai `BigDecimal` seperti `TransactionCalculation.parseQuantity`.
- Operasi lokal tidak bergantung pada login, jaringan, Firebase, atau server milik app.
- Tidak ada dependensi baru tanpa alasan tertulis di DEC.
- Laporan adalah turunan; tidak boleh jadi tempat input kedua.
- Bahasa sehari-hari di layar (untung, modal, HPP, uang masuk/keluar). Istilah formal hanya di PDF dan "Rincian analisis" (#19).
- Form panjang = halaman penuh dengan Simpan tetap di bawah; bottom sheet hanya untuk pilihan pendek dan konfirmasi (#13).
- Setiap commit membawa entri `logs/ACTIVITY_LOG.md` (paling atas; jam = jam nyata dari `Get-Date -Format "yyyy-MM-dd HH:mm"`,
  jangan mengarang), dan `logs/DECISION_LOG.md` / `logs/CHANGELOG.md` bila tugas menyentuhnya (AGENTS.md).
- Teks layar baru masuk ke `app/src/main/res/values/strings.xml` **dan** `values-en/strings.xml`.
- Gradle dijalankan dari PowerShell dengan PATH yang dibersihkan (PATH Git Bash punya entri `"` yang membuat tes unit gagal):
  `$env:Path = ($env:Path -split ';' | Where-Object { $_ -notmatch '"' }) -join ';'`. Bila Gradle macet: `./gradlew.bat --stop`.
- Tes instrumented menghapus data app di emulator; itu diterima (data uji).

## Keputusan rencana (dibuat saat menulis rencana ini, bukan dari spec)

Masing-masing dicatat di DEC-028 (Task 2). Format: keputusan — alasan — biaya bila salah.

1. **`stock_items` dipakai ulang sebagai daftar bahan.** Tabelnya sudah menyimpan nama, satuan, dan harga beli,
   sehingga tidak perlu tabel baru. Biaya bila salah: barang dagangan non-bahan tercampur di daftar yang sama (labelnya "Bahan & barang").
2. **Harga bahan diturunkan, tidak disimpan.** Harga = `unitPriceRupiah` dari pengeluaran terbaru (tanggal, lalu waktu
   dicatat) yang menautkan bahan itu. Bila tidak ada, dipakai harga yang diisi saat bahan dibuat. Menghapus belanja
   otomatis mengembalikan harga. Biaya bila salah: satu query tambahan saat membaca daftar bahan.
3. **Belanja bahan tidak menambah stok bahan.** Stok bahan tetap dihitung manual lewat Masuk/Keluar/Opname, karena
   #9 menetapkan bahan tidak dikurangi oleh produksi. Kalau belanja menambah tetapi produksi tidak mengurangi, angkanya
   hanya akan naik terus. Biaya bila salah: pengguna yang mengharapkan stok bahan bertambah otomatis harus mencatat Masuk sendiri.
4. **Stok produk diturunkan dari penjualan.** Tidak ada baris "keluar" yang ditulis saat menjual. Stok = urutan
   produksi, hitung stok, dan penjualan (produk + jumlah × isi satuan) menurut tanggal. Ubah atau hapus penjualan langsung
   benar. Penjualan lama dari sebelum update ikut terhitung, jadi stok bisa minus sampai pemilik mencatat produksi atau
   hitung stok; layar menampilkan petunjuk itu. Biaya bila salah: tester melihat stok minus setelah update.
5. **Snapshot HPP per penjualan (#6) dipindah ke Fase 2.** Bentuk catatan penjualan berubah total di Fase 2 (nota
   multi-produk, #17), dan laba saat terjual (#3) juga baru dihitung di sana. Biaya bila salah: tidak ada, karena laporan
   F1 belum memakai HPP.
6. **Bentuk produk lama (schema 12 / template v4) diubah lewat satu adaptor.** Setiap produk menjadi batch berisi 1
   satuan dasar, satuan jual lamanya menjadi satuan jual pertama (isi 1, id tetap `<produk>-u0`), dan tiap komponen
   "Rp X untuk N unit" menjadi biaya bebas `round(X ÷ N)`. Pembulatan maksimal ±Rp0,5 per komponen per unit. Id
   ditentukan dari data, jadi dua HP yang bermigrasi sendiri-sendiri menghasilkan baris yang sama. Biaya bila salah: HPP produk lama bisa bergeser Rp1.
7. **Sinkron antar versi berbeda:** HP schema 12 menolak menerima dari HP schema 13, karena perilaku itu sudah ada
   (hanya menerima `schemaVersion <= own`); HP schema 13 tetap menerima dari HP schema 12. Perubahan produk yang dikirim HP schema 12 ke HP schema 13 kehilangan satuan jual dan
   resepnya. Catatan rilis: "perbarui semua HP". Biaya bila salah: produk yang diubah di HP lama selama masa transisi perlu diisi ulang.
8. **Satuan jual pertama = satuan utama.** Satuan ini dipakai Rencana Usaha (target per bulan, BEP) dan sebagai isian
   awal form penjualan, sampai Fase 3 merombak Rencana. Biaya bila salah: urutan satuan perlu diatur ulang di editor.
9. **Template Excel naik ke v5 khusus untuk lembar produk** (plus kolom Satuan dan Bahan di Transaksi), supaya aturan
   DEC-012 "ekspor bisa diimpor ulang tanpa kehilangan" tetap berlaku di setiap rilis. File v4 ditolak, karena header lembar
   Produk-nya tidak cocok, dengan daftar masalah yang jelas. Template lengkap dan pesan penolakan yang ramah tetap di Fase 5.
   Biaya bila salah: lembar produk ditulis ulang lagi di F5.

## Peta berkas

| Berkas | Tanggung jawab | Tugas |
|---|---|---|
| `domain/ProductCosting.kt` (baru) | HPP per batch/satuan, untung, dampak harga bahan, stok produk | 1 |
| `data/Entities.kt` | `ProductEntity` baru, 3 entitas baru, `FinancialEventEntity.stockItemId` | 2 |
| `data/LegacyProductShape.kt` (baru) | Adaptor bentuk produk schema 12 / template v4 | 2 |
| `data/Migration12To13.kt` (baru) | Migrasi 12→13 | 2 |
| `data/ProductRecord.kt` (baru) | Produk + satuan + resep + gerakan + penjualan → `ProductSpec`, stok | 2, 3 |
| `data/AppDatabase.kt` | versi 13, entitas, `DATA_TABLES`, `addMigrations` | 2 |
| `backup/BackupUpgrade.kt` (baru) | Snapshot schema 12 → 13 | 2 |
| `data/FinanceDao.kt`, `data/FinanceRepository.kt` | Query dan tulis produk, produksi, hitung stok, penjualan, harga bahan | 2, 3, 4 |
| `data/PlanAssembly.kt` | Rencana memakai satuan utama + HPP; penjualan dikonversi antar satuan | 2, 3 |
| `importer/*`, `exporter/FinanceWorkbookExporter.kt` | Template v5 lembar produk | 2 (adaptor), 4 |
| `presentation/screens/ProductsScreen.kt` (baru), `ProductEditorScreen.kt` (baru), `StockSection.kt` | Tab Produk, editor halaman penuh, Produksi, hitung stok | 5 |
| `presentation/screens/PlanScreen.kt` | Bagian produk membuka editor; sheet produk lama dihapus | 5 |
| `presentation/screens/EntryScreen.kt`, `AppViewModel.kt`, `MankeuApp.kt` | Pilihan satuan jual, pilihan bahan, dialog dampak harga | 5, 6 |

---

### Task 1: Hitungan HPP dan stok produk (domain murni)

**Files:**
- Create: `app/src/main/java/com/mankeu/domain/ProductCosting.kt`
- Test: `app/src/test/java/com/mankeu/domain/ProductCostingTest.kt`

**Interfaces:**
- Consumes: `TransactionCalculation.computeTotalRupiah(quantity: BigDecimal, unitPriceRupiah: Long): Long` (sudah ada).
- Produces (dipakai Task 2–6, nama dan tipe persis):
  - `data class SaleUnit(val id: String, val name: String, val content: BigDecimal, val priceRupiah: Long)`
  - `data class RecipeLine(val name: String, val materialId: String? = null, val quantity: BigDecimal? = null, val amountRupiah: Long? = null)`
  - `data class ProductSpec(val id: String, val name: String, val baseUnit: String, val batchYield: BigDecimal, val saleUnits: List<SaleUnit>, val recipe: List<RecipeLine>)`
  - `data class UnitCost(val unit: SaleUnit, val hppRupiah: Long)` dengan `val profitRupiah: Long`
  - `data class ProductCost(val batchCostRupiah: Long, val hppPerBaseUnitRupiah: Long, val perSaleUnit: List<UnitCost>, val unpricedLines: List<String>, val hasRecipe: Boolean)` dengan `val complete: Boolean` (= `hasRecipe && unpricedLines.isEmpty()`)
  - `data class HppChange(val product: ProductSpec, val before: ProductCost, val after: ProductCost)`
  - `object ProductCosting { fun lineCost(line: RecipeLine, materialPrices: Map<String, Long>): Long?; fun hpp(batchCostRupiah: Long, batchYield: BigDecimal, content: BigDecimal): Long; fun cost(product: ProductSpec, materialPrices: Map<String, Long>): ProductCost; fun priceImpact(products: List<ProductSpec>, materialPrices: Map<String, Long>, materialId: String, newPriceRupiah: Long): List<HppChange>; fun legacyComponentPerUnit(amountRupiah: Long, yieldQuantity: BigDecimal): Long }`
  - `enum class ProductMovementType { PRODUCTION, SALE, ADJUSTMENT }`
  - `data class ProductMovement(val type: ProductMovementType, val quantity: BigDecimal)`
  - `object ProductStock { fun level(movements: List<ProductMovement>): BigDecimal; fun split(level: BigDecimal, content: BigDecimal): Pair<BigDecimal, BigDecimal> }`

- [ ] **Step 1: Tulis tes yang gagal**

```kotlin
package com.mankeu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ProductCostingTest {
    private fun qty(s: String) = BigDecimal(s)

    // Kriteria F1: batch Rp2,4 jt → 400 bungkus, dus isi 40.
    private val keripik = ProductSpec(
        id = "keripik",
        name = "Keripik singkong",
        baseUnit = "bungkus",
        batchYield = qty("400"),
        saleUnits = listOf(
            SaleUnit("u-bks", "bungkus", qty("1"), 8_000),
            SaleUnit("u-dus", "dus", qty("40"), 300_000),
        ),
        recipe = listOf(
            RecipeLine("Singkong", materialId = "singkong", quantity = qty("50")),
            RecipeLine("Minyak", materialId = "minyak", quantity = qty("20")),
            RecipeLine("Bumbu & kemasan", amountRupiah = 300_000),
            RecipeLine("Gas", amountRupiah = 100_000),
        ),
    )
    private val prices = mapOf("singkong" to 30_000L, "minyak" to 25_000L)

    @Test fun `keripik golden - HPP per bungkus and per dus without manual division`() {
        val cost = ProductCosting.cost(keripik, prices)
        assertEquals(2_400_000L, cost.batchCostRupiah)
        assertEquals(6_000L, cost.hppPerBaseUnitRupiah)
        assertEquals(listOf(6_000L, 240_000L), cost.perSaleUnit.map { it.hppRupiah })
        assertEquals(listOf(2_000L, 60_000L), cost.perSaleUnit.map { it.profitRupiah })
        assertTrue(cost.complete)
        assertTrue(cost.hasRecipe)
    }

    @Test fun `hpp rounds once half up, never from a rounded per-unit value`() {
        assertEquals(333L, ProductCosting.hpp(1_000, qty("3"), qty("1")))
        assertEquals(667L, ProductCosting.hpp(1_000, qty("3"), qty("2")))
        assertEquals(3L, ProductCosting.hpp(5, qty("2"), qty("1"))) // 2,5 → 3
    }

    @Test fun `material line costs quantity times price rounded half up; fractional quantities allowed`() {
        assertEquals(37_500L, ProductCosting.lineCost(RecipeLine("Gula", "gula", qty("2.5")), mapOf("gula" to 15_000L)))
        assertEquals(2L, ProductCosting.lineCost(RecipeLine("Garam", "garam", qty("0.3")), mapOf("garam" to 5L))) // 1,5 → 2
        assertEquals(1L, ProductCosting.lineCost(RecipeLine("Garam", "garam", qty("0.25")), mapOf("garam" to 5L))) // 1,25 → 1
    }

    @Test fun `missing or unpriced material makes the HPP incomplete instead of silently cheaper`() {
        val cost = ProductCosting.cost(keripik, mapOf("singkong" to 30_000L))
        assertFalse(cost.complete)
        assertEquals(listOf("Minyak"), cost.unpricedLines)
        assertEquals(1_900_000L, cost.batchCostRupiah)
        val zeroPrice = ProductCosting.cost(keripik, prices + ("minyak" to 0L))
        assertEquals(listOf("Minyak"), zeroPrice.unpricedLines)
    }

    @Test fun `empty recipe is reported, not shown as HPP zero`() {
        val cost = ProductCosting.cost(keripik.copy(recipe = emptyList()), prices)
        assertFalse(cost.hasRecipe)
        assertEquals(0L, cost.batchCostRupiah)
    }

    @Test fun `new oil price shows its impact on every product that uses oil (spec 20)`() {
        val other = keripik.copy(id = "lain", name = "Tanpa minyak", recipe = listOf(RecipeLine("Gas", amountRupiah = 100_000)))
        val changes = ProductCosting.priceImpact(listOf(keripik, other), prices, "minyak", 27_500)
        assertEquals(listOf("keripik"), changes.map { it.product.id })
        val change = changes.single()
        assertEquals(6_000L, change.before.hppPerBaseUnitRupiah)
        assertEquals(2_450_000L, change.after.batchCostRupiah)
        assertEquals(listOf(6_125L, 245_000L), change.after.perSaleUnit.map { it.hppRupiah })
        assertEquals(listOf(1_875L, 55_000L), change.after.perSaleUnit.map { it.profitRupiah })
    }

    @Test fun `same price is no impact`() {
        assertTrue(ProductCosting.priceImpact(listOf(keripik), prices, "minyak", 25_000).isEmpty())
    }

    @Test fun `legacy component Rp X for N units becomes a per-unit cost`() {
        assertEquals(500L, ProductCosting.legacyComponentPerUnit(25_000, qty("50")))
        assertEquals(3_333L, ProductCosting.legacyComponentPerUnit(10_000, qty("3")))
        assertEquals(3_000L, ProductCosting.legacyComponentPerUnit(18_000, qty("6")))
    }

    @Test fun `product stock - production adds, sale subtracts, count sets, and it may go negative`() {
        fun m(type: ProductMovementType, q: String) = ProductMovement(type, qty(q))
        assertEquals(0, qty("330").compareTo(ProductStock.level(listOf(m(ProductMovementType.PRODUCTION, "400"), m(ProductMovementType.SALE, "40"), m(ProductMovementType.SALE, "30")))))
        assertEquals(0, qty("290").compareTo(ProductStock.level(listOf(m(ProductMovementType.SALE, "80"), m(ProductMovementType.ADJUSTMENT, "300"), m(ProductMovementType.SALE, "10")))))
        assertEquals(0, qty("-80").compareTo(ProductStock.level(listOf(m(ProductMovementType.SALE, "80")))))
    }

    @Test fun `stock splits into whole sale units and the rest`() {
        val (whole, rest) = ProductStock.split(qty("410"), qty("40"))
        assertEquals(0, qty("10").compareTo(whole))
        assertEquals(0, qty("10").compareTo(rest))
    }
}
```

- [ ] **Step 2: Jalankan tes, pastikan gagal**

Run (PowerShell, PATH dibersihkan): `./gradlew.bat :app:testDebugUnitTest --tests "com.mankeu.domain.ProductCostingTest"`
Expected: FAIL kompilasi, `Unresolved reference: ProductSpec`.

- [ ] **Step 3: Tulis implementasi**

```kotlin
package com.mankeu.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** A way a product is sold (spec #5): [content] base units per one of it (bungkus = 1, dus = 40), at its own price. */
data class SaleUnit(val id: String, val name: String, val content: BigDecimal, val priceRupiah: Long)

/**
 * One line of a per-batch recipe (spec #6, #20): [quantity] of material [materialId], priced at the
 * material's last purchase price, or a free [amountRupiah] such as gas or wages.
 */
data class RecipeLine(
    val name: String,
    val materialId: String? = null,
    val quantity: BigDecimal? = null,
    val amountRupiah: Long? = null,
)

data class ProductSpec(
    val id: String,
    val name: String,
    val baseUnit: String,
    /** Base units one batch yields. */
    val batchYield: BigDecimal,
    /** In display order; the first is the main unit the plan and the sale form start from. */
    val saleUnits: List<SaleUnit>,
    val recipe: List<RecipeLine>,
)

data class UnitCost(val unit: SaleUnit, val hppRupiah: Long) {
    val profitRupiah: Long get() = unit.priceRupiah - hppRupiah
}

data class ProductCost(
    val batchCostRupiah: Long,
    val hppPerBaseUnitRupiah: Long,
    val perSaleUnit: List<UnitCost>,
    /** Lines whose cost is unknown (material removed or never priced). The HPP is then a floor, not the HPP. */
    val unpricedLines: List<String>,
    val hasRecipe: Boolean,
) {
    val complete: Boolean get() = hasRecipe && unpricedLines.isEmpty()
}

data class HppChange(val product: ProductSpec, val before: ProductCost, val after: ProductCost)

object ProductCosting {
    fun lineCost(line: RecipeLine, materialPrices: Map<String, Long>): Long? = if (line.materialId != null) {
        val price = materialPrices[line.materialId]?.takeIf { it > 0 }
        val quantity = line.quantity
        if (price == null || quantity == null) null else TransactionCalculation.computeTotalRupiah(quantity, price)
    } else {
        line.amountRupiah?.takeIf { it > 0 }
    }

    /** HPP of [content] base units: batch cost × content ÷ yield, rounded half up once. */
    fun hpp(batchCostRupiah: Long, batchYield: BigDecimal, content: BigDecimal): Long =
        BigDecimal.valueOf(batchCostRupiah).multiply(content).divide(batchYield, 0, RoundingMode.HALF_UP).longValueExact()

    fun cost(product: ProductSpec, materialPrices: Map<String, Long>): ProductCost {
        val costs = product.recipe.map { it to lineCost(it, materialPrices) }
        val batch = costs.sumOf { it.second ?: 0L }
        return ProductCost(
            batchCostRupiah = batch,
            hppPerBaseUnitRupiah = hpp(batch, product.batchYield, BigDecimal.ONE),
            perSaleUnit = product.saleUnits.map { UnitCost(it, hpp(batch, product.batchYield, it.content)) },
            unpricedLines = costs.filter { it.second == null }.map { it.first.name },
            hasRecipe = product.recipe.isNotEmpty(),
        )
    }

    /** What [newPriceRupiah] for [materialId] does to every product whose recipe uses it (spec #20). */
    fun priceImpact(
        products: List<ProductSpec>,
        materialPrices: Map<String, Long>,
        materialId: String,
        newPriceRupiah: Long,
    ): List<HppChange> {
        val after = materialPrices + (materialId to newPriceRupiah)
        return products.filter { product -> product.recipe.any { it.materialId == materialId } }
            .map { HppChange(it, cost(it, materialPrices), cost(it, after)) }
            .filter { it.before.batchCostRupiah != it.after.batchCostRupiah }
    }

    /** Schema 12 / template v4 said "Rp [amountRupiah] covers [yieldQuantity] units"; see `LegacyProductShape`. */
    fun legacyComponentPerUnit(amountRupiah: Long, yieldQuantity: BigDecimal): Long =
        BigDecimal.valueOf(amountRupiah).divide(yieldQuantity, 0, RoundingMode.HALF_UP).longValueExact()
}

enum class ProductMovementType { PRODUCTION, SALE, ADJUSTMENT }

/** [quantity] in base units. */
data class ProductMovement(val type: ProductMovementType, val quantity: BigDecimal)

object ProductStock {
    /**
     * [movements] oldest first. Unlike material stock this may go negative: sales recorded before
     * any production are real sales, and hiding them at zero would make the count lie.
     */
    fun level(movements: List<ProductMovement>): BigDecimal = movements.fold(BigDecimal.ZERO) { level, m ->
        when (m.type) {
            ProductMovementType.PRODUCTION -> level + m.quantity
            ProductMovementType.SALE -> level - m.quantity
            ProductMovementType.ADJUSTMENT -> m.quantity
        }
    }

    /** 410 bungkus with dus = 40 → (10 dus, 10 bungkus left). */
    fun split(level: BigDecimal, content: BigDecimal): Pair<BigDecimal, BigDecimal> {
        val whole = level.divide(content, 0, RoundingMode.DOWN)
        return whole to level - whole * content
    }
}
```

- [ ] **Step 4: Jalankan tes, pastikan lulus**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.mankeu.domain.ProductCostingTest"`
Expected: PASS (10 tes).

- [ ] **Step 5: Commit**

Tambahkan entri `logs/ACTIVITY_LOG.md` (paling atas): goal "Fase 1 Task 1: hitungan HPP per batch dan stok produk",
berkas, hasil tes.

```bash
git add app/src/main/java/com/mankeu/domain/ProductCosting.kt app/src/test/java/com/mankeu/domain/ProductCostingTest.kt logs/ACTIVITY_LOG.md
git commit -m "feat(domain): HPP per batch and sale unit, material price impact, product stock (spec #5 #6 #9 #20)"
```

---

### Task 2: Schema 13, migrasi, backup, dan lapisan data produk

Tugas terbesar. Model: paling mampu. Build harus hijau di akhir tugas; UI lama tetap jalan lewat adaptor.

**Files:**
- Modify: `app/src/main/java/com/mankeu/data/Entities.kt` (ProductEntity, hapus ProductCostComponentEntity, 3 entitas baru, FinancialEventEntity)
- Create: `app/src/main/java/com/mankeu/data/LegacyProductShape.kt`
- Create: `app/src/main/java/com/mankeu/data/Migration12To13.kt`
- Create: `app/src/main/java/com/mankeu/data/ProductRecord.kt`
- Modify: `app/src/main/java/com/mankeu/data/AppDatabase.kt`
- Create: `app/src/main/java/com/mankeu/backup/BackupUpgrade.kt`
- Modify: `app/src/main/java/com/mankeu/backup/EncryptedBackupManager.kt` (`prepareRestore`, doc `OLDEST_RESTORABLE_SCHEMA`)
- Modify: `app/src/main/java/com/mankeu/data/FinanceDao.kt` (query produk)
- Modify: `app/src/main/java/com/mankeu/data/FinanceRepository.kt` (`products`, `saveProduct`, `recordProduction`, `countProductStock`, impor/ekspor v4 lewat adaptor)
- Modify: `app/src/main/java/com/mankeu/data/PlanAssembly.kt` (`products`, `planInputs`)
- Modify: `app/src/main/java/com/mankeu/exporter/FinanceWorkbookExporter.kt` (`ProductWithComponents` → `ProductRecord`)
- Modify: `app/src/main/java/com/mankeu/presentation/AppViewModel.kt`, `presentation/screens/PlanScreen.kt` (kompilasi saja, lewat adaptor)
- Generate: `app/schemas/com.mankeu.data.AppDatabase/13.json`
- Test: `app/src/androidTest/java/com/mankeu/data/Migration12To13Test.kt`, `app/src/test/java/com/mankeu/backup/BackupUpgradeTest.kt`, `app/src/test/java/com/mankeu/data/LegacyProductShapeTest.kt`
- Fix call sites: `PlanAssemblyTest.kt`, `TemplateImportTest.kt`, `ImportCommitTest.kt`, `ModuleScreenshotTest.kt`, `ScaleSimulationTest.kt` (grep `ProductEntity(`, `ProductCostComponentEntity`, `ProductWithComponents`)
- Docs/log: `docs/DATABASE_MIGRATIONS.md`, `logs/DECISION_LOG.md` (DEC-028), `logs/ACTIVITY_LOG.md`

**Interfaces:**
- Consumes (Task 1): `ProductSpec`, `SaleUnit`, `RecipeLine`, `ProductCosting`, `ProductStock`, `ProductMovement`, `ProductMovementType`.
- Produces:
  - Entitas `ProductEntity(id, projectId, name, unit, batchYieldText = "1", targetPerMonthText, sortOrder, archived, createdAtEpochMillis, updatedAtEpochMillis, importBatchId, sellFromMonth)` (tanpa `priceRupiah`)
  - `ProductUnitEntity(id, productId, name, contentText, priceRupiah, sortOrder = 0)`
  - `ProductRecipeLineEntity(id, productId, name, stockItemId = null, quantityText = null, amountRupiah = null, sortOrder = 0)`
  - `ProductMovementEntity(id, productId, movementType, quantityText, batchesText = null, dateEpochDay, notes = "", createdAtEpochMillis)`
  - `FinancialEventEntity.stockItemId: String? = null`
  - `data class ProductRecord(product, units, recipe, movements)` dengan `spec: ProductSpec`, `stock: BigDecimal`, `mainUnit: ProductUnitEntity?`
  - `data class SaleUnitDraft(val id: String? = null, val name: String, val contentText: String, val priceRupiah: Long)`
  - `data class RecipeLineDraft(val name: String, val stockItemId: String? = null, val quantityText: String? = null, val amountRupiah: Long? = null)`
  - `data class ProductDraft(val name: String, val baseUnit: String, val batchYieldText: String, val units: List<SaleUnitDraft>, val recipe: List<RecipeLineDraft>, val targetPerMonthText: String? = null, val sellFromMonth: Int = 1)`
  - `FinanceRepository.products(projectId: String): Flow<List<ProductRecord>>`
  - `FinanceRepository.saveProduct(projectId: String, id: String?, draft: ProductDraft): String`
  - `FinanceRepository.recordProduction(productId: String, batches: BigDecimal, date: LocalDate, notes: String)`
  - `FinanceRepository.countProductStock(productId: String, counted: BigDecimal, date: LocalDate, notes: String)`
  - `PlanAssembly.products(rows: List<ProductRecord>, materialPrices: Map<String, Long>): List<PlanProduct>`
  - `PlanAssembly.planInputs(project, products: List<ProductRecord>, planItems, categories, today, materialPrices: Map<String, Long>)`
  - `LegacyProductShape.saleUnitId(productId: String): String`, `.saleUnit(...)`, `.recipeLine(...)`, `.draft(...)`, `.components(...)`
  - `BackupUpgrade.toCurrent(snapshot: PortableBackup): PortableBackup`

- [ ] **Step 1: Ubah entitas**

Di `Entities.kt`, ganti `ProductEntity` dan `ProductCostComponentEntity` dengan:

```kotlin
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
    /** [com.mankeu.domain.ProductMovementType.PRODUCTION] or `ADJUSTMENT` (a count). */
    val movementType: String,
    val quantityText: String,
    /** Production only: how many batches made [quantityText]. */
    val batchesText: String? = null,
    val dateEpochDay: Long,
    val notes: String = "",
    val createdAtEpochMillis: Long,
)
```

Di `FinancialEventEntity`, tambahkan setelah `usefulLifeMonths` dan tambahkan `Index("stockItemId")` ke `indices`:

```kotlin
    /** The material an expense bought (spec #20); the latest such purchase sets the material's price. */
    val stockItemId: String? = null,
```

- [ ] **Step 2: Naikkan versi database, daftarkan tabel**

Di `AppDatabase.kt`:
- `entities`: ganti `ProductCostComponentEntity::class` dengan `ProductUnitEntity::class, ProductRecipeLineEntity::class, ProductMovementEntity::class`.
- `version = 13`.
- `DATA_TABLES`: ganti `"product_cost_components"` dengan `"product_units", "product_recipe_lines", "product_movements"` (setelah `"products"`; induk sebelum anak).
- Builder: tambahkan `.addMigrations(Migration12To13)` sebelum `.addCallback(...)`.

Lalu build supaya Room menulis `13.json`:
Run: `./gradlew.bat :app:compileDebugKotlin`. Kompilasi akan gagal di pemanggil lama; itu diperbaiki di Step 6–9. Jalankan
ulang setelah Step 9 dan pastikan `app/schemas/com.mankeu.data.AppDatabase/13.json` muncul.

- [ ] **Step 3: Tulis adaptor bentuk lama dan tesnya**

`app/src/main/java/com/mankeu/data/LegacyProductShape.kt`:

```kotlin
package com.mankeu.data

import com.mankeu.domain.ProductCosting
import com.mankeu.domain.TransactionCalculation
import java.math.BigDecimal
import java.math.MathContext

/** One recipe line as schema 12 and template v4 wrote it: Rp [amountRupiah] covers [yieldText] units. */
data class ComponentDraft(val name: String, val amountRupiah: Long, val yieldText: String = "1")

/**
 * The schema-12 / template-v4 product shape, carried forward per spec #1: one unit at one price and
 * components "Rp X covers N units". It becomes a batch of 1 base unit, that unit as the first sale
 * unit (content 1) and each component a free line of round(X ÷ N) (DEC-028 §6). The migration, a
 * schema-12 backup, and until Task 4/5 the v4 import and the plan's product sheet all go through here.
 * Ids are derived from the old ids, so two devices migrating on their own produce the same rows.
 */
object LegacyProductShape {
    fun saleUnitId(productId: String) = "$productId-u0"

    fun saleUnit(productId: String, unit: String, priceRupiah: Long) =
        ProductUnitEntity(saleUnitId(productId), productId, unit, "1", priceRupiah, 0)

    fun recipeLine(id: String, productId: String, name: String, amountRupiah: Long, yieldText: String?, sortOrder: Int) =
        ProductRecipeLineEntity(
            id = id,
            productId = productId,
            name = name,
            amountRupiah = ProductCosting.legacyComponentPerUnit(amountRupiah, TransactionCalculation.parseQuantity(yieldText) ?: BigDecimal.ONE),
            sortOrder = sortOrder,
        )

    fun draft(name: String, unit: String, priceRupiah: Long, targetPerMonthText: String?, components: List<ComponentDraft>, sellFromMonth: Int) =
        ProductDraft(
            name = name,
            baseUnit = unit,
            batchYieldText = "1",
            units = listOf(SaleUnitDraft(name = unit, contentText = "1", priceRupiah = priceRupiah)),
            recipe = components.filter { it.name.isNotBlank() }.map { c ->
                RecipeLineDraft(c.name, amountRupiah = ProductCosting.legacyComponentPerUnit(c.amountRupiah, TransactionCalculation.parseQuantity(c.yieldText) ?: BigDecimal.ONE))
            },
            targetPerMonthText = targetPerMonthText,
            sellFromMonth = sellFromMonth,
        )

    /**
     * The other direction, for the v4 export and the plan sheet until Task 4/5: each line's cost
     * covers batchYield ÷ main content units. Lossless for products of the legacy shape only.
     */
    fun components(record: ProductRecord, materialPrices: Map<String, Long>): List<ComponentDraft> {
        val spec = record.spec
        val content = spec.saleUnits.firstOrNull()?.content ?: BigDecimal.ONE
        val covers = spec.batchYield.divide(content, MathContext.DECIMAL64).stripTrailingZeros().toPlainString()
        return spec.recipe.map { ComponentDraft(it.name, ProductCosting.lineCost(it, materialPrices) ?: 0L, covers) }
    }
}
```

Pindahkan `data class ComponentDraft` dari `FinanceRepository.kt` ke berkas ini (hapus definisi lamanya).

`app/src/test/java/com/mankeu/data/LegacyProductShapeTest.kt`:

```kotlin
package com.mankeu.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyProductShapeTest {
    @Test fun `old product becomes batch of one with its unit as first sale unit`() {
        val draft = LegacyProductShape.draft(
            "Kopi susu", "cup", 10_000, "700",
            listOf(ComponentDraft("Susu", 18_000, "6"), ComponentDraft("Plastik", 25_000, "50"), ComponentDraft("Gula", 10_000, "3")),
            sellFromMonth = 2,
        )
        assertEquals("1", draft.batchYieldText)
        assertEquals(listOf(SaleUnitDraft(name = "cup", contentText = "1", priceRupiah = 10_000)), draft.units)
        assertEquals(listOf(3_000L, 500L, 3_333L), draft.recipe.map { it.amountRupiah })
        assertEquals(2, draft.sellFromMonth)
    }

    @Test fun `sale unit id is derived so independent migrations agree`() {
        assertEquals("p1-u0", LegacyProductShape.saleUnit("p1", "cup", 1).id)
    }
}
```

- [ ] **Step 4: Tulis `ProductRecord`**

`app/src/main/java/com/mankeu/data/ProductRecord.kt`:

```kotlin
package com.mankeu.data

import com.mankeu.domain.ProductMovement
import com.mankeu.domain.ProductMovementType
import com.mankeu.domain.ProductSpec
import com.mankeu.domain.ProductStock
import com.mankeu.domain.RecipeLine
import com.mankeu.domain.SaleUnit
import com.mankeu.domain.TransactionCalculation
import java.math.BigDecimal

/** A product with everything stored about it; [spec] and [stock] are what screens and reports use. */
data class ProductRecord(
    val product: ProductEntity,
    val units: List<ProductUnitEntity>,
    val recipe: List<ProductRecipeLineEntity>,
    val movements: List<ProductMovementEntity> = emptyList(),
) {
    val spec: ProductSpec
        get() = ProductSpec(
            id = product.id,
            name = product.name,
            baseUnit = product.unit,
            batchYield = TransactionCalculation.parseQuantity(product.batchYieldText) ?: BigDecimal.ONE,
            saleUnits = units.sortedBy { it.sortOrder }.map {
                SaleUnit(it.id, it.name, TransactionCalculation.parseQuantity(it.contentText) ?: BigDecimal.ONE, it.priceRupiah)
            },
            recipe = recipe.sortedBy { it.sortOrder }.map {
                RecipeLine(it.name, it.stockItemId, TransactionCalculation.parseQuantity(it.quantityText), it.amountRupiah)
            },
        )

    /** The first sale unit (DEC-028 §8): the plan's unit and the sale form's default. */
    val mainUnit: ProductUnitEntity? get() = units.minByOrNull { it.sortOrder }

    /** Base units on hand. Task 3 adds the sales. */
    val stock: BigDecimal
        get() = ProductStock.level(
            movements.sortedWith(compareBy({ it.dateEpochDay }, { it.createdAtEpochMillis })).map {
                // parseQuantity returns null for "0", which a count may legitimately be.
                ProductMovement(ProductMovementType.valueOf(it.movementType), TransactionCalculation.parseQuantity(it.quantityText) ?: BigDecimal.ZERO)
            },
        )
}
```

Hapus `data class ProductWithComponents` dari `PlanAssembly.kt`.

- [ ] **Step 5: Tulis migrasi**

`app/src/main/java/com/mankeu/data/Migration12To13.kt`. **Penting:** setiap `CREATE TABLE`/`CREATE INDEX` di bawah harus
sama persis dengan `createSql` di `13.json` hasil Step 2. Salin dari JSON dan ganti `${TABLE_NAME}` dengan nama
tabelnya. Bila ada perbedaan, JSON yang benar, karena `MigrationTestHelper` memvalidasi skema terhadapnya.

```kotlin
package com.mankeu.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mankeu.sync.SyncSchema

/**
 * Schema 13 (Fase 1, DEC-028): products get a base unit, a batch yield, sale units and a per-batch
 * recipe; expenses can name the material they bought. Old products are reshaped by
 * [LegacyProductShape]. `products` is rebuilt (its price moves to `product_units`), which drops its
 * sync triggers, so [SyncSchema.createTriggers] runs at the end (see SyncSchema's KDoc).
 */
object Migration12To13 : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Read the old shape before anything is dropped. Nothing references products except
        // product_cost_components, which goes first, so the rebuild cascades into nothing.
        val units = db.query("SELECT id, unit, priceRupiah FROM products").use { c ->
            buildList { while (c.moveToNext()) add(LegacyProductShape.saleUnit(c.getString(0), c.getString(1), c.getLong(2))) }
        }
        val lines = db.query("SELECT id, productId, name, amountRupiah, yieldText, sortOrder FROM product_cost_components").use { c ->
            buildList {
                while (c.moveToNext()) add(LegacyProductShape.recipeLine(c.getString(0), c.getString(1), c.getString(2), c.getLong(3), c.getString(4), c.getInt(5)))
            }
        }
        db.execSQL("DROP TABLE product_cost_components")

        db.execSQL("CREATE TABLE IF NOT EXISTS `products_new` (`id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `name` TEXT NOT NULL, `unit` TEXT NOT NULL, `batchYieldText` TEXT NOT NULL DEFAULT '1', `targetPerMonthText` TEXT, `sortOrder` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, `importBatchId` TEXT, `sellFromMonth` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`id`), FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL(
            "INSERT INTO products_new (id, projectId, name, unit, batchYieldText, targetPerMonthText, sortOrder, archived, createdAtEpochMillis, updatedAtEpochMillis, importBatchId, sellFromMonth) " +
                "SELECT id, projectId, name, unit, '1', targetPerMonthText, sortOrder, archived, createdAtEpochMillis, updatedAtEpochMillis, importBatchId, sellFromMonth FROM products",
        )
        db.execSQL("DROP TABLE products")
        db.execSQL("ALTER TABLE products_new RENAME TO products")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_projectId` ON `products` (`projectId`)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `product_units` (`id` TEXT NOT NULL, `productId` TEXT NOT NULL, `name` TEXT NOT NULL, `contentText` TEXT NOT NULL, `priceRupiah` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_units_productId` ON `product_units` (`productId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `product_recipe_lines` (`id` TEXT NOT NULL, `productId` TEXT NOT NULL, `name` TEXT NOT NULL, `stockItemId` TEXT, `quantityText` TEXT, `amountRupiah` INTEGER, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_recipe_lines_productId` ON `product_recipe_lines` (`productId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_recipe_lines_stockItemId` ON `product_recipe_lines` (`stockItemId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `product_movements` (`id` TEXT NOT NULL, `productId` TEXT NOT NULL, `movementType` TEXT NOT NULL, `quantityText` TEXT NOT NULL, `batchesText` TEXT, `dateEpochDay` INTEGER NOT NULL, `notes` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_movements_productId` ON `product_movements` (`productId`)")

        units.forEach { u ->
            db.insert("product_units", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                put("id", u.id); put("productId", u.productId); put("name", u.name)
                put("contentText", u.contentText); put("priceRupiah", u.priceRupiah); put("sortOrder", u.sortOrder)
            })
        }
        lines.forEach { l ->
            db.insert("product_recipe_lines", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                put("id", l.id); put("productId", l.productId); put("name", l.name)
                putNull("stockItemId"); putNull("quantityText"); put("amountRupiah", l.amountRupiah); put("sortOrder", l.sortOrder)
            })
        }

        db.execSQL("ALTER TABLE financial_events ADD COLUMN `stockItemId` TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_events_stockItemId` ON `financial_events` (`stockItemId`)")

        SyncSchema.createTriggers(db)
    }
}
```

Perbarui KDoc `AppDatabase.create` dan `SyncSchema` bila menyebut "only schema 12" supaya menyebut 13.

- [ ] **Step 6: Tulis tes migrasi (androidTest)**

Periksa dulu bahwa `13.json` dan `12.json` ikut ke aset androidTest. Plugin Room biasanya mengaturnya sendiri; bila
`MigrationTestHelper` gagal menemukan `12.json`, tambahkan `sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")`
di blok `android` `app/build.gradle.kts`.

`app/src/androidTest/java/com/mankeu/data/Migration12To13Test.kt`:

```kotlin
package com.mankeu.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration12To13Test {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun productsKeepTheirPriceAsFirstSaleUnitAndComponentsBecomePerUnitLines() {
        helper.createDatabase(DB, 12).use { db ->
            db.execSQL("INSERT INTO businesses (id, name, currency, archived, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('b', 'Usaha', 'IDR', 0, 0, 0)")
            db.execSQL("INSERT INTO projects (id, businessId, name, isGeneral, archived, createdAtEpochMillis, description, updatedAtEpochMillis) VALUES ('p', 'b', 'Umum', 1, 0, 0, '', 0)")
            db.execSQL("INSERT INTO products (id, projectId, name, unit, priceRupiah, targetPerMonthText, sortOrder, archived, createdAtEpochMillis, updatedAtEpochMillis, importBatchId, sellFromMonth) VALUES ('kopi', 'p', 'Kopi susu', 'cup', 10000, '700', 0, 0, 0, 0, NULL, 2)")
            db.execSQL("INSERT INTO product_cost_components (id, productId, name, amountRupiah, yieldText, sortOrder) VALUES ('c1', 'kopi', 'Susu', 18000, '6', 0)")
            db.execSQL("INSERT INTO product_cost_components (id, productId, name, amountRupiah, yieldText, sortOrder) VALUES ('c2', 'kopi', 'Gula', 10000, '3', 1)")
        }

        helper.runMigrationsAndValidate(DB, 13, true, Migration12To13).use { db ->
            db.query("SELECT name, unit, batchYieldText, targetPerMonthText, sellFromMonth FROM products").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(listOf("Kopi susu", "cup", "1", "700", "2"), (0..4).map { c.getString(it) })
            }
            db.query("SELECT id, name, contentText, priceRupiah FROM product_units WHERE productId = 'kopi'").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(listOf("kopi-u0", "cup", "1", "10000"), (0..3).map { c.getString(it) })
            }
            db.query("SELECT id, amountRupiah, stockItemId FROM product_recipe_lines ORDER BY sortOrder").use { c ->
                val rows = buildList { while (c.moveToNext()) add(Triple(c.getString(0), c.getLong(1), c.isNull(2))) }
                assertEquals(listOf(Triple("c1", 3_000L, true), Triple("c2", 3_333L, true)), rows)
            }
            val triggers = db.query("SELECT name FROM sqlite_master WHERE type = 'trigger'").use { c -> buildSet { while (c.moveToNext()) add(c.getString(0)) } }
            listOf("products", "product_units", "product_recipe_lines", "product_movements").forEach { table ->
                assertTrue("missing sync trigger on $table", "sync_${table}_ins" in triggers)
            }
        }
    }

    private companion object { const val DB = "migration-12-13" }
}
```

Bila kolom `INSERT` untuk `businesses`/`projects` di atas tidak cocok dengan `12.json`, sesuaikan dengan `12.json` (bukan sebaliknya).

- [ ] **Step 7: Tulis upgrade backup schema 12 dan tesnya**

`app/src/main/java/com/mankeu/backup/BackupUpgrade.kt`:

```kotlin
package com.mankeu.backup

import com.mankeu.data.LegacyProductShape

/**
 * Brings a snapshot from an older schema to the current one, the way the Room migrations bring a
 * database (spec #1 carries schema 12 forward). Columns a newer schema dropped are ignored by the
 * restore and new columns take their SQL default, so only reshaped tables need work here.
 */
object BackupUpgrade {
    fun toCurrent(snapshot: PortableBackup): PortableBackup =
        if (snapshot.schemaVersion == 12) from12(snapshot) else snapshot

    /** Mirrors `Migration12To13`: price → first sale unit, components → per-unit recipe lines. */
    private fun from12(snapshot: PortableBackup): PortableBackup {
        val byName = snapshot.tables.associateBy { it.name }
        val products = byName.getValue("products")
        val components = byName.getValue("product_cost_components")
        fun BackupTable.text(row: List<BackupValue>, column: String) = (row[columns.indexOf(column)] as? BackupValue.Text)?.value
        fun BackupTable.long(row: List<BackupValue>, column: String) = (row[columns.indexOf(column)] as BackupValue.Integer).value

        val units = BackupTable(
            name = "product_units",
            columns = listOf("id", "productId", "name", "contentText", "priceRupiah", "sortOrder"),
            rows = products.rows.map { row ->
                val unit = LegacyProductShape.saleUnit(products.text(row, "id")!!, products.text(row, "unit")!!, products.long(row, "priceRupiah"))
                listOf(BackupValue.Text(unit.id), BackupValue.Text(unit.productId), BackupValue.Text(unit.name), BackupValue.Text(unit.contentText), BackupValue.Integer(unit.priceRupiah), BackupValue.Integer(0))
            },
        )
        val lines = BackupTable(
            name = "product_recipe_lines",
            columns = listOf("id", "productId", "name", "stockItemId", "quantityText", "amountRupiah", "sortOrder"),
            rows = components.rows.map { row ->
                val line = LegacyProductShape.recipeLine(
                    components.text(row, "id")!!, components.text(row, "productId")!!, components.text(row, "name")!!,
                    components.long(row, "amountRupiah"), components.text(row, "yieldText"), components.long(row, "sortOrder").toInt(),
                )
                listOf(BackupValue.Text(line.id), BackupValue.Text(line.productId), BackupValue.Text(line.name), BackupValue.Null, BackupValue.Null, BackupValue.Integer(line.amountRupiah!!), BackupValue.Integer(line.sortOrder.toLong()))
            },
        )
        val movements = BackupTable(name = "product_movements", columns = listOf("id"), rows = emptyList())
        return snapshot.copy(
            schemaVersion = 13,
            tables = snapshot.tables.filterNot { it.name == "product_cost_components" } + units + lines + movements,
        )
    }
}
```

Sesuaikan nama field `PortableBackup`/`BackupTable` dengan definisinya di `EncryptedBackupManager.kt:22-40`
(`schemaVersion`, `tables`, `name`, `columns`, `rows`). Bila salah satu bukan `data class`, pakai konstruktornya.

Di `EncryptedBackupManager.prepareRestore`, ubah menjadi
`val snapshot = BackupUpgrade.toCurrent(PortableBackupCodec.decode(PasswordBackupCipher.decrypt(envelope, password)))`.
`OLDEST_RESTORABLE_SCHEMA` tetap 12; ganti KDoc-nya menjadi: "Nothing older than 12 is restorable (spec #1). A later
migration that reshapes a table must teach [BackupUpgrade] the same reshaping."

`app/src/test/java/com/mankeu/backup/BackupUpgradeTest.kt`:

```kotlin
package com.mankeu.backup

import com.mankeu.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BackupUpgradeTest {
    private fun table(name: String, columns: List<String> = listOf("id"), rows: List<List<BackupValue>> = emptyList()) = BackupTable(name, columns, rows)

    private val schema12Tables = (AppDatabase.DATA_TABLES - listOf("product_units", "product_recipe_lines", "product_movements")).map { name ->
        when (name) {
            "products" -> table(name, listOf("id", "projectId", "name", "unit", "priceRupiah"), listOf(listOf(BackupValue.Text("kopi"), BackupValue.Text("p"), BackupValue.Text("Kopi"), BackupValue.Text("cup"), BackupValue.Integer(10_000))))
            else -> table(name)
        }
    } + table(
        "product_cost_components",
        listOf("id", "productId", "name", "amountRupiah", "yieldText", "sortOrder"),
        listOf(listOf(BackupValue.Text("c1"), BackupValue.Text("kopi"), BackupValue.Text("Susu"), BackupValue.Integer(18_000), BackupValue.Text("6"), BackupValue.Integer(0))),
    )

    @Test fun `schema 12 snapshot gets the current table set and the reshaped product rows`() {
        val upgraded = BackupUpgrade.toCurrent(PortableBackup(12, 0, schema12Tables))
        assertEquals(13, upgraded.schemaVersion)
        assertEquals(AppDatabase.DATA_TABLES.toSet(), upgraded.tables.map { it.name }.toSet())
        val units = upgraded.tables.single { it.name == "product_units" }
        assertEquals(listOf(BackupValue.Text("kopi-u0"), BackupValue.Text("kopi"), BackupValue.Text("cup"), BackupValue.Text("1"), BackupValue.Integer(10_000), BackupValue.Integer(0)), units.rows.single())
        val lines = upgraded.tables.single { it.name == "product_recipe_lines" }
        assertEquals(BackupValue.Integer(3_000), lines.rows.single()[5])
    }

    @Test fun `current snapshot passes through untouched`() {
        val current = PortableBackup(13, 0, AppDatabase.DATA_TABLES.map { table(it) })
        assertSame(current, BackupUpgrade.toCurrent(current))
    }
}
```

Bila konstruktor `PortableBackup` punya urutan atau nama parameter lain, sesuaikan (lihat `capture()` di `EncryptedBackupManager.kt`).

- [ ] **Step 8: DAO dan repository produk**

Di `FinanceDao.kt`, ganti empat query `product_cost_components` dengan:

```kotlin
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
```

Di `FinanceRepository.kt`, tambahkan di samping `EventDraft` (tipe draft dari bagian Interfaces di atas):

```kotlin
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
```

Ganti `products(projectId)`, `productsOf`, dan `saveProduct` lama dengan:

```kotlin
    /** Products of a business unit with sale units, recipe and stock movements (spec #5 #6 #9). */
    fun products(projectId: String): Flow<List<ProductRecord>> = combine(
        dao.observeProducts(projectId),
        dao.observeProductUnits(projectId),
        dao.observeRecipeLines(projectId),
        dao.observeProductMovements(projectId),
    ) { products, units, recipe, movements ->
        val unitsOf = units.groupBy { it.productId }
        val recipeOf = recipe.groupBy { it.productId }
        val movementsOf = movements.groupBy { it.productId }
        products.map { ProductRecord(it, unitsOf[it.id].orEmpty(), recipeOf[it.id].orEmpty(), movementsOf[it.id].orEmpty()) }
    }

    private suspend fun productsOf(projectId: String): List<ProductRecord> =
        dao.getProducts(projectId).map { ProductRecord(it, dao.getProductUnits(it.id), dao.getRecipeLines(it.id)) }

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
        val recipe = draft.recipe.filter { it.stockItemId != null || it.name.isNotBlank() }.map { line ->
            if (line.stockItemId != null) {
                val material = stockDao.getStockItem(line.stockItemId)
                require(material != null && material.businessId == project.businessId) { "Bahan tidak ditemukan di usaha ini." }
                require(TransactionCalculation.parseQuantity(line.quantityText) != null) { "Jumlah ${material.name} per batch harus angka lebih dari nol." }
                // The material's name is kept on the line, so a removed material still shows what was used.
                line.copy(name = material.name, amountRupiah = null)
            } else {
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
```

Impor (`commitImport`, sekitar baris 714–735): hapus `priceRupiah = item.priceRupiah` dari `ProductEntity(...)`, lalu
ganti `dao.insertProductComponents(...)` dengan:

```kotlin
            dao.insertProductUnits(listOf(LegacyProductShape.saleUnit(productId, item.unit.trim(), item.priceRupiah)))
            dao.insertRecipeLines(item.components.mapIndexed { i, c -> LegacyProductShape.recipeLine(UUID.randomUUID().toString(), productId, c.name, c.amountRupiah, c.yieldText, i) })
```

Panggilan `planAnalysis` dan sejenisnya yang memanggil `PlanAssembly.planInputs`/`products` sekarang butuh harga bahan:
`stockDao.getAllStockItems(project.businessId).associate { it.id to it.basePriceRupiah }`. Task 3 menggantinya dengan harga
belanja terakhir.

- [ ] **Step 9: Sesuaikan PlanAssembly, exporter, ViewModel, PlanScreen**

`PlanAssembly.products`:

```kotlin
    /**
     * Until Fase 3 rewrites the plan it prices each product by its main sale unit (DEC-028 §8), with
     * the HPP of that unit as its one variable cost, so the plan and the Produk tab show the same number.
     */
    fun products(rows: List<ProductRecord>, materialPrices: Map<String, Long>): List<PlanProduct> = rows.mapNotNull { row ->
        val spec = row.spec
        val main = spec.saleUnits.firstOrNull() ?: return@mapNotNull null
        val hpp = ProductCosting.cost(spec, materialPrices).perSaleUnit.first().hppRupiah
        PlanProduct(
            id = spec.id,
            name = spec.name,
            unit = main.name,
            priceRupiah = main.priceRupiah,
            targetPerMonth = TransactionCalculation.parseQuantity(row.product.targetPerMonthText),
            components = if (hpp > 0) listOf(CostComponent("HPP", hpp, BigDecimal.ONE)) else emptyList(),
            sellFromMonth = row.product.sellFromMonth,
        )
    }
```

`planInputs` mendapat parameter terakhir `materialPrices: Map<String, Long>` dan meneruskannya ke `products(...)`.
Ubah semua pemanggil (`grep -rn "planInputs\|PlanAssembly.products" app/src`). Di ViewModel, harga bahan =
`stockItems.value.associate { it.id to it.basePriceRupiah }`.

Exporter (`FinanceWorkbookExporter.kt`): `products: List<ProductRecord>`; lembar Produk ditulis dengan
`listOf(p.product.name, p.mainUnit?.name ?: p.product.unit, p.mainUnit?.priceRupiah ?: 0, quantity(p.product.targetPerMonthText), p.product.sellFromMonth)`;
lembar Resep dari `LegacyProductShape.components(p, materialPrices)`. Ini lossless karena sebelum Task 5 belum ada
produk berbentuk baru. Tambahkan `materialPrices: Map<String, Long> = emptyMap()` ke `FinanceExportBundle`, diisi oleh
`buildProjectExportBundle`.

ViewModel: `ProductWithComponents` → `ProductRecord` di state. `saveProduct(...)` tetap dengan tanda tangan lama dan
memanggil `repository.saveProduct(projectId, id, LegacyProductShape.draft(name, unit, priceRupiah, targetPerMonthText, components, sellFromMonth))`.
Tambahkan:

```kotlin
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
```

Tambahkan string `msg_production_saved` ("Produksi dicatat." / "Production recorded.") dan `msg_stock_count_saved`
("Stok produk diperbarui." / "Product stock updated.") ke kedua `strings.xml`.

PlanScreen (hanya supaya tetap terkompilasi; Task 5 menggantinya): `ProductWithComponents` → `ProductRecord`;
`row.product.priceRupiah` → `row.mainUnit?.priceRupiah ?: 0`; `row.product.unit` di baris ringkasan → `row.mainUnit?.name ?: row.product.unit`;
`PlanAssembly.products(listOf(row))` → `PlanAssembly.products(listOf(row), materialPrices)`, dengan `materialPrices` dari
`state.stockItems`. Di `ProductSheet`, harga awal dari `existing?.mainUnit?.priceRupiah` dan komponen awal dari
`LegacyProductShape.components(existing, materialPrices)`.

Perbaiki pemanggil di tes (`grep -rn "ProductEntity(\|ProductCostComponentEntity\|ProductWithComponents" app/src/test app/src/androidTest`):
- Hapus argumen harga dari `ProductEntity(...)`.
- Fixture produk menjadi `ProductRecord(ProductEntity(...), listOf(ProductUnitEntity("kopi-u0", "kopi", "cup", "1", 10_000)), listOf(ProductRecipeLineEntity("k1", "kopi", "Susu", amountRupiah = 3_000)))`.
- Untuk ekspektasi tes yang dulu memakai komponen `yieldText = "6"`, nilainya sekarang per unit (18.000 ÷ 6 = 3.000, ditulis dengan yield "1").

Sesuaikan ekspektasinya dan catat di laporan tes mana yang angkanya berubah, beserta alasannya.

- [ ] **Step 10: Jalankan semua tes**

Run:
1. `./gradlew.bat :app:testDebugUnitTest`. Expected: semua PASS (68 sebelumnya + tes baru).
2. `./gradlew.bat :app:lintDebug :app:assembleDebug`. Expected: hijau, dan `13.json` ter-commit.
3. Emulator menyala (`adb devices` menunjukkan `emulator-5554`):
   `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.mankeu.data.Migration12To13Test"`.
   Expected: PASS.
4. `./gradlew.bat :app:connectedDebugAndroidTest`. Expected: tidak ada kegagalan baru. `ModuleScreenshotTest`
   "Lampiran A" sudah gagal sejak sebelum Fase 0 dan boleh tetap gagal.

- [ ] **Step 11: Dokumentasi, DEC-028, commit**

- `docs/DATABASE_MIGRATIONS.md`: tambahkan bagian "12 → 13 (Fase 1)" berisi tabel baru, kolom `financial_events.stockItemId`,
  pembangunan ulang `products`, adaptor `LegacyProductShape` (dengan aturan pembulatannya), `BackupUpgrade`, dan perilaku
  sinkron antar versi (DEC-028 §7).
- `logs/DECISION_LOG.md`: entri `[DEC-028]` 2026-10-29, status Accepted. Context: Fase 1 spec #5/#6/#9/#20. Decision: sembilan
  poin "Keputusan rencana" di atas, masing-masing dengan alasan dan biaya bila salah. Affected: berkas Task 2–6.
- `logs/ACTIVITY_LOG.md`: entri Task 2.

```bash
git add -A app/src app/schemas docs/DATABASE_MIGRATIONS.md logs/
git commit -m "feat(data): schema 13 product model - sale units, batch recipe, product stock; migrate and restore schema 12"
```

---

### Task 3: Stok produk dari penjualan, harga bahan dari belanja terakhir

**Files:**
- Modify: `app/src/main/java/com/mankeu/data/FinanceDao.kt` (`ProductSaleRow`, `MaterialPurchaseRow`, query, `CostRow.unit`)
- Modify: `app/src/main/java/com/mankeu/data/ProductRecord.kt` (`sales`, `contentOf`)
- Modify: `app/src/main/java/com/mankeu/data/FinanceRepository.kt` (`products`, `stockItems`, `materialPrice`, `EventDraft.stockItemId`, validasi)
- Modify: `app/src/main/java/com/mankeu/domain/FinanceModels.kt` (`MoneyEvent.stockItemId`)
- Modify: `app/src/main/java/com/mankeu/domain/StockManagement.kt` (KDoc `StockItem.basePriceRupiah`)
- Modify: `app/src/main/java/com/mankeu/data/PlanAssembly.kt` (`actualMonth` mengonversi satuan)
- Test: `app/src/androidTest/java/com/mankeu/data/ProductStockFlowTest.kt`, `app/src/test/java/com/mankeu/data/PlanAssemblyTest.kt`

**Interfaces:**
- Consumes (Task 2): `ProductRecord`, `FinanceRepository.saveProduct`, `recordProduction`, `countProductStock`, `ProductDraft`.
- Produces:
  - `data class ProductSaleRow(val id: String, val productId: String, val quantityText: String?, val unit: String?, val dateEpochDay: Long, val createdAtEpochMillis: Long)`
  - `ProductRecord(product, units, recipe, movements, sales: List<ProductSaleRow> = emptyList())`, `fun contentOf(unitName: String?): BigDecimal`
  - `EventDraft.stockItemId: String? = null`, `MoneyEvent.stockItemId: String? = null`
  - `FinanceRepository.stockItems(...)`: `StockItem.basePriceRupiah` = harga belanja terakhir, bila tidak ada maka harga yang diisi saat bahan dibuat
  - `FinanceRepository.materialPrice(stockItemId: String): Long?`
  - `PlanAssembly.actualMonth(month, rows, products: List<ProductRecord>)`; unit penjualan dihitung dalam satuan utama

- [ ] **Step 1: Tulis tes alur yang gagal (androidTest)**

Ikuti pola setup `LedgerIntegrityTest.kt` (database in-memory dan `FinanceRepository`). Bagian intinya:

```kotlin
@RunWith(AndroidJUnit4::class)
class ProductStockFlowTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: FinanceRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repo = FinanceRepository(db)
    }
    @After fun tearDown() = db.close()

    @Test fun keripikGolden_productionSalesAndOilPrice() = runBlocking {
        val (businessId, projectId) = repo.createBusinessWithUnit("Keripik Bu Sri", "Umum")
        val book = repo.books(projectId).first().single { it.mode == BookMode.REALIZATION.name }
        val minyak = repo.createStockItem(businessId, projectId, false, "Minyak", "liter", 25_000, BigDecimal.ZERO, BigDecimal.ZERO)
        val singkong = repo.createStockItem(businessId, projectId, false, "Singkong", "kg", 30_000, BigDecimal.ZERO, BigDecimal.ZERO)
        val productId = repo.saveProduct(projectId, null, ProductDraft(
            name = "Keripik", baseUnit = "bungkus", batchYieldText = "400",
            units = listOf(SaleUnitDraft(name = "bungkus", contentText = "1", priceRupiah = 8_000), SaleUnitDraft(name = "dus", contentText = "40", priceRupiah = 300_000)),
            recipe = listOf(
                RecipeLineDraft("", stockItemId = singkong, quantityText = "50"),
                RecipeLineDraft("", stockItemId = minyak, quantityText = "20"),
                RecipeLineDraft("Bumbu & kemasan", amountRupiah = 300_000),
                RecipeLineDraft("Gas", amountRupiah = 100_000),
            ),
        ))
        suspend fun keripik() = repo.products(projectId).first().single { it.product.id == productId }
        suspend fun prices() = repo.stockItems(businessId, projectId).first().associate { it.id to it.basePriceRupiah }

        assertEquals(listOf(6_000L, 240_000L), ProductCosting.cost(keripik().spec, prices()).perSaleUnit.map { it.hppRupiah })

        repo.recordProduction(productId, BigDecimal.ONE, LocalDate.of(2026, 10, 1), "")
        assertEquals(0, BigDecimal(400).compareTo(keripik().stock))

        fun sale(qty: String, unit: String?) = EventDraft(businessId, projectId, book.id, FinancialEventType.CASH_INCOME, 600_000, LocalDate.of(2026, 10, 2), "Jual ke Toko A", "Penjualan", quantityText = qty, unit = unit, productId = productId)
        val saleId = repo.recordEvent(sale("2", "dus"))
        assertEquals(0, BigDecimal(320).compareTo(keripik().stock))
        repo.updateEvent(saleId, sale("3", "dus").copy(amountRupiah = 900_000))
        assertEquals(0, BigDecimal(280).compareTo(keripik().stock))
        repo.deleteEvent(saleId)
        assertEquals(0, BigDecimal(400).compareTo(keripik().stock))

        val purchase = repo.recordEvent(EventDraft(businessId, projectId, book.id, FinancialEventType.CASH_EXPENSE, 550_000, LocalDate.of(2026, 10, 3), "Minyak", "Bahan baku", quantityText = "20", unit = "liter", stockItemId = minyak))
        assertEquals(27_500L, prices()[minyak])
        assertEquals(27_500L, repo.materialPrice(minyak))
        assertEquals(listOf(6_125L, 245_000L), ProductCosting.cost(keripik().spec, prices()).perSaleUnit.map { it.hppRupiah })
        repo.deleteEvent(purchase)
        assertEquals(25_000L, prices()[minyak])

        val bad = runCatching { repo.recordEvent(sale("1", "karung")) }
        assertTrue(bad.exceptionOrNull()?.message.orEmpty().contains("karung"))
    }
}
```

Sesuaikan tanda tangan `createStockItem`, `createBusinessWithUnit`, dan nama kategori dengan kodenya
(`FinanceRepository.kt:319-335, 577-612`). `createStockItem` mengembalikan id. Bila kategori bernama dibutuhkan, beri
`categoryId = null`.

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.mankeu.data.ProductStockFlowTest"`
Expected: FAIL kompilasi pada `stockItemId`/`materialPrice`.

- [ ] **Step 3: Model dan validasi**

`MoneyEvent` (`FinanceModels.kt`): tambahkan `val stockItemId: String? = null` setelah `productId`. `EventDraft`: tambahkan
`/** The material an expense bought (spec #20). */ val stockItemId: String? = null`. Teruskan field ini di `toEvent()`,
`FinancialEventEntity.toDomain()`, dan `MoneyEvent.toEntity()` (`FinanceRepository.kt` sekitar 1025–1090).

Di `requireValid(draft)` tambahkan:

```kotlin
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
```

Belanja bahan harus punya harga satuan supaya bisa menjadi harga bahan. Di `recordEvent` dan `updateEvent`, ganti
`draft.toEvent()` dengan `draft.toEvent().withPurchasePrice()`:

```kotlin
    /** A material purchase without a unit price takes amount ÷ quantity, so it can price the material (spec #20). */
    private fun MoneyEvent.withPurchasePrice(): MoneyEvent {
        if (stockItemId == null || unitPriceRupiah != null) return this
        val quantity = TransactionCalculation.parseQuantity(quantityText) ?: return this
        return copy(unitPriceRupiah = TransactionCalculation.computeUnitPriceRupiah(amountRupiah, quantity))
    }
```

`deleteEvent`: bungkus dengan `database.withTransaction { dao.deleteEvent(eventId) }`. Tidak ada baris gerakan yang
perlu dibersihkan, karena stok dan harga diturunkan saat dibaca.

- [ ] **Step 4: Query turunan**

`FinanceDao.kt`:

```kotlin
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
```

```kotlin
    /** Realisasi sales that name a product: product stock subtracts these (DEC-028 §4). */
    @Query(
        """
        SELECT e.id, e.productId, e.quantityText, e.unit, e.dateEpochDay, e.createdAtEpochMillis
        FROM financial_events e INNER JOIN books b ON b.id = e.bookId
        WHERE e.projectId = :projectId AND e.productId IS NOT NULL AND e.type = :saleType AND b.mode = :mode
        """,
    )
    fun observeProductSales(projectId: String, saleType: String, mode: String): Flow<List<ProductSaleRow>>

    /** Oldest first, so the last row per material is its current price. */
    @Query(
        """
        SELECT stockItemId, unitPriceRupiah FROM financial_events
        WHERE businessId = :businessId AND stockItemId IS NOT NULL AND unitPriceRupiah IS NOT NULL
        ORDER BY dateEpochDay, createdAtEpochMillis
        """,
    )
    fun observeMaterialPurchases(businessId: String): Flow<List<MaterialPurchaseRow>>

    @Query(
        """
        SELECT unitPriceRupiah FROM financial_events
        WHERE stockItemId = :stockItemId AND unitPriceRupiah IS NOT NULL
        ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC LIMIT 1
        """,
    )
    suspend fun latestPurchasePrice(stockItemId: String): Long?
```

`CostRow`: tambahkan `val unit: String?` setelah `quantityText`, lalu tambahkan `e.unit` ke SELECT `observeCostRows` di posisi yang sama.

- [ ] **Step 5: ProductRecord membaca penjualan**

```kotlin
data class ProductRecord(
    val product: ProductEntity,
    val units: List<ProductUnitEntity>,
    val recipe: List<ProductRecipeLineEntity>,
    val movements: List<ProductMovementEntity> = emptyList(),
    val sales: List<ProductSaleRow> = emptyList(),
) {
    // spec dan mainUnit seperti Task 2.

    /**
     * Base units in one [unitName]. Null (sales from before sale units, or from a template without
     * the column) means the main unit; a name that no longer exists, e.g. a unit renamed since, also
     * falls back to the main unit rather than dropping the sale from the stock.
     */
    fun contentOf(unitName: String?): BigDecimal {
        val main = spec.saleUnits.firstOrNull()?.content ?: BigDecimal.ONE
        val name = unitName?.trim()?.ifBlank { null } ?: return main
        if (name.equals(product.unit, ignoreCase = true)) return BigDecimal.ONE
        return spec.saleUnits.firstOrNull { it.name.equals(name, ignoreCase = true) }?.content ?: main
    }

    val stock: BigDecimal
        get() {
            val stored = movements.map { Triple(it.dateEpochDay, it.createdAtEpochMillis, ProductMovement(ProductMovementType.valueOf(it.movementType), TransactionCalculation.parseQuantity(it.quantityText) ?: BigDecimal.ZERO)) }
            val sold = sales.mapNotNull { s ->
                TransactionCalculation.parseQuantity(s.quantityText)?.let { q -> Triple(s.dateEpochDay, s.createdAtEpochMillis, ProductMovement(ProductMovementType.SALE, q * contentOf(s.unit))) }
            }
            return ProductStock.level((stored + sold).sortedWith(compareBy({ it.first }, { it.second })).map { it.third })
        }
}
```

Tambahkan `observeProductSales(projectId, FinancialEventType.CASH_INCOME.name, BookMode.REALIZATION.name)` sebagai flow
kelima di `combine` `FinanceRepository.products` (pakai overload `combine` dengan lima flow), lalu kelompokkan per `productId`.

- [ ] **Step 6: Harga bahan turunan**

```kotlin
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
```

Pertahankan bagian lain dari isi `stockItems` lama (lihat `FinanceRepository.kt:570`), bila ada. Di KDoc
`StockItem.basePriceRupiah`, ganti komentarnya menjadi `// Current price: newest purchase, else the price entered with the item (exact integer rupiah)`.
Pemanggil Task 2 yang memakai `stockDao.getAllStockItems(...).associate { it.id to it.basePriceRupiah }` untuk rencana
dan ekspor harus diganti ke harga turunan. Buat helper privat
`suspend fun materialPrices(businessId: String): Map<String, Long>` yang menggabungkan `getAllStockItems` dengan
`latestPurchasePrice`, lalu pakai helper itu.

- [ ] **Step 7: actualMonth dalam satuan utama**

`PlanAssembly.actualMonth(month, rows, products: List<ProductRecord>)`:

```kotlin
        val byId = products.associateBy { it.product.id }
        // ...
            unitsSold = named.groupBy { it.productId!! }.mapValues { (productId, list) ->
                val record = byId[productId]
                val main = record?.contentOf(null) ?: BigDecimal.ONE
                list.fold(BigDecimal.ZERO) { sum, r ->
                    val content = record?.contentOf(r.unit) ?: BigDecimal.ONE
                    sum + TransactionCalculation.parseQuantity(r.quantityText)!! * content.divide(main, MathContext.DECIMAL64)
                }
            },
```

Ubah semua pemanggil (`grep -rn "actualMonth(" app/src`). Di `PlanAssemblyTest`, tambahkan tes: produk dengan satuan
utama dus (isi 40) dan bungkus (isi 1); penjualan 2 dus + 40 bungkus memberi `unitsSold` = 3 dus.

```kotlin
    @Test fun `units sold are counted in the main sale unit`() {
        val record = ProductRecord(
            ProductEntity("k", "p", "Keripik", "bungkus", batchYieldText = "400", createdAtEpochMillis = 0),
            listOf(ProductUnitEntity("dus", "k", "dus", "40", 300_000, 0), ProductUnitEntity("bks", "k", "bungkus", "1", 8_000, 1)),
            emptyList(),
        )
        val day = LocalDate.of(2026, 10, 5).toEpochDay()
        val rows = listOf(
            CostRow("1", FinancialEventType.CASH_INCOME.name, day, 600_000, null, "k", "2", "dus", null),
            CostRow("2", FinancialEventType.CASH_INCOME.name, day, 320_000, null, "k", "40", "bungkus", null),
        )
        val actual = PlanAssembly.actualMonth(YearMonth.of(2026, 10), rows, listOf(record))
        assertEquals(0, BigDecimal(3).compareTo(actual.unitsSold.getValue("k")))
    }
```

- [ ] **Step 8: Jalankan tes**

Run: `./gradlew.bat :app:testDebugUnitTest`, lalu `connectedDebugAndroidTest` untuk `ProductStockFlowTest`, lalu seluruh connected suite.
Expected: semua PASS, kecuali kegagalan lama di ModuleScreenshotTest.

- [ ] **Step 9: Commit**

Entri ACTIVITY_LOG Task 3. Tambahkan juga di CHANGELOG `[Unreleased]` → Added: "Stok produk otomatis: Produksi
menambah, penjualan mengurangi (spec #9)" dan "Harga bahan mengikuti belanja terakhir (spec #20)".

```bash
git add -A app/src logs/
git commit -m "feat(data): product stock from sales, material price from latest purchase (spec #9 #20)"
```

---

### Task 4: Template Excel v5 untuk lembar produk

**Files:**
- Modify: `app/src/main/java/com/mankeu/importer/ImportModels.kt`
- Modify: `app/src/main/java/com/mankeu/importer/FinanceWorkbookImporter.kt`
- Modify: `app/src/main/java/com/mankeu/exporter/FinanceWorkbookExporter.kt`
- Modify: `app/src/main/java/com/mankeu/data/FinanceRepository.kt` (`reviewImport`, `commitImport`, `importProductPlan`, `exportTemplate`)
- Test: `app/src/test/java/com/mankeu/importer/TemplateImportTest.kt`, `app/src/androidTest/java/com/mankeu/data/ImportCommitTest.kt`

**Interfaces:**
- Consumes: `ProductRecord`, `ProductUnitEntity`, `ProductRecipeLineEntity`, `EventDraft/MoneyEvent.stockItemId` (Task 2–3).
- Produces:
  - `MankeuTemplate.VERSION = 5`, `SALE_UNITS = "Satuan Jual"`
  - `PRODUCT_HEADERS = listOf("Nama", "Satuan Dasar", "Hasil per Batch", "Target per Bulan", "Mulai Dijual (bulan ke-)")`
  - `SALE_UNIT_HEADERS = listOf("Produk", "Satuan", "Isi (satuan dasar)", "Harga Jual (Rp)")`
  - `RECIPE_HEADERS = listOf("Produk", "Komponen", "Bahan", "Jumlah per Batch", "Biaya per Batch (Rp)")`
  - `TRANSACTION_HEADERS` = header lama + `"Satuan"`, `"Bahan"`
  - `data class ImportSaleUnit(val name: String, val contentText: String, val priceRupiah: Long)`
  - `data class ImportRecipeLine(val source: String, val name: String, val materialName: String?, val quantityText: String?, val amountRupiah: Long?)`
  - `ImportProduct(source, name, baseUnit, batchYieldText, targetPerMonthText, sellFromMonth, units: List<ImportSaleUnit>, recipe: List<ImportRecipeLine>)`
  - `ImportTransaction` + `unit: String? = null`, `materialName: String? = null` (parameter terakhir)
  - `ImportComponent` dihapus. `LegacyProductShape.components` tidak lagi dipakai ekspor.

- [ ] **Step 1: Tulis tes round-trip yang gagal**

Ubah fixture `bundle()` di `TemplateImportTest.kt` (sekitar baris 269–325) supaya memuat bentuk baru. Di Task 2,
fixture ini sudah diubah ke `ProductRecord` dengan bentuk lama.

```kotlin
        products = listOf(
            ProductRecord(
                ProductEntity(id = "kopi", projectId = "p", name = "Kopi Susu", unit = "cup", batchYieldText = "6", targetPerMonthText = "700", createdAtEpochMillis = 0),
                listOf(
                    ProductUnitEntity(id = "u1", productId = "kopi", name = "cup", contentText = "1", priceRupiah = 10_000, sortOrder = 0),
                    ProductUnitEntity(id = "u2", productId = "kopi", name = "pak", contentText = "10", priceRupiah = 95_000, sortOrder = 1),
                ),
                listOf(
                    ProductRecipeLineEntity(id = "k1", productId = "kopi", name = "Susu", amountRupiah = 18_000, sortOrder = 0),
                    ProductRecipeLineEntity(id = "k2", productId = "kopi", name = "Gula", stockItemId = "s", quantityText = "0.5", sortOrder = 1),
                ),
            ),
        ),
```

Dua event di buku realisasi berubah: `e2` menjadi belanja Gula, dan `e3` menjadi penjualan per pak.

```kotlin
                    event("e2", FinancialEventType.CASH_EXPENSE, 1_150_000, "Beli kopi", "Bahan Baku & Stok", createdAt = 2)
                        .copy(costNature = CostNature.FIXED.name, stockItemId = "s", quantityText = "10", unit = "kg", unitPriceRupiah = 115_000),
                    event("e3", FinancialEventType.CASH_INCOME, 4_000_000, "Jual kopi", "Penjualan & Jasa", createdAt = 3)
                        .copy(productId = "kopi", quantityText = "40", unit = "pak"),
```

Ekspektasi di `an export imports back without loss` berubah untuk baris A3/A4 dan produk:

```kotlin
                ImportTransaction("Transaksi!A3", DAY, "Beli kopi", "Bahan Baku & Stok", incoming = false, amountRupiah = 1_150_000, costNature = CostNature.FIXED, quantityText = "10", unit = "kg", materialName = "Gula"),
                ImportTransaction("Transaksi!A4", DAY, "Jual kopi", "Penjualan & Jasa", incoming = true, amountRupiah = 4_000_000, productName = "Kopi Susu", quantityText = "40", unit = "pak"),
```

```kotlin
        assertEquals(
            listOf(
                ImportProduct(
                    source = "Produk!A2", name = "Kopi Susu", baseUnit = "cup", batchYieldText = "6", targetPerMonthText = "700", sellFromMonth = 1,
                    units = listOf(ImportSaleUnit("cup", "1", 10_000), ImportSaleUnit("pak", "10", 95_000)),
                    recipe = listOf(
                        ImportRecipeLine("Resep Produk!A2", "Susu", materialName = null, quantityText = null, amountRupiah = 18_000),
                        ImportRecipeLine("Resep Produk!A3", "Gula", materialName = "Gula", quantityText = "0.5", amountRupiah = null),
                    ),
                ),
            ),
            preview.products,
        )
```

Kolom `Jumlah` Transaksi untuk belanja sekarang ikut ditulis bila ada bahan. Bila exporter lama hanya menulis jumlah
untuk baris produk (`quantity(event.quantityText.takeIf { event.productId != null })`), ubah kondisinya menjadi
`event.productId != null || event.stockItemId != null`.

Tambahkan tes penolakan v4:

```kotlin
    @Test
    fun `a version 4 file is rejected with the exact differences`() {
        val v4 = XlsxWorkbookWriter().write(
            listOf(
                WorkbookTable(MankeuTemplate.GUIDE, MankeuTemplate.GUIDE_HEADERS, listOf(listOf("Format", MankeuTemplate.MARKER))),
                WorkbookTable(MankeuTemplate.TRANSACTIONS, MankeuTemplate.TRANSACTION_HEADERS.dropLast(2), emptyList()),
                WorkbookTable(MankeuTemplate.BUDGET, MankeuTemplate.BUDGET_HEADERS, emptyList()),
                WorkbookTable(MankeuTemplate.PRODUCTS, listOf("Nama", "Satuan", "Harga Jual (Rp)", "Target per Bulan"), emptyList()),
                WorkbookTable(MankeuTemplate.RECIPES, listOf("Produk", "Komponen", "Biaya (Rp)", "Untuk Berapa Unit"), emptyList()),
                WorkbookTable(MankeuTemplate.STOCK, MankeuTemplate.STOCK_HEADERS, emptyList()),
            ),
        )

        val errors = importer.preview("lama.xlsx", v4).errors

        assertTrue(errors.joinToString(), "Lembar \"Satuan Jual\" tidak ada." in errors)
        assertTrue(errors.joinToString(), "Lembar \"Produk\" kolom B1 harus \"Satuan Dasar\", tertulis \"Satuan\"." in errors)
    }
```

Fixture `template(...)` mendapat lembar `SALE_UNITS` (parameter `saleUnits: List<List<Any?>> = emptyList()`) setelah
Produk. Tes yang menyusun baris produk atau resep sendiri berpindah ke kolom v5:
- `a recipe line for a product not in the file is rejected` memakai baris resep lima kolom.
- `a bad selling month...` memakai baris produk `listOf("Cabai", "kg", "1", "300", "0")` plus baris Satuan Jual untuk tiap produk, dan bagian "renamed selling month column" dihapus.

Tes `a version 3 file without the selling month column still imports` dihapus, karena v3/v4 sekarang ditolak (DEC-028 §9).
Pesan error untuk sel lain tetap sama.

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.mankeu.importer.TemplateImportTest"`
Expected: FAIL (kompilasi: `ImportSaleUnit` belum ada).

- [ ] **Step 3: Model dan header**

Di `ImportModels.kt`, ganti konstanta dan header sesuai bagian Interfaces di atas. `REQUIRED` mendapat
`SALE_UNITS to SALE_UNIT_HEADERS` setelah `PRODUCTS`. Hapus `OPTIONAL` beserta pemakaiannya di importer
(`grep -rn "OPTIONAL" app/src`), karena v5 menulis semua kolom. Ganti `ImportComponent`/`ImportProduct` dengan tipe
baru, lalu tambahkan `unit` dan `materialName` ke `ImportTransaction`.

- [ ] **Step 4: Importer**

`readProducts(products, saleUnits, recipes, errors)`:
- Satuan Jual: kolom 0 Produk (wajib), 1 Satuan (wajib), 2 Isi (`quantity`, wajib > 0), 3 Harga (`money`, wajib).
- Resep: kolom 0 Produk, 1 Komponen (wajib), 2 Bahan (opsional), 3 Jumlah per Batch, 4 Biaya per Batch.
  - Bila Bahan terisi, Jumlah wajib dan Biaya harus kosong ("`ref`: baris berbahan diisi Jumlah, Biaya dibiarkan kosong").
  - Bila Bahan kosong, Biaya wajib.
- Produk: kolom 0 Nama, 1 Satuan Dasar, 2 Hasil per Batch (wajib > 0), 3 Target (opsional > 0), 4 Mulai Dijual (opsional, bilangan bulat ≥ 1, default 1).
- Produk tanpa satu pun baris Satuan Jual → error "`ref`: produk \"X\" belum punya baris di lembar \"Satuan Jual\"."
- Baris Satuan Jual / Resep yang menyebut produk tak dikenal → error seperti pesan lama.

Transaksi: kolom 9 Satuan dan 10 Bahan → `row.text(9)`, `row.text(10)`.

- [ ] **Step 5: Review dan commit impor**

`reviewImport` menambah konflik, dengan nama bahan dibandingkan tanpa beda huruf besar/kecil terhadap Stok Barang di
file dan bahan yang sudah ada di usaha (`stockDao.getStockItems(businessId, projectId)`):
- Bahan di resep atau transaksi yang tidak ada di kedua sumber → `"${source}: bahan \"$name\" tidak ada di lembar Stok Barang maupun di usaha ini."`
- Satuan di transaksi produk yang bukan satuan jual produk itu → `"${source}: \"$unit\" bukan satuan jual $product."`

`commitImport`:
- Pindahkan penyisipan `addedStock` **sebelum** produk.
- Buat `materialIds` = bahan yang sudah ada (berdasarkan nama ternormalisasi) + yang baru ditambahkan.
- Produk: `ProductEntity(... unit = baseUnit, batchYieldText = ...)`, `insertProductUnits(...)`, `insertRecipeLines(...)` dengan
  `stockItemId = materialIds[normalize(materialName)]`. Nama baris berbahan = nama bahan.
- Transaksi: `unit = row.unit`, `stockItemId = row.materialName?.let { materialIds.getValue(normalize(it)) }`, lalu
  `.withPurchasePrice()` (Task 3).
- Hapus pemakaian `LegacyProductShape.saleUnit/recipeLine` dari impor. Keduanya tetap dipakai migrasi dan backup.

Di undo impor tidak ada yang perlu ditambah: produk terhapus menurut `importBatchId` (cascade ke satuan, resep, dan
gerakan), dan stok/harga dibaca dari transaksi yang ikut terhapus.

- [ ] **Step 6: Exporter**

- Lembar Produk: `listOf(p.product.name, p.product.unit, quantity(p.product.batchYieldText), quantity(p.product.targetPerMonthText), p.product.sellFromMonth)`.
- Lembar Satuan Jual: `p.units.sortedBy { it.sortOrder }.map { listOf(p.product.name, it.name, quantity(it.contentText), it.priceRupiah) }`.
- Lembar Resep: `p.recipe.sortedBy { it.sortOrder }.map { listOf(p.product.name, it.name, it.stockItemId?.let(stockNames::get), quantity(it.quantityText), it.amountRupiah) }`.
- Transaksi: tambahkan `event.unit.takeIf { event.productId != null || event.stockItemId != null }` dan `event.stockItemId?.let(stockNames::get)`.

`stockNames` = `bundle.stockItems.associate { it.id to it.name }`. Pastikan `buildProjectExportBundle` memasukkan semua
bahan yang dirujuk resep dan transaksi (bahan unit + bahan bersama). Bila belum, ganti sumbernya ke
`stockDao.getStockItems(businessId, projectId)`. Hapus `materialPrices` dari bundle bila tidak dipakai lagi.
`exportTemplate` (template kosong) menulis lembar Satuan Jual kosong.

- [ ] **Step 7: Tes commit impor (androidTest)**

Di `ImportCommitTest.kt`, tambahkan kasus: impor file keripik (dibangun dari ekspor bundle Step 1), lalu periksa:
- produk punya 2 satuan;
- baris resep "Minyak" mengarah ke `stock_items` "Minyak" yang baru dibuat;
- belanja punya `stockItemId` yang sama;
- harga minyak dari `repo.materialPrice` sama dengan 27.500.

Lalu batalkan impor dan pastikan produk dan bahannya hilang.

- [ ] **Step 8: Jalankan tes, commit**

Run: unit test penuh, lalu connected `ImportCommitTest`. Expected: PASS.
Perbarui `docs/` yang memuat header template (`grep -rln "Untuk Berapa Unit" docs/`): baris ini berbeda dan arsip
`docs/archive/` tidak disentuh. Entri ACTIVITY_LOG, dan CHANGELOG → Changed: "Template Excel v5: lembar Produk, Satuan
Jual, Resep per batch; kolom Satuan dan Bahan di Transaksi. File template lama ditolak dengan daftar kolom yang berbeda."

```bash
git add -A app/src docs logs/
git commit -m "feat(import): template v5 product sheets - sale units, batch recipe with materials (DEC-012, DEC-028)"
```

---

### Task 5: Tab Produk, editor halaman penuh, Produksi, hitung stok

Model: standar atau paling mampu (UI multi-berkas).

**Files:**
- Create: `app/src/main/java/com/mankeu/presentation/screens/ProductsScreen.kt`
- Create: `app/src/main/java/com/mankeu/presentation/screens/ProductEditorScreen.kt`
- Modify: `app/src/main/java/com/mankeu/presentation/screens/StockSection.kt` (isi daftar bahan menjadi `LazyListScope.materialItems`)
- Modify: `app/src/main/java/com/mankeu/presentation/MankeuApp.kt` (`Destination.STOCK` → `PRODUCTS`, `Overlay.PRODUCT`)
- Modify: `app/src/main/java/com/mankeu/presentation/AppViewModel.kt` (`saveProduct(id, draft)`)
- Modify: `app/src/main/java/com/mankeu/presentation/screens/PlanScreen.kt` (hapus `ProductSheet`, buka editor)
- Modify: `app/src/main/java/com/mankeu/data/LegacyProductShape.kt` (hapus `draft`, `components`, `ComponentDraft` bila tak terpakai lagi)
- Modify: `app/src/main/res/values/strings.xml`, `values-en/strings.xml`
- Test: `app/src/androidTest/java/com/mankeu/presentation/ProductEditorTest.kt`

**Interfaces:**
- Consumes: `ProductRecord`, `ProductCosting.cost`, `ProductStock.split`, `ProductDraft`, `SaleUnitDraft`, `RecipeLineDraft`,
  `AppViewModel.recordProduction`, `countProductStock`, `StockItem`.
- Produces:
  - `AppViewModel.saveProduct(id: String?, draft: ProductDraft)` (menggantikan tanda tangan lama)
  - `@Composable fun ProductsScreen(products: List<ProductRecord>, materials: List<StockItem>, hasBusiness: Boolean, unitName: String?, onManageScope: () -> Unit, onOpenProduct: (String?) -> Unit, onProduce: (String, BigDecimal, LocalDate) -> Unit, onCountStock: (String, BigDecimal, LocalDate) -> Unit, onCreateStockItem: ..., onRecordStockMovement: ..., expanded: Boolean)`
  - `@Composable fun ProductEditorScreen(existing: ProductRecord?, materials: List<StockItem>, onSave: (ProductDraft) -> Unit, onDelete: (() -> Unit)?, modifier: Modifier = Modifier)`
  - `Overlay.PRODUCT` + `var productEditorId: String?` (null = produk baru) di `MankeuApp`

- [ ] **Step 1: String baru**

Tambahkan ke `values/strings.xml` (dan `values-en/strings.xml` dengan terjemahan di kolom kanan):

| id | id | en |
|---|---|---|
| `nav_products` | Produk | Products |
| `products_add` | Tambah produk | Add product |
| `products_empty_title` | Belum ada produk | No products yet |
| `products_empty_body` | Isi resep per batch sekali, Mankeu menghitung HPP per bungkus, per dus, dan satuan lain. | Enter one batch recipe and Mankeu works out the cost per pack, per box and any other unit. |
| `product_hpp_line` | HPP %1$s/%2$s · untung %3$s | Cost %1$s/%2$s · profit %3$s |
| `product_hpp_incomplete` | HPP belum lengkap: harga %1$s belum ada | Cost incomplete: no price for %1$s |
| `product_no_recipe` | Resep belum diisi | No recipe yet |
| `product_stock_line` | Stok %1$s %2$s | Stock %1$s %2$s |
| `product_stock_split` | (%1$s %2$s + %3$s %4$s) | (%1$s %2$s + %3$s %4$s) |
| `product_stock_negative_hint` | Stok minus: catat produksi atau hitung stok. | Stock below zero: record production or count stock. |
| `product_produce` | Produksi | Produce |
| `product_count_stock` | Hitung stok | Count stock |
| `production_title` | Produksi %1$s | Produce %1$s |
| `production_batches` | Berapa batch? | How many batches? |
| `production_result` | = %1$s %2$s masuk stok | = %1$s %2$s into stock |
| `count_stock_title` | Hitung stok %1$s | Count stock of %1$s |
| `count_stock_field` | Jumlah sekarang (%1$s) | Quantity now (%1$s) |
| `editor_title_new` | Produk baru | New product |
| `editor_title_edit` | Ubah produk | Edit product |
| `editor_name` | Nama produk | Product name |
| `editor_base_unit` | Satuan terkecil | Smallest unit |
| `editor_base_unit_hint` | Satuan untuk stok dan HPP, mis. bungkus | Unit for stock and cost, e.g. pack |
| `editor_batch_yield` | Satu batch menghasilkan berapa %1$s? | How many %1$s does one batch make? |
| `editor_recipe` | Resep 1 batch | Recipe for 1 batch |
| `editor_add_material` | Tambah bahan | Add material |
| `editor_add_cost` | Tambah biaya lain | Add other cost |
| `editor_material` | Bahan | Material |
| `editor_quantity_per_batch` | Jumlah (%1$s) | Quantity (%1$s) |
| `editor_cost_name` | Biaya (mis. gas, upah) | Cost (e.g. gas, wages) |
| `editor_cost_amount` | Biaya per batch | Cost per batch |
| `editor_batch_total` | Biaya 1 batch %1$s · HPP %2$s per %3$s | Cost per batch %1$s · %2$s per %3$s |
| `editor_units` | Dijual per | Sold per |
| `editor_unit_name` | Satuan | Unit |
| `editor_unit_content` | Isi (%1$s) | Contains (%1$s) |
| `editor_unit_price` | Harga jual | Selling price |
| `editor_unit_add` | Tambah satuan jual | Add selling unit |
| `editor_unit_result` | HPP %1$s · untung %2$s | Cost %1$s · profit %2$s |
| `editor_plan_section` | Untuk rencana usaha (boleh kosong) | For the business plan (optional) |
| `editor_target` | Target terjual per bulan (%1$s) | Sales target per month (%1$s) |
| `editor_remove_line` | Hapus baris | Remove line |
| `editor_delete` | Hapus produk | Delete product |

Ganti label `Destination` untuk tab ini ke `nav_products`; `nav_stock` dihapus bila tak terpakai lagi.

- [ ] **Step 2: Tulis tes UI yang gagal**

`ProductEditorTest.kt` memakai `createComposeRule()`, `setContent { MankeuTheme { ProductEditorScreen(existing = null, materials = listOf(StockItem("minyak", "b", "Minyak", "liter", 25_000, BigDecimal.ZERO, BigDecimal.ZERO)), onSave = { saved = it }, onDelete = null) } }`
(nama tema: lihat `Theme.kt`). Skenarionya:
- isi Nama "Keripik", Satuan terkecil "bungkus", batch "400";
- tambah bahan Minyak 20 dan biaya "Lain-lain" 1.900.000;
- satuan jual baris 1 "bungkus" isi 1 harga 8.000, lalu tambah "dus" isi 40 harga 300.000.

Ekspektasinya:
- `onNodeWithText("Biaya 1 batch Rp2.400.000", substring = true).assertExists()`;
- `onNodeWithText("HPP Rp240.000", substring = true).assertExists()`;
- setelah klik Simpan, `saved!!.units.map { it.contentText } == listOf("1", "40")` dan `saved!!.recipe.first().stockItemId == "minyak"`.

Format rupiah persis mengikuti `toRupiah()` (`Formatting.kt`); sesuaikan teks yang dicari dengannya. Sentuh field lewat
`onNodeWithText(label)` dan `performTextInput`.

Run: `./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.mankeu.presentation.ProductEditorTest"`
Expected: FAIL (kompilasi).

- [ ] **Step 3: Editor halaman penuh**

`ProductEditorScreen.kt`: `Column(Modifier.fillMaxSize())` berisi `LazyColumn(Modifier.weight(1f))` dan tombol Simpan
tetap di bawah (`Button(Modifier.fillMaxWidth().padding(16.dp).imePadding())`), sesuai #13. Isi daftar, berurutan:

1. `OutlinedTextField` Nama; Satuan terkecil (supporting `editor_base_unit_hint`); batch (`KeyboardType.Decimal`, `sanitizeDecimal`, label `editor_batch_yield` dengan satuan dasar, atau "satuan" bila kosong).
2. Judul `editor_recipe`, lalu per baris resep:
   - baris bahan: `ChoiceMenu(fieldLabel = editor_material, label = nama bahan, options = materials.map { it.id to it.name }, onSelected)` + field jumlah (`editor_quantity_per_batch` dengan satuan bahan) + teks biaya baris (`ProductCosting.lineCost(...)?.toRupiah()` atau "harga belum ada") + `IconButton(Icons.Default.Close, contentDescription = editor_remove_line)`;
   - baris biaya: field nama (`editor_cost_name`) + `MoneyField(editor_cost_amount)` + tombol hapus.
   - Tombol `TextButton` `editor_add_material` (hanya bila `materials` tidak kosong) dan `editor_add_cost`.
3. Ringkasan: `editor_batch_total` dengan `cost.batchCostRupiah`, `cost.hppPerBaseUnitRupiah`, satuan dasar. Bila
   `!cost.complete && cost.hasRecipe`, tampilkan `product_hpp_incomplete` dengan nama baris yang belum berharga.
4. Judul `editor_units`, lalu per baris satuan jual: Satuan, Isi (`editor_unit_content` dengan satuan dasar), `MoneyField` harga,
   dan di bawahnya `editor_unit_result` (HPP dan untung per satuan itu, dari `cost.perSaleUnit[i]`). Hapus baris bila lebih dari satu.
   Tombol `editor_unit_add`. Baris pertama awalnya: nama = satuan dasar (ikut terisi selama pengguna belum mengubahnya), isi "1".
5. Bagian `editor_plan_section` (bisa dilipat, tertutup secara default bila target kosong): target (`editor_target` dengan satuan jual pertama) dan "Mulai dijual bulan ke-" (pakai field lama dari `ProductSheet` PlanScreen).
6. Bila `onDelete != null`: `TextButton` merah `editor_delete` dengan konfirmasi `AlertDialog` (pakai string konfirmasi hapus
   yang sudah ada di `ProductSheet` lama).

State: daftar baris disimpan sebagai `mutableStateListOf` dari data class UI lokal yang `@Parcelize`/`Saver`. Pola
`rememberSaveable` lain di app memakai string; pilih cara yang bertahan saat rotasi. Pratinjau HPP dihitung ulang setiap
recomposition:
`ProductCosting.cost(ProductSpec(...dari state...), materials.associate { it.id to it.basePriceRupiah })`.
Angka yang tidak valid diperlakukan sebagai baris tanpa biaya, tanpa crash.

Simpan: tombol aktif bila nama, satuan dasar, batch > 0, dan minimal satu satuan berharga sudah terisi. Lalu panggil
`onSave(ProductDraft(...))`. Validasi akhir tetap di repository (Task 2); pesan error datang lewat snackbar seperti form lain.

- [ ] **Step 4: Tab Produk**

`StockSection.kt`: pindahkan isi `LazyColumn` di `StockScreen` (tanpa cek `hasBusiness`) ke
`internal fun LazyListScope.materialItems(items, unitName, onCreateStockItem, onRecordStockMovement)`. Sheet tambah dan
gerakan barang tetap di sana, dengan state yang diangkat ke pemanggil bila perlu. Hapus `StockScreen` setelah
`ProductsScreen` memakainya.

`ProductsScreen.kt`: satu `LazyColumn`. Bila belum ada usaha, tampilkan `EmptyCard` yang sama dengan `StockScreen` lama.
Selain itu, susunannya:
- tombol `products_add` → `onOpenProduct(null)`;
- bila daftar kosong, `EmptyCard(products_empty_title, products_empty_body, products_add, ...)`;
- per produk, `Card` yang bisa diklik (→ `onOpenProduct(id)`) berisi:
  - nama (bold);
  - satu baris `product_hpp_line` per satuan jual (`UnitCost`), atau `product_no_recipe` / `product_hpp_incomplete`;
  - baris stok: `product_stock_line` (stok dalam satuan dasar). Bila ada satuan jual dengan isi > 1, tambahkan
    `product_stock_split` untuk satuan berisi terbesar (`ProductStock.split`). Bila stok < 0, tampilkan
    `product_stock_negative_hint` berwarna `semantic.danger`;
  - `Row` dua `FilledTonalButton`: `product_produce` dan `product_count_stock`;
- judul bagian "Bahan & barang" (pakai `stock_*` string judul yang sudah ada), lalu `materialItems(...)`.

Produksi dan hitung stok = `MankeuSheet` pendek (#13 membolehkan untuk form pendek):
- **Produksi:** field batch (default "1", desimal), `production_result` hidup (`batch × hasil`), dan `DateField` (default hari ini).
  Simpan memanggil `onProduce(id, batches, date)`.
- **Hitung stok:** field jumlah (desimal, 0 boleh; isi awal stok sekarang bila ≥ 0) dan `DateField`.
  Simpan memanggil `onCountStock(id, counted, date)`.

Angka kuantitas ditampilkan dengan `toQuantityText()` yang sudah ada.

- [ ] **Step 5: Sambungkan di MankeuApp, ViewModel, PlanScreen**

- `Destination.STOCK` → `PRODUCTS(R.string.nav_products, Icons.Default.Inventory2)`; ganti semua rujukannya
  (`grep -rn "Destination.STOCK\|STOCK ->" app/src`, termasuk `guide/Guides.kt` dan `articlesFor`).
- `Overlay.PRODUCT`; `var productEditorId by rememberSaveable { mutableStateOf<String?>(null) }`. `onOpenProduct = { id -> productEditorId = id; overlay = Overlay.PRODUCT }`.
- Cabang `Overlay.PRODUCT -> OverlayScaffold(title = if (productEditorId == null) editor_title_new else editor_title_edit, onBack = { overlay = Overlay.NONE }, snackbarHost = ...) { ProductEditorScreen(existing = state.products.firstOrNull { it.product.id == productEditorId }, materials = state.stockItems, onSave = { viewModel.saveProduct(productEditorId, it); overlay = Overlay.NONE }, onDelete = productEditorId?.let { id -> { viewModel.archiveProduct(id); overlay = Overlay.NONE } }) }`.
  Bila penyimpanan gagal, overlay tidak boleh tertutup. Jadikan `saveProduct` di ViewModel menerima `onSuccess: () -> Unit`
  dan tutup overlay di callback itu, bukan langsung.
- ViewModel: `fun saveProduct(id: String?, draft: ProductDraft, onSuccess: () -> Unit = {})` memanggil `repository.saveProduct(projectId, id, draft)`.
  Hapus tanda tangan lama.
- PlanScreen: hapus `ProductSheet` dan `PlanEditor.Product`. `PlanSectionCard` produk: `onAdd = { onOpenProduct(null) }`,
  baris `onClick = { onOpenProduct(row.product.id) }`, subtitle `plan_product_row` dari `row.mainUnit` dan HPP satuan utama.
  Tambahkan parameter `onOpenProduct: (String?) -> Unit` dan teruskan dari MankeuApp.
- `LegacyProductShape`: hapus `draft`, `components`, dan `ComponentDraft` (`grep` dulu; tes `LegacyProductShapeTest` yang
  menguji `draft` ikut dihapus atau diubah ke `saleUnit`/`recipeLine`).

- [ ] **Step 6: Jalankan tes**

Run: unit test penuh, `lintDebug`, `assembleDebug`, connected `ProductEditorTest`, lalu seluruh connected suite. Expected: PASS
(ModuleScreenshotTest dan HelpScreenshotTest boleh gagal hanya karena gambar lama; catat di laporan, karena screenshot diganti di F7).

- [ ] **Step 7: Commit**

ACTIVITY_LOG Task 5; CHANGELOG → Added: "Tab Produk: HPP dan untung per satuan jual, stok produk, Produksi, hitung stok;
editor produk halaman penuh." Changed: "Tab Stok menjadi Produk; bahan ada di bagian Bahan & barang."

```bash
git add -A app/src logs/
git commit -m "feat(products): Produk tab with HPP per sale unit, stock, production; full-page product editor (spec #5 #6 #9 #13)"
```

---

### Task 6: Form catat: satuan jual, pilihan bahan, peringatan HPP

**Files:**
- Modify: `app/src/main/java/com/mankeu/presentation/screens/EntryScreen.kt` (sekitar 173, 208, 661–684, 860–890)
- Modify: `app/src/main/java/com/mankeu/presentation/AppViewModel.kt` (`recordEvent`/`updateEvent` + `stockItemId`, `priceAlert`)
- Modify: `app/src/main/java/com/mankeu/presentation/MankeuApp.kt` (dialog)
- Modify: `strings.xml` ×2
- Test: `app/src/androidTest/java/com/mankeu/presentation/ViewModelFlowsTest.kt`

**Interfaces:**
- Consumes: `ProductCosting.priceImpact`, `HppChange`, `FinanceRepository.materialPrice`, `EventDraft.stockItemId`, `ProductRecord.spec`.
- Produces:
  - `data class PriceAlert(val materialName: String, val unit: String, val oldPriceRupiah: Long, val newPriceRupiah: Long, val changes: List<HppChange>)`
  - `AppUiState.priceAlert: PriceAlert?`, `AppViewModel.dismissPriceAlert()`

- [ ] **Step 1: String baru**

| id | id | en |
|---|---|---|
| `entry_sale_unit` | Satuan | Unit |
| `entry_material` | Bahan (opsional) | Material (optional) |
| `entry_material_none` | Bukan bahan resep | Not a recipe material |
| `entry_material_hint` | Harga satuannya menjadi harga bahan untuk HPP. | Its unit price becomes the material price for cost. |
| `price_alert_title` | Harga %1$s berubah | %1$s price changed |
| `price_alert_body` | %1$s → %2$s per %3$s. Dampaknya: | %1$s → %2$s per %3$s. Effect: |
| `price_alert_row` | %1$s: HPP %2$s → %3$s per %4$s, untung %5$s → %6$s | %1$s: cost %2$s → %3$s per %4$s, profit %5$s → %6$s |
| `price_alert_open_products` | Lihat produk | View products |

- [ ] **Step 2: Tulis tes alur ViewModel yang gagal**

Di `ViewModelFlowsTest.kt`, ikuti setup tes lain di berkas itu. Skenarionya:
- buat usaha;
- buat bahan Minyak (Rp25.000/liter) dan produk keripik (resep dan satuan seperti golden Task 1);
- `viewModel.recordEvent(...)` belanja 20 liter Rp550.000 dengan `stockItemId = minyak`;
- tunggu `state.priceAlert != null`.

Ekspektasi:
- `oldPriceRupiah == 25_000`, `newPriceRupiah == 27_500`;
- `changes.single().after.perSaleUnit.map { it.hppRupiah } == listOf(6_125L, 245_000L)`.

Belanja kedua dengan harga yang sama tidak memunculkan alert.

Run connected `ViewModelFlowsTest`. Expected: FAIL (kompilasi).

- [ ] **Step 3: ViewModel**

Telusuri bagaimana `productId` diteruskan (`AppViewModel.kt:575-611`, `EntryScreen.kt:872`), lalu teruskan
`stockItemId: String? = null` dengan cara yang sama ke `EventDraft`. Di jalur simpan (record dan update):

```kotlin
        val material = stockItemId?.let { id -> stockItems.value.firstOrNull { it.id == id } }
        val oldPrices = stockItems.value.associate { it.id to it.basePriceRupiah }
        runCatching { /* repository.recordEvent(draft) atau updateEvent */ }
            .onSuccess {
                material?.let { m ->
                    val newPrice = repository.materialPrice(m.id) ?: return@let
                    val changes = ProductCosting.priceImpact(products.value.map { it.spec }, oldPrices, m.id, newPrice)
                    if (newPrice != m.basePriceRupiah && changes.isNotEmpty()) {
                        priceAlert.value = PriceAlert(m.name, m.unit, m.basePriceRupiah, newPrice, changes)
                    }
                }
                // pesan sukses yang sudah ada
            }
```

`priceAlert` = `MutableStateFlow<PriceAlert?>(null)` yang ikut ke `AppUiState`. `fun dismissPriceAlert() { priceAlert.value = null }`.
Bila `products`/`stockItems` di ViewModel bernama lain, pakai nama yang ada.

- [ ] **Step 4: EntryScreen**

- State `var stockItemId by rememberSaveable { mutableStateOf<String?>(null) }`, diisi dari `event.stockItemId` saat mengedit (di samping `productId = event.productId`, baris 208).
- Penjualan berproduk (blok 661–684): saat produk dipilih, `unit = record.mainUnit?.name ?: record.product.unit` dan
  `unitPrice = mainUnit.priceRupiah`. Bila produk punya lebih dari satu satuan jual, tampilkan `ChoiceMenu(fieldLabel = entry_sale_unit, label = unit, options = record.units.sortedBy { it.sortOrder }.map { it.name to it.name })`.
  Memilih satuan mengisi `unit` dan `unitPrice`, lalu menghitung ulang `amount` dari jumlah seperti kode yang sudah ada.
  `state.products` sekarang berisi `ProductRecord`.
- Belanja (keluar, peran EXPENSE, bukan ASSET) dengan `state.stockItems` tidak kosong: `ChoiceMenu(fieldLabel = entry_material, options = listOf("" to entry_material_none) + stockItems.map { it.id to it.name })`.
  Memilih bahan mengisi `unit = material.unit`, `unitPrice = material.basePriceRupiah` (bila > 0), `advanced = true`,
  dengan supporting text `entry_material_hint`. Saat bahan terpilih, unit tidak boleh diubah (field unit `readOnly`).
- Teruskan `stockItemId.takeIf { !incoming && selectedRole == CategoryRole.EXPENSE }` ke simpan (pola sama dengan `productId` di baris 872).
  Reset `stockItemId = null` saat form dibersihkan (baris 884).

- [ ] **Step 5: Dialog**

Di `MankeuApp`, saat `state.priceAlert != null`, tampilkan `AlertDialog`:
- judul `price_alert_title`;
- isi `price_alert_body` (harga lama → baru, satuan bahan), diikuti satu `price_alert_row` per `HppChange` × satuan jual
  (`before.perSaleUnit[i]` vs `after.perSaleUnit[i]`);
- `confirmButton` `price_alert_open_products` → `dismissPriceAlert()` lalu `destination = Destination.PRODUCTS`;
- `dismissButton` "Oke" (pakai string OK yang sudah ada) → `dismissPriceAlert()`.

Dialog adalah konfirmasi pendek, jadi sesuai dengan #13.

- [ ] **Step 6: Jalankan tes, commit**

Run: unit, lint, assemble, connected `ViewModelFlowsTest`, lalu seluruh connected suite. Expected: PASS.
ACTIVITY_LOG Task 6; CHANGELOG → Added: "Catat penjualan per satuan jual (bungkus, dus); belanja bisa menautkan bahan dan
memperingatkan dampak harga baru ke HPP dan untung (spec #20)."

```bash
git add -A app/src logs/
git commit -m "feat(entry): sale unit choice, material on purchases, HPP impact warning (spec #5 #20)"
```

---

### Task 7: Uji alur pengusaha di emulator, dokumen, penutupan fase

**Files:**
- Create: `docs/audit/2026-10-29_UJI_FASE1.md` (ditulis per checkpoint)
- Modify: `docs/DOMAIN_MODEL.md`, `docs/PRODUCT_REQUIREMENTS.md` (bagian produk), `docs/superpowers/plans/2026-10-29-redesign-roadmap.md`, `docs/WORK_IN_PROGRESS.md`
- Modify: `logs/ACTIVITY_LOG.md`, `logs/DECISION_LOG.md` (status DEC-028 → Implemented, plus temuan), `logs/CHANGELOG.md`

- [ ] **Step 1: Pasang build debug di emulator**

Run: `./gradlew.bat :app:installDebug`; buka app (`com.mankeu`). Data emulator sudah terhapus oleh tes, sehingga ini instal bersih.

- [ ] **Step 2: Jalankan kriteria F1, catat per checkpoint**

Setelah setiap checkpoint, langsung tulis hasil, angka yang terlihat, dan screenshot (`docs/audit/img/f1-*.png`) ke
`docs/audit/2026-10-29_UJI_FASE1.md`, supaya hasil tetap tersimpan bila sesi terputus.

- **CP1:** buat usaha "Keripik Bu Sri" lalu buka tab Produk. Bagian Bahan & barang: tambah Singkong (kg, Rp30.000) dan Minyak (liter, Rp25.000).
- **CP2:** Tambah produk:
  - Keripik, satuan terkecil bungkus, 1 batch = 400.
  - Resep: Singkong 50, Minyak 20, Bumbu & kemasan Rp300.000, Gas Rp100.000.
  - Satuan jual: bungkus Rp8.000 dan dus (isi 40) Rp300.000.
  - **Harus terlihat tanpa hitung manual:** Biaya 1 batch Rp2.400.000, HPP Rp6.000/bungkus dan Rp240.000/dus, untung Rp2.000 dan Rp60.000.
- **CP3:** Produksi 1 batch → stok 400 bungkus (10 dus).
- **CP4:** Catat → uang masuk, produk Keripik, satuan dus, jumlah 2 → nominal Rp600.000 otomatis; stok menjadi 320 bungkus (8 dus).
- **CP5:** Catat → uang keluar, bahan Minyak, jumlah 20 liter, nominal Rp550.000 → dialog "Harga Minyak berubah", Rp25.000 → Rp27.500;
  isinya Keripik HPP Rp6.000 → Rp6.125 per bungkus dan Rp240.000 → Rp245.000 per dus. "Lihat produk" menampilkan angka baru.
- **CP6:** Hapus belanja itu → HPP kembali Rp6.000; hapus penjualan → stok 400.
- **CP7:** Rencana Usaha → bagian produk menampilkan Keripik dengan HPP satuan utama; ketuk → editor yang sama terbuka.
- **CP8:** Ekspor Excel → impor ke unit baru → produk, satuan, resep (bahan tertaut), dan transaksi kembali sama. Lalu impor
  template v4 lama → ditolak dengan daftar kolom yang berbeda.
- **CP9:** Sheet produksi dan editor: geser isi tidak menutup sheet, dan keyboard tidak membuat layar memantul (regresi F0).

Bila ada temuan, perbaiki dengan siklus TDD kecil di task ini (commit terpisah per perbaikan, masing-masing dengan log),
lalu ulangi checkpoint yang terkena.

- [ ] **Step 3: Dokumen**

- `DOMAIN_MODEL.md`: entitas Produk (satuan dasar, hasil per batch), Satuan Jual, Baris Resep (bahan × jumlah atau biaya
  bebas), Gerakan Produk (produksi, hitung), stok = turunan; harga bahan = turunan; aturan pembulatan HPP.
- Roadmap: baris F1 diberi tanda selesai dengan rujukan DEC-028. Baris F2 mendapat "+ snapshot HPP per baris nota (#6,
  dipindah dari F1, DEC-028 §5)". Kriteria F1 dicentang dengan rujukan ke laporan uji.
- `WORK_IN_PROGRESS.md`: Fase 1 selesai; berikutnya rencana Fase 2.
- DEC-028: status Implemented, plus hal yang belum terverifikasi (Gboard asli, sinkron dua HP lintas versi).

- [ ] **Step 4: Verifikasi akhir**

Run: `./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease`, lalu `:app:connectedDebugAndroidTest`.
Expected: hijau, kecuali kegagalan screenshot yang sudah dicatat. Tulis angkanya ke laporan dan ACTIVITY_LOG.

- [ ] **Step 5: Commit, push, pointer**

```bash
git add -A docs logs
git commit -m "docs(mankeu): close redesign phase 1 - products and HPP"
git push origin main
```

Lalu di superproject (`E:\managed-projects-handoff`): `git add mankeu`, entri root `logs/ACTIVITY_LOG.md`, commit
`chore(mankeu): bump pointer - phase 1 products and HPP`, push. Rilis ke internal testing (Play Console) adalah tindakan
pemilik: siapkan catatan rilis "perbarui semua HP yang memakai sinkron" (DEC-028 §7), tetapi jangan unggah.

---

## Self-review (dijalankan penulis rencana)

- **Cakupan spec:**
  - #5 (Task 2, 3, 5, 6).
  - #6: resep per batch dan HPP otomatis (Task 1, 2, 5); snapshot dipindah ke F2 (DEC-028 §5, dicatat di roadmap Task 7).
  - #9: stok otomatis, Produksi, bahan terpisah dan tidak dikurangi (Task 1, 3, 5).
  - #20: bahan + jumlah, harga terakhir, peringatan, penjualan lama tidak berubah karena belum ada snapshot (Task 1, 3, 6).
  - #13 (editor halaman penuh, sheet pendek) dan #19 (string bahasa awam).
  - Kriteria F1 diuji di Task 1 (golden), Task 3 (alur data), Task 6 (peringatan), Task 7 (emulator).
- **Placeholder:** tidak ada.
  - Kode SQL migrasi dan teks string sudah lengkap. Implementer hanya perlu mencocokkan `createSql` dengan `13.json` yang
    dihasilkan Room.
  - Kode UI Task 5–6 dijabarkan per komponen dengan nama string dan fungsi yang persis, bukan disalin utuh, karena
    bergantung pada pola komponen yang sudah ada (`ChoiceMenu`, `MoneyField`, `MankeuSheet`, `OverlayScaffold`).
- **Konsistensi tipe:**
  - `ProductRecord` (Task 2) diperluas dengan `sales` (Task 3), dengan default `emptyList()`, sehingga pemanggil Task 2 tetap sah.
  - `saveProduct(projectId, id, draft)` di repository sejak Task 2; tanda tangan ViewModel berubah di Task 5.
  - `PlanAssembly.products(rows, materialPrices)` sejak Task 2; `actualMonth(..., products)` sejak Task 3.
  - `LegacyProductShape.draft/components` hidup dari Task 2 sampai Task 5, lalu dihapus.
