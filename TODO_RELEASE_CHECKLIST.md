# Checklist Tindakan Final Rilis "Mankeu" (Release TODOs)

> **Dokumen riwayat.** Daftar tugas 2026-10-20. Butirnya sudah dikerjakan atau digantikan oleh DEC-011 s.d. DEC-019. **Jangan dijalankan lagi.** Keadaan sekarang (Room schema 11): `README.md`, `docs/DOMAIN_MODEL.md`, `docs/V1_IMPLEMENTATION_SPEC.md`, `docs/diagrams/`, dan keputusan di `logs/DECISION_LOG.md`.

Dokumen ini berisi daftar tugas tindakan konkret, lokasi baris kode, rancangan solusi teknis, dan perintah verifikasi untuk menyelesaikan aplikasi Mankeu hari ini hingga 100% siap rilis (*production-ready*).

---

## 📌 Prioritas P0: Pemblokir Rilis (Release Blockers)

Daftar di bawah ini wajib diselesaikan terlebih dahulu karena berdampak langsung pada kegagalan build/test, kompatibilitas data pengguna, dan kelengkapan laporan keuangan.

---

### [TODO-P0-01] Perbaiki Kompilasi Suite Tes Instrumentasi Android (`LedgerIntegrityTest.kt`)
* **File Terdampak**: `app/src/androidTest/java/com/mankeu/data/LedgerIntegrityTest.kt` (baris 74, 107-110, 134-136, 170-171, 224-240)
* **Status**: ❌ Broken Build
* **Masalah**:
  - `LedgerIntegrityTest.kt` memanggil fungsi `settleObligation`, `getObligationBalances`, dan `candidate(..., sourceObligationId = ...)` yang telah dihapus saat migrasi skema 7 (*Pure Cash*).
  - Menyebabkan `./gradlew compileDebugAndroidTestKotlin` gagal kompilasi (*Unresolved reference 'getObligationBalances'*).
* **Solusi Teknis**:
  - Hapus atau sesuaikan test case yang berkaitan dengan pelunasan utang/piutang (karena schema 7 sudah tidak mendukung obligations).
  - Ganti `summary()` di helper test agar hanya membaca posting rows tanpa menggabungkan `obligationBalances`:
    ```kotlin
    private suspend fun summary() = PostingReportCalculator.calculate(
        dao.getPostingRows(bookId).map {
            PostedRecord(
                it.eventId,
                CashFlowRole.valueOf(it.cashFlowRole),
                LedgerAccount.valueOf(it.account),
                PostingSide.valueOf(it.side),
                it.amountRupiah,
            )
        },
    )
    ```
  - Hapus method `obligationOf()`.
* **Perintah Verifikasi**:
  ```powershell
  ./gradlew compileDebugAndroidTestSources
  ```

---

### [TODO-P0-02] Amankan Kompatibilitas Pemulihan Backup Lama (`EncryptedBackupManager.kt`)
* **File Terdampak**: `app/src/main/java/com/mankeu/backup/EncryptedBackupManager.kt` (baris 262-277 dan baris 298-301)
* **Status**: ⚠️ Rawan Crash saat Restore Cadangan v0.0.4 - v0.0.6
* **Masalah**:
  - Konstanta `TABLES` di baris 366 memuat 14 tabel (termasuk `stock_items` dan `stock_movements`).
  - Baris 299: `require(names.containsAll(TABLES) && (snapshot.schemaVersion < current || names.size == TABLES.size))`
  - File cadangan dari aplikasi v0.0.4 (skema 3), v0.0.5 (skema 4), atau v0.0.6 (skema 5) tidak memiliki tabel stok. Pengecekan `containsAll(TABLES)` akan selalu melempar exception, sehingga backup versi lama gagal dipulihkan (melanggar `OLDEST_RESTORABLE_SCHEMA = 3`).
  - Selain itu, di baris 264: `val table = tables.getValue(tableName)` akan melempar `NoSuchElementException` jika tabel stok tidak ada di cadangan lama.
* **Solusi Teknis**:
  - Pisahkan tabel inti (*Core Tables*) yang wajib ada di setiap versi:
    ```kotlin
    val CORE_TABLES = listOf(
        "businesses", "projects", "books", "categories", "import_batches",
        "budget_items", "financial_events", "journal_entries", "postings",
        "scenario_assumptions", "import_notices", "provenance_records"
    )
    ```
  - Perbarui validasi di `validate(snapshot)`:
    ```kotlin
    val names = snapshot.tables.map { it.name }.toSet()
    require(names.containsAll(CORE_TABLES)) {
        "Daftar tabel cadangan tidak lengkap: tabel inti tidak ditemukan."
    }
    ```
  - Perbarui loop restorasi di `restore()`:
    ```kotlin
    TABLES.forEach { tableName ->
        val table = tables[tableName] ?: return@forEach // Lewati jika tabel baru tidak ada di backup lama
        val liveColumns = writable.query("PRAGMA table_info(`$tableName`)").use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name"))) }
        }
        table.rows.forEach { row ->
            val values = ContentValues(table.columns.size)
            table.columns.zip(row)
                .filter { (column, _) -> column in liveColumns }
                .forEach { (column, value) -> values.putBackupValue(column, value) }
            check(writable.insert(tableName, 0, values) != -1L) { "Gagal memulihkan tabel $tableName." }
            rowCount += 1
        }
    }
    ```
* **Perintah Verifikasi**:
  ```powershell
  ./gradlew testDebugUnitTest --tests com.mankeu.backup.EncryptedBackupCodecTest
  ```

---

### [TODO-P0-03] Cetak Hasil BEP & CVP pada Ekspor Laporan PDF (`FinancePdfExporter.kt`)
* **File Terdampak**: `app/src/main/java/com/mankeu/exporter/FinancePdfExporter.kt` (baris 71-80)
* **Status**: ⚠️ Informasi Finansial Kurang Lengkap
* **Masalah**:
  - `FinancePdfExporter.kt` mencetak saldo awal, biaya tetap, dan biaya variabel di bawah subseksi *"Asumsi"*, namun tidak mencetak **hasil perhitungan CVP** (BEP Unit, BEP Rupiah, R/C Ratio, B/C Ratio, dan Status Kelayakan).
  - Padahal metrik ini sudah dihitung di `BusinessAnalysisCalculator.calculate` dan sudah dicetak di ekspor Excel.
* **Solusi Teknis**:
  - Di dalam loop `bundle.books.sortedBy { ... }`, hitung analisis bisnis:
    ```kotlin
    book.assumptions?.let { assumptions ->
        val inputs = AnalysisAssumptions(
            openingCashRupiah = assumptions.openingCashRupiah,
            ownerCapitalRupiah = assumptions.ownerCapitalRupiah,
            otherFinancingCostRupiah = assumptions.otherFinancingCostRupiah,
            outputQuantityText = assumptions.outputQuantityText,
            outputUnit = assumptions.outputUnit,
            outputPriceRupiah = assumptions.outputPriceRupiah,
            fixedCostRupiah = assumptions.fixedCostRupiah,
            variableCostPerUnitRupiah = assumptions.variableCostPerUnitRupiah,
        )
        val analysis = BusinessAnalysisCalculator.calculate(book.summary, inputs)

        report.subsection("Titik Impas (BEP) & Kelayakan Usaha")
        report.keyValue("Biaya Tetap (FC)", money(inputs.fixedCostRupiah ?: (book.summary.operatingExpenseRupiah + inputs.otherFinancingCostRupiah)))
        inputs.variableCostPerUnitRupiah?.let { report.keyValue("Biaya Variabel/HPP Satuan (VC)", money(it)) }
        inputs.outputPriceRupiah?.let { report.keyValue("Harga Jual Satuan (P)", money(it)) }
        analysis.breakEvenQuantity?.let { report.keyValue("BEP Volume (Unit)", "${it.stripTrailingZeros().toPlainString()} ${assumptions.outputUnit ?: "unit"}") }
        analysis.breakEvenRevenueRupiah?.let { report.keyValue("BEP Nilai (Omzet Rupiah)", money(it)) }
        analysis.revenueCostRatio?.let { report.keyValue("Revenue-Cost (R/C) Ratio", it.stripTrailingZeros().toPlainString()) }
        analysis.benefitCostRatio?.let { report.keyValue("Benefit-Cost (B/C) Ratio", it.stripTrailingZeros().toPlainString()) }
        analysis.feasible?.let { report.keyValue("Status Kelayakan", if (it) "LAYAK" else "BELUM LAYAK") }
    }
    ```
* **Perintah Verifikasi**:
  - Jalankan build dan verifikasi ekspor PDF melalui aplikasi di emulator.

---

### [TODO-P0-04] Panggil `budgetTable` pada Ekspor Excel (`FinanceWorkbookExporter.kt`)
* **File Terdampak**: `app/src/main/java/com/mankeu/exporter/FinanceWorkbookExporter.kt` (baris 45-56, 77)
* **Status**: ⚠️ Dead Code / Sheet RAB Hilang
* **Masalah**:
  - Fungsi `budgetTable` sudah ditulis lengkap di baris 77 untuk mengekspor sheet *"Anggaran Rencana/Realisasi"*, namun di baris 48-52 fungsi `export()` hanya menambahkan `transactionTable`.
* **Solusi Teknis**:
  - Tambahkan pemanggilan `budgetTable` di blok `buildList`:
    ```kotlin
    BookMode.entries.forEach { mode ->
        booksByMode[mode]?.let { book ->
            if (book.budgets.isNotEmpty()) {
                add(budgetTable(mode, book, categories))
            }
            add(transactionTable(mode, book))
        }
    }
    ```
* **Perintah Verifikasi**:
  ```powershell
  ./gradlew testDebugUnitTest --tests com.mankeu.exporter.XlsxWorkbookWriterTest
  ```

---

### [TODO-P0-05] Tambahkan Kunci Terjemahan Bahasa Inggris yang Hilang
* **File Terdampak**: `app/src/main/res/values-en/strings.xml`
* **Status**: ⚠️ Missing Translation
* **Masalah**:
  - Kunci `reports_rc_ratio` ada di `values/strings.xml` baris 175, namun hilang di `values-en/strings.xml`.
* **Solusi Teknis**:
  - Tambahkan baris berikut ke dalam `app/src/main/res/values-en/strings.xml`:
    ```xml
    <string name="reports_rc_ratio">Revenue-to-Cost Ratio (R/C Ratio): %1$s</string>
    ```
* **Perintah Verifikasi**:
  ```powershell
  ./gradlew assembleDebug
  ```

---

## 📌 Prioritas P1: Kebutuhan Fungsional & Akademik (Dosen UMKM)

---

### [TODO-P1-01] Sambungkan Fitur Manajemen Stok Minimal ke Tampilan Antarmuka (UI)
* **File Terdampak**: 
  - `app/src/main/java/com/mankeu/presentation/AppViewModel.kt`
  - `app/src/main/java/com/mankeu/presentation/screens/HomeScreen.kt` atau `app/src/main/java/com/mankeu/presentation/screens/EntryScreen.kt`
* **Status**: ❌ Backend-Only (UI Belum Ada)
* **Masalah**:
  - Dosen mewajibkan manajemen persediaan (stok) minimal: master barang, kuantitas stok saat ini, peringatan stok menipis (*low stock alert*), dan pencatatan barang masuk/keluar (*stock opname*).
  - Kode domain (`StockManagement.kt`) sudah siap 100%, Room Dao (`StockDao`) sudah siap, tetapi `AppViewModel` belum mengekspos state stok dan tidak ada composable yang menampilkan stok ke pengguna.
* **Solusi Teknis**:
  1. Di `AppViewModel.kt`:
     - Tambahkan `stockSummary: StateFlow<StockSummary>` yang diobservasi dari `GetStockSummaryUseCase(repository.stockDao)`.
     - Buat fungsi `recordStockMovement(itemId: String, type: StockMovementType, qty: BigDecimal, notes: String)`.
  2. Di UI (`HomeScreen.kt` atau komponen terpisah `StockCard.kt`):
     - Buat kartu ringkas **"Persediaan & Stok Barang"**:
       - Menampilkan: Total Nilai Persediaan (Rp), Jumlah Item, dan Peringatan jika ada item dengan `currentStock <= minAlertStock` (badge merah: *"X Barang Menipis"*).
       - Tombol aksi `[ 📦 Kelola Stok ]` yang membuka Bottom Sheet sederhana untuk melihat daftar stok, mencatat barang masuk, atau penyesuaian stok opname.
* **Perintah Verifikasi**:
  ```powershell
  ./gradlew testDebugUnitTest --tests com.mankeu.domain.StockManagementTest
  ```
  Dan lakukan uji interaktif pada emulator.

---

## 📌 Prioritas P2: Pembersihan Kode Basi & Penyelarasan Dokumentasi

---

### [TODO-P2-01] Bersihkan Stub Usang (*Dead Stubs*) Utang/Piutang
* **File Terdampak**:
  - `app/src/main/java/com/mankeu/data/FinanceRepository.kt` (hapus `data class ObligationView`, fungsi `obligations()`, dan `obligationBalances()`)
  - `app/src/main/java/com/mankeu/presentation/AppViewModel.kt` (hapus field `obligations` di `AppUiState`)
  - `app/src/main/java/com/mankeu/presentation/screens/ReportsScreen.kt` (hapus parameter `onSettle` yang tidak terpakai)
* **Status**: ⚠️ Kebersihan Kode
* **Tindakan**: Hapus deklarasi stub usang tersebut untuk menjaga integritas codebase *pure cash*.

---

### [TODO-P2-02] Selaraskan Berkas Dokumentasi di Folder `docs/`
* **File Terdampak**:
  - `docs/DATABASE_MIGRATIONS.md`: Tambahkan entri Skema 6 (`MIGRATION_5_6`, penambahan tabel stok & CVP) dan Skema 7 (`MIGRATION_6_7`, eliminasi utang/piutang ke arah pure cash).
  - `docs/DOMAIN_MODEL.md`: Perbarui status entitas AR/AP menjadi didepresiasi/pensiun, dan dokumentasikan entitas stok (`StockItemEntity`, `StockMovementEntity`) serta metrik CVP.
  - `docs/PRODUCT_REQUIREMENTS.md`: Tandai FR-ARAP-001 s/d FR-ARAP-004 sebagai digantikan oleh arsitektur kas murni (*Pure Cash MVP*) sesuai arahan akademik.

---

## Ringkasan Perintah Validasi Komprehensif

Sebelum menyatakan rilis final, jalankan urutan perintah verifikasi berikut di terminal:

```powershell
# 1. Jalankan seluruh unit test (wajib 100% lolos)
./gradlew testDebugUnitTest

# 2. Verifikasi kompilasi tes instrumentasi (wajib lolos tanpa error)
./gradlew compileDebugAndroidTestSources

# 3. Rakit APK Debug untuk pengujian
./gradlew assembleDebug

# 4. Rakit APK Release (atau Bundle) untuk memastikan lint dan pengemasan bersih
./gradlew assembleRelease
```
