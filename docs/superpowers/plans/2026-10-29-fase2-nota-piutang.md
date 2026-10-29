# Fase 2: Nota Multi-Produk, Piutang, Utang Pemasok, Konsinyasi, Untung saat Terjual â€” Rencana Implementasi

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development. Langkah memakai checkbox.
> Setiap tugas selesai: commit + entri `logs/ACTIVITY_LOG.md`, dan baris status di bagian **Status** paling bawah
> diperbarui di commit yang sama (permintaan pemilik: dokumentasi bertahap supaya bisa dilanjutkan setelah terputus).

**Goal:** Pengusaha mencatat penjualan seperti cara ia berjualan: satu nota ke Toko A berisi beberapa produk, lunas
atau belum, dibayar sebagian, atau titip jual. Piutang, utang ke pemasok, dan barang titipan terlihat jelas. Untung
bulanan dihitung saat barang terjual (penjualan âˆ’ HPP âˆ’ biaya tetap), terpisah dari uang masuk/keluar.

**Architecture:**
- **Nota** adalah dokumen (`sales` + `sale_lines`) yang menyimpan snapshot HPP per baris.
- **Uang** tetap tercatat sebagai `financial_events`. Pembayaran nota adalah event uang masuk yang menautkan `saleId`.
  Belanja tempo adalah event baru tanpa kas, dan pembayarannya event uang keluar yang menautkan belanja itu.
- **Untung** dihitung oleh kalkulator murni `ProfitCalculator` dari baris nota, pemasukan non-produk, dan pengeluaran
  dengan sifat biaya efektif yang dibaca saat itu (bukan dari jurnal). Akibatnya, mengubah sifat biaya kategori
  langsung benar untuk masa lalu.
- **Arus kas dan saldo kas** tetap dari postingan jurnal.
- **Piutang, utang, dan saldo titipan** diturunkan saat dibaca, sama seperti stok di Fase 1.

**Tech Stack:** Kotlin, Compose M3, Room 2.8.4 (schema 13 â†’ 14), JUnit4, `MigrationTestHelper`.

**Spec:** `docs/superpowers/specs/2026-10-29-redesign-spec.md`: #2, #3, #6 (snapshot, dipindah dari F1), #7, #17, #19, #13,
dan **Keputusan Fase 2 F2-1 sampai F2-7**. Peta fase: `2026-10-29-redesign-roadmap.md` (kriteria F2).
Fase 1 dan perapihannya: `2026-10-29-fase1-produk-hpp.md`, `2026-10-29-fase1-perapihan.md`, DEC-028.

## Global Constraints

- Uang selalu `Long` rupiah bulat; pembagian dibulatkan setengah ke atas dengan satu aturan yang diuji. Kuantitas `BigDecimal`.
- Operasi lokal tanpa login atau jaringan. Tidak ada dependensi baru.
- Laporan adalah turunan; tidak ada input kedua di layar laporan.
- Tidak ada pinjaman, bunga, atau cicilan bank (#2); yang ada hanya piutang pelanggan, utang pemasok, dan titip jual.
- Bahasa sehari-hari di layar (tagihan, belum dibayar, titip, untung, uang masuk/keluar); istilah formal (piutang usaha,
  utang usaha, HPP, persediaan konsinyasi) hanya di PDF / "Rincian analisis" (#19). "HPP" boleh di layar (#19).
- Form panjang (nota, titip) = halaman penuh dengan Simpan tetap di bawah; sheet hanya untuk form pendek (bayar, tarik) (#13).
- DEC-012 berlaku: setiap entitas baru ikut template v5, dan ekspor â†’ impor ulang tanpa kehilangan. Template v5 belum
  pernah dirilis, jadi lembar baru tidak menaikkan versi.
- Setiap commit membawa log (ACTIVITY_LOG, dan DECISION_LOG/CHANGELOG bila relevan) dengan jam nyata. String baru ditulis di `values` dan `values-en`.
- Gradle dari PowerShell dengan PATH dibersihkan: `$env:Path = ($env:Path -split ';' | Where-Object { $_ -notmatch '"' }) -join ';'`.

## Keputusan rencana (penulis rencana; masuk DEC-029 di Task 2)

Format: keputusan â€” alasan â€” biaya bila salah.

1. **Satu kas.** Tidak ada pembedaan tunai/transfer atau rekening bank. Fase 2 tentang tagihan, bukan dompet.
   Biaya bila salah: rekening ditambah nanti sebagai kolom di event.
2. **Pembayaran nota = event uang masuk (`CASH_INCOME`) dengan `saleId`.** Jurnalnya Debit Kas / Kredit Piutang, bukan
   Pendapatan; pendapatan diakui oleh nota. Nota lunas = nota + satu pembayaran di tanggal yang sama, dibuat otomatis.
   Biaya bila salah: jenis event baru bila perlu dipisah.
3. **Belanja tempo = event baru `CREDIT_PURCHASE`** (tanpa kas; Debit Beban / Kredit Utang), dengan `contactId` dan
   `dueDateEpochDay`. Pembayarannya `CASH_EXPENSE` dengan `settlesEventId`, berjurnal Debit Utang / Kredit Kas. Belanja
   tempo bisa menautkan bahan dan ikut menentukan harga bahan (#20, sama seperti belanja tunai). Biaya bila salah: satu
   jenis event yang perlu dipetakan di laporan.
4. **Untung dihitung `ProfitCalculator` dari data, bukan dari postingan.**
   - Pendapatan = total nota setelah diskon (menurut tanggal nota) + uang masuk non-produk tanpa nota.
   - HPP = jumlah snapshot HPP baris nota.
   - Biaya = pengeluaran (tunai atau tempo, menurut tanggal belanja) yang sifat biaya efektifnya bukan `VARIABLE`.
   - Belanja variabel ditampilkan sebagai "Belanja produksi (lewat HPP)" plus baris "Selisih belanja produksi vs HPP terjual" (F2-1).
   - Penyusutan aset tetap seperti `PlanAssembly.actualMonth` (sudah ada).
   - Pembayaran nota dan pembayaran utang **bukan** pendapatan atau biaya.

   Biaya bila salah: satu kalkulator yang diganti di F4.
5. **Diskon per nota dibagi ke baris secara proporsional** (dibulatkan setengah ke atas, sisa pembulatan ke baris
   terakhir) hanya untuk laporan per produk. Total nota tetap total âˆ’ diskon. Biaya bila salah: untung per produk
   bergeser Rp1.
6. **Status nota diturunkan:** lunas (dibayar â‰¥ total), sebagian (0 < dibayar < total), belum (dibayar 0). Titip
   bukan status nota; titip adalah dokumen sendiri yang menghasilkan nota saat laku (F2-6). Lewat jatuh tempo =
   belum/sebagian dan `dueDate` < hari ini. Biaya bila salah: tidak ada, karena status selalu dihitung ulang.
7. **Nota menyimpan satuan, isi, harga, dan HPP per baris saat dibuat (snapshot).** Mengubah produk tidak mengubah
   nota lama. Mengedit nota menghitung ulang HPP baris memakai HPP saat diedit, dengan pemberitahuan. Biaya bila
   salah: HPP nota lama yang diedit ikut harga baru.
8. **Stok produk:**
   - Nota mengurangi stok sebesar jumlah Ã— isi.
   - Titip mengurangi stok "di gudang" dan menambah "dititipkan" per toko.
   - Laku dari titipan membuat nota yang **tidak** mengurangi stok lagi, karena barangnya sudah keluar saat titip.
   - Tarik mengembalikan barang ke stok.
   - Stok yang tampil = di gudang; "dititipkan N" tampil terpisah.

   Biaya bila salah: angka stok perlu ditinjau ulang.
9. **Kontak per usaha** (bukan per unit), memakai kunci alami sinkron (`businessId`, `normalizedName`). Kontak
   diarsipkan, tidak dihapus, bila sudah dipakai. Biaya bila salah: kontak per unit ditambah kolom nanti.
10. **Migrasi data lama (F2-4):**
    - Setiap `CASH_INCOME` berproduk di buku realisasi menjadi nota lunas satu baris (`hppEstimated = 1`), dan event
      itu menjadi pembayaran nota (`saleId` diisi, id tetap). HPP baris memakai biaya resep saat migrasi.
    - Id nota = `<eventId>-s`, baris = `<eventId>-l`, sehingga dua HP yang bermigrasi sendiri menghasilkan baris yang sama.
    - Belanja lama tetap belanja tunai.

    Biaya bila salah: HPP penjualan lama hanya perkiraan, dan itu ditandai.
11. **Form uang masuk tidak lagi menawarkan produk.** Penjualan produk selalu lewat nota (satu jalur). Uang masuk
    biasa (jasa, lain-lain) tetap di form lama. Biaya bila salah: pengguna yang terbiasa harus pindah ke tombol Nota.
12. **Tempat di navigasi (sementara, sampai F3):** tombol catat (FAB) mendapat "Nota penjualan". Tab Catatan
    menampilkan daftar nota di atas daftar transaksi, dengan filter "Belum lunas". Beranda mendapat kartu "Tagihan"
    (piutang + utang + lewat jatuh tempo). Kontak dibuka dari menu Data. Biaya bila salah: F3 memindahkan tempatnya.

## Peta berkas

| Berkas | Tanggung jawab | Tugas |
|---|---|---|
| `domain/SalesMath.kt` (baru) | Total nota, diskon per baris, status, sisa tagihan | 1 |
| `domain/ProfitCalculator.kt` (baru) | Untung saat terjual + selisih belanja produksi | 1 |
| `domain/ConsignmentMath.kt` (baru) | Saldo titipan per toko per produk | 1 |
| `data/Entities.kt` | `ContactEntity`, `SaleEntity`, `SaleLineEntity`, `ConsignmentEntity`, `ConsignmentLineEntity`; `FinancialEventEntity` + `saleId`, `contactId`, `dueDateEpochDay`, `settlesEventId`; `ProductMovementEntity` jenis titip/tarik | 2 |
| `data/Migration13To14.kt` (baru), `backup/BackupUpgrade.kt` | Schema 14 + konversi F2-4 | 2 |
| `domain/FinanceModels.kt`, `domain/LedgerPolicy.kt` | `CREDIT_PURCHASE`; jurnal pembayaran nota/utang | 2 |
| `data/FinanceDao.kt`, `data/FinanceRepository.kt`, `data/SalesRecord.kt` (baru) | Kontak, nota, pembayaran, belanja tempo, titip/laku/setor/tarik, saldo turunan | 3 |
| `data/PlanAssembly.kt`, ringkasan (`accountingSummary`) | Untung dari `ProfitCalculator`, unit terjual dari baris nota | 4 |
| `presentation/screens/ContactsScreen.kt`, `SaleEditorScreen.kt`, `SalesListSection.kt`, `ConsignmentScreen.kt` (baru) | UI | 5, 6 |
| `importer/*`, `exporter/*` | Lembar Kontak, Nota, Baris Nota, Titip; kolom event baru | 7 |

---

### Task 1: Matematika nota, untung, dan titipan (domain murni)

**Files:** Create `domain/SalesMath.kt`, `domain/ProfitCalculator.kt`, `domain/ConsignmentMath.kt`; Test `domain/SalesMathTest.kt`, `ProfitCalculatorTest.kt`, `ConsignmentMathTest.kt`.

**Interfaces (Produces):**

```kotlin
data class SaleLineInput(val productId: String, val quantity: BigDecimal, val unitPriceRupiah: Long, val hppRupiah: Long)
enum class SaleStatus { PAID, PARTIAL, UNPAID }
object SalesMath {
    fun lineTotal(quantity: BigDecimal, unitPriceRupiah: Long): Long        // TransactionCalculation.computeTotalRupiah
    fun subtotal(lines: List<SaleLineInput>): Long
    fun total(lines: List<SaleLineInput>, discountRupiah: Long): Long        // require 0 <= discount <= subtotal
    /** Discount shared by line value, half up, remainder to the last line; sum == discount. */
    fun discountShares(lines: List<SaleLineInput>, discountRupiah: Long): List<Long>
    fun status(totalRupiah: Long, paidRupiah: Long): SaleStatus
    fun outstanding(totalRupiah: Long, paidRupiah: Long): Long              // max(0, total - paid)
    fun isOverdue(dueDate: LocalDate?, outstanding: Long, today: LocalDate): Boolean
}

data class ProfitSale(val date: LocalDate, val revenueRupiah: Long, val hppRupiah: Long, val hppEstimated: Boolean)
data class ProfitExpense(val date: LocalDate, val amountRupiah: Long, val variable: Boolean)
data class ProfitAsset(val date: LocalDate, val amountRupiah: Long, val usefulLifeMonths: Int)
data class MonthProfit(
    val month: YearMonth,
    val salesRupiah: Long,               // nota setelah diskon
    val otherIncomeRupiah: Long,         // uang masuk non-produk tanpa nota
    val hppRupiah: Long,
    val fixedCostRupiah: Long,
    val depreciationRupiah: Long,
    val productionSpendRupiah: Long,     // belanja variabel, tidak dikurangkan
    val hppEstimatedIncluded: Boolean,
) {
    val grossProfitRupiah get() = salesRupiah - hppRupiah
    val profitRupiah get() = salesRupiah + otherIncomeRupiah - hppRupiah - fixedCostRupiah - depreciationRupiah
    /** Positive: more bought for production than sold at HPP (stock built up or HPP too low). */
    val productionVarianceRupiah get() = productionSpendRupiah - hppRupiah
}
object ProfitCalculator {
    fun month(month: YearMonth, sales: List<ProfitSale>, otherIncome: List<Pair<LocalDate, Long>>, expenses: List<ProfitExpense>, assets: List<ProfitAsset>): MonthProfit
}

data class ConsignmentMove(val contactId: String, val productId: String, val baseQuantity: BigDecimal, val kind: Kind) { enum class Kind { SENT, SOLD, RETURNED } }
object ConsignmentMath {
    /** Base units still at each (contact, product): sent âˆ’ sold âˆ’ returned; never below zero is enforced by the repository, not here. */
    fun balances(moves: List<ConsignmentMove>): Map<Pair<String, String>, BigDecimal>
}
```

Penyusutan: `amount / life` (pembagian bulat, sama dengan `PlanAssembly.actualMonth`) untuk bulan pembelian sampai
`life - 1` bulan sesudahnya. Pindahkan rumusnya ke sini dan buat `PlanAssembly` memakainya, sehingga hanya ada satu rumus.

- [ ] **Step 1: Tes golden (kriteria F2) â€” gagal dulu.** Bulan Oktober 2026, usaha keripik (HPP Rp6.000/bungkus, dus isi 40 = Rp240.000):
  - Nota A ke Toko A: 10 dus Ã— Rp300.000 + 100 bungkus Ã— Rp8.000 = 3.800.000, diskon 100.000 â†’ total 3.700.000; HPP 10 Ã— 240.000 + 100 Ã— 6.000 = 3.000.000.
  - Laku titipan Toko B: 6 dus Ã— Rp280.000 = 1.680.000; HPP 1.440.000.
  - Uang masuk jasa antar Rp50.000 (tanpa nota).
  - Belanja variabel: bahan 4.900.000 (tunai) + kemasan 300.000 (tempo, belum dibayar).
  - Biaya tetap: sewa 1.000.000, listrik 200.000.
  - Aset: mesin 3.600.000 umur 36 bulan dibeli September â†’ penyusutan 100.000.
  - Pembayaran sebagian Nota A Rp2.000.000 di Oktober dan pembayaran utang lama Rp500.000: tidak memengaruhi untung.

  Hitungan tangan yang menjadi ekspektasi:
  - penjualan 5.380.000;
  - lain 50.000;
  - HPP 4.440.000;
  - biaya tetap 1.200.000;
  - penyusutan 100.000;
  - **untung 5.380.000 + 50.000 âˆ’ 4.440.000 âˆ’ 1.200.000 âˆ’ 100.000 = âˆ’310.000**;
  - belanja produksi 5.200.000, selisih +760.000.

  Tambahkan tes:
  - pembagian diskon: 3 baris (1.000, 2.000, 3.000), diskon 100 â†’ (17, 33, 50); jumlahnya harus 100;
  - status di batas: 0, sebagian, persis lunas, lebih bayar â†’ PAID dengan outstanding 0;
  - overdue: jatuh tempo kemarin dengan sisa > 0 â†’ true; sisa 0 â†’ false;
  - saldo titip: kirim 10 dus = 400, laku 240, tarik 160 â†’ 0;
  - diskon > subtotal â†’ `IllegalArgumentException`.
- [ ] **Step 2: Implementasi minimal**, satu aturan pembulatan (`RoundingMode.HALF_UP`), tanpa floating point.
- [ ] **Step 3: Unit test penuh lulus; commit** (log + baris Status).

### Task 2: Schema 14, migrasi F2-4, jurnal, backup

**Files:** `Entities.kt`, `AppDatabase.kt` (versi 14, entitas, `DATA_TABLES`: `contacts` setelah `businesses`; `sales`, `sale_lines`, `consignments`, `consignment_lines` setelah produk dan **sebelum** `financial_events`, induk dulu), `Migration13To14.kt`, `BackupUpgrade.kt` (13 â†’ 14, dan 12 â†’ 13 â†’ 14 berantai), `SyncEngine.NATURAL_KEYS` (`contacts`: `businessId`, `normalizedName`) dan `REFERENCES` (`contactId` â†’ `contacts`, `saleId` â†’ `sales`), `FinanceModels.kt` (`CREDIT_PURCHASE`; `LedgerAccount.RECEIVABLE`, `PAYABLE`), `LedgerPolicy.kt`, `docs/DATABASE_MIGRATIONS.md`, DEC-029.

**Entitas:**

```kotlin
@Entity(tableName = "contacts", foreignKeys = [ForeignKey(BusinessEntity::class, ["id"], ["businessId"], ForeignKey.CASCADE)],
    indices = [Index("businessId"), Index(value = ["businessId", "normalizedName"], unique = true)])
data class ContactEntity(@PrimaryKey val id: String, val businessId: String, val name: String, val normalizedName: String,
    val phone: String? = null, val address: String? = null, val notes: String? = null,
    val isCustomer: Boolean = true, val isSupplier: Boolean = false, val archived: Boolean = false,
    val createdAtEpochMillis: Long, val updatedAtEpochMillis: Long = createdAtEpochMillis, val importBatchId: String? = null)

@Entity(tableName = "sales", foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], ForeignKey.CASCADE)],
    indices = [Index("projectId"), Index("contactId"), Index("consignmentId"), Index("importBatchId")])
data class SaleEntity(@PrimaryKey val id: String, val businessId: String, val projectId: String, val contactId: String? = null,
    val dateEpochDay: Long, val dueDateEpochDay: Long? = null, val discountRupiah: Long = 0, val notes: String = "",
    /** Set when the sale is a consignment sell-through (F2-6); such lines do not take stock again (DEC-029 Â§8). */
    val consignmentId: String? = null,
    /** Migrated from a schema-13 cash sale (F2-4): the HPP is the recipe cost at migration time. */
    val hppEstimated: Boolean = false,
    val importBatchId: String? = null, val createdAtEpochMillis: Long, val updatedAtEpochMillis: Long = createdAtEpochMillis)

@Entity(tableName = "sale_lines", foreignKeys = [ForeignKey(SaleEntity::class, ["id"], ["saleId"], ForeignKey.CASCADE)],
    indices = [Index("saleId"), Index("productId")])
data class SaleLineEntity(@PrimaryKey val id: String, val saleId: String, val productId: String,
    val unitName: String, val contentText: String, val quantityText: String, val unitPriceRupiah: Long,
    val totalRupiah: Long, /** HPP of this line when sold (spec #6). */ val hppRupiah: Long, val sortOrder: Int = 0)

@Entity(tableName = "consignments", foreignKeys = [ForeignKey(ProjectEntity::class, ["id"], ["projectId"], ForeignKey.CASCADE)],
    indices = [Index("projectId"), Index("contactId")])
data class ConsignmentEntity(@PrimaryKey val id: String, val businessId: String, val projectId: String, val contactId: String,
    val dateEpochDay: Long, val notes: String = "", val importBatchId: String? = null,
    val createdAtEpochMillis: Long, val updatedAtEpochMillis: Long = createdAtEpochMillis)

/** One product sent on consignment, priced per sale unit at sending (F2-6). */
@Entity(tableName = "consignment_lines", foreignKeys = [ForeignKey(ConsignmentEntity::class, ["id"], ["consignmentId"], ForeignKey.CASCADE)],
    indices = [Index("consignmentId"), Index("productId")])
data class ConsignmentLineEntity(@PrimaryKey val id: String, val consignmentId: String, val productId: String,
    val unitName: String, val contentText: String, val quantityText: String, val priceRupiah: Long, val sortOrder: Int = 0)
```

`FinancialEventEntity` mendapat empat kolom nullable: `saleId`, `contactId`, `dueDateEpochDay`, `settlesEventId`, masing-masing dengan index.
`ProductMovementEntity.movementType` mendapat `CONSIGN_RETURN` (tarik). "Kirim titip" tidak disimpan sebagai gerakan,
melainkan dibaca dari `consignment_lines`. "Laku" dibaca dari baris nota yang punya `consignmentId`. `ProductMovementType` di domain mendapat `CONSIGN_OUT` dan `CONSIGN_RETURN`.

**Jurnal (`LedgerPolicy`):**
- `CASH_INCOME` dengan `saleId`: Debit Kas / Kredit Piutang.
- `CASH_INCOME` tanpa `saleId`: seperti sekarang.
- `CREDIT_PURCHASE`: Debit Beban (atau Aset bila kategori ASSET; boleh dibatasi ke EXPENSE dulu) / Kredit Utang; peran kas tidak ada, artinya tidak menyentuh akun Kas.
- `CASH_EXPENSE` dengan `settlesEventId`: Debit Utang / Kredit Kas, peran OPERATING.

Akibatnya, laporan kas lama tetap benar. `PostingReportCalculator` tidak lagi menjadi sumber untung (Task 4).

**Migrasi 13â†’14:** buat tabel dan kolom, lalu konversi F2-4 (DEC-029 Â§10).
- Untuk setiap event `CASH_INCOME` dengan `productId` dan buku `REALIZATION`:
  - sisipkan `sales` (id `<eventId>-s`, tanggal, `hppEstimated = 1`) dan `sale_lines` (id `<eventId>-l`, satuan dari `event.unit` atau satuan utama, isi dari satuan itu, jumlah, harga satuan = `event.unitPriceRupiah` atau `amount Ã· qty`, total = `event.amountRupiah`);
  - HPP dihitung lewat `ProductCosting` dengan harga bahan saat migrasi; baca baris yang diperlukan di migrasi lalu pakai fungsi murni yang sama;
  - set `event.saleId`.
- Jurnal event itu dibangun ulang (Kredit Piutang) di migrasi yang sama.
- `SyncSchema.createTriggers`.

`BackupUpgrade` melakukan konversi yang sama untuk snapshot 13, dan rantai 12 â†’ 13 â†’ 14.

Tes:
- `Migration13To14Test`: penjualan berproduk menjadi nota lunas dengan id deterministik, jurnal Kas/Piutang seimbang, dan uang masuk non-produk tidak berubah.
- `BackupUpgradeTest` 13 â†’ 14 dan 12 â†’ 14.
- `LedgerPolicyTest` untuk tiga jurnal baru.

### Task 3: Repository: kontak, nota, pembayaran, tempo, titip

**Produces (nama persis, dipakai UI):**
- Kontak:
  - `contacts(businessId): Flow<List<ContactEntity>>`
  - `saveContact(businessId, id?, ContactDraft): String` (nama unik per usaha tanpa beda huruf; minimal satu peran)
  - `archiveContact(id)`
- `data class SaleLineDraft(val productId: String, val unitName: String, val quantityText: String, val unitPriceRupiah: Long)`
- `data class SaleDraft(val contactId: String?, val date: LocalDate, val dueDate: LocalDate?, val discountRupiah: Long, val notes: String, val lines: List<SaleLineDraft>, val paidNowRupiah: Long)`
- `saveSale(projectId, id?, draft): String`, dalam satu transaksi:
  - Validasi: baris â‰¥ 1, satuan milik produk, jumlah > 0, harga > 0, diskon 0..subtotal, `paidNow` 0..total, pelanggan wajib bila `paidNow < total` (F2-3).
  - Isi, total, dan HPP baris di-snapshot.
  - Bila `paidNow > 0`, buat pembayaran (`CASH_INCOME`, kategori Penjualan, `saleId`, tanggal nota).
  - Saat mengedit: baris diganti, HPP dihitung ulang (Â§7), dan pembayaran yang ada dibiarkan. `paidNow` hanya berlaku saat membuat; pembayaran setelahnya lewat `recordSalePayment`.
- `recordSalePayment(saleId, amount, date)`: menolak bila melebihi sisa.
- `deleteSale(id)`: menghapus nota beserta pembayarannya. Konfirmasi di UI menyebut jumlah pembayaran.
- `sales(projectId): Flow<List<SaleRecord>>`, dengan `SaleRecord(sale, lines, payments, contact)` dan properti `total`, `paid`, `outstanding`, `status`, `overdue(today)`.
- Belanja tempo:
  - `recordEvent` menerima `type = CREDIT_PURCHASE` dengan `contactId` wajib (pemasok) dan `dueDate` opsional.
  - `recordSupplierPayment(purchaseEventId, amount, date)` membuat `CASH_EXPENSE` dengan `settlesEventId`, kategori sama dengan belanja, dan menolak bila melebihi sisa.
  - `payables(businessId)`: belanja tempo + pembayaran â†’ sisa, jatuh tempo.
- Titip:
  - `sendConsignment(projectId, contactId, date, lines: List<ConsignmentLineDraft>)`: harga per satuan wajib, jumlah > 0.
  - `reportConsignmentSold(consignmentId, soldLines: List<Pair<lineId, quantityText>>, date, paidNowRupiah)`: membuat nota dengan `consignmentId`, harga dari baris titip, dan HPP snapshot. Menolak bila melebihi sisa titipan.
  - `returnConsignment(consignmentId, lineId, quantityText, date)`: menulis gerakan `CONSIGN_RETURN`.
  - `consignments(projectId)`: saldo per toko per produk lewat `ConsignmentMath`.
  - Setor = `recordSalePayment` pada nota laku.
- Stok produk (`ProductRecord.stock`):
  - Penjualan dibaca dari `sale_lines` (bukan lagi dari event berproduk). Baris dari nota ber-`consignmentId` tidak dihitung.
  - Kirim titip mengurangi; tarik menambah.
  - `ProductRecord` mendapat `consigned: BigDecimal`.
- Harga bahan turunan (Fase 1) juga membaca `CREDIT_PURCHASE` yang menautkan bahan.
- Hapus semua data, undo impor, dan sinkron mencakup tabel baru.

Tes androidTest `SalesFlowTest` (golden kriteria F2):
- Nota ke Toko A dengan 2 produk, `paidNow = 0` â†’ status belum dan tampil di daftar tagihan.
- Bayar 1.000.000 â†’ sebagian. Bayar sisanya â†’ lunas, dan bayar lebih ditolak.
- Titip 10 dus ke Toko B â†’ stok turun 400, dititipkan 400.
- Laku 6 â†’ nota baru, stok **tidak** turun lagi, sisa titip 160.
- Setor penuh â†’ nota lunas.
- Tarik 4 â†’ stok +160, titipan 0.
- Belanja tempo 300.000 â†’ utang 300.000; bayar 100.000 â†’ sisa 200.000; kas berkurang hanya 100.000.
- Nota tanpa pelanggan dengan `paidNow < total` ditolak.

### Task 4: Untung saat terjual dan sumber ringkasan

- `accountingSummary(bookId)` untuk buku realisasi:
  - `operatingIncomeRupiah` = penjualan + lain;
  - `operatingExpenseRupiah` = HPP + biaya tetap + penyusutan;
  - `profitRupiah` dari `ProfitCalculator` (semua bulan, atau bulan berjalan sesuai pemanggil yang ada).
  - Arus kas dan saldo kas tetap dari postingan.
- Tambahkan field `hppRupiah`, `productionSpendRupiah`, `productionVarianceRupiah`, `receivableRupiah`, `payableRupiah`
  ke `FinancialSummary` (default 0 supaya pemanggil lain tetap jalan).
- Laporan dan Beranda menampilkan HPP terjual, belanja produksi, selisih (dengan penjelasan satu kalimat), serta tagihan
  dan utang. Tata letaknya minimal, karena F4 merombak laporan.
- `PlanAssembly.actualMonth`: unit terjual dan pendapatan dari baris nota; rumus penyusutan dari Task 1.
- PDF dan xlsx "Ringkasan" memakai angka yang sama.

Tes: unit test ringkasan memakai data golden Task 1, lewat repository (androidTest), sehingga Beranda dan Laporan
menunjukkan untung âˆ’310.000 untuk Oktober.

### Task 5: UI kontak, nota, tagihan

- **Kontak** (halaman penuh dari menu Data):
  - daftar dengan cari, tambah/ubah, peran (pelanggan/pemasok), arsip;
  - per kontak tampil total tagihan dan utang;
  - layar ini juga dipakai sebagai pemilih kontak (cari + "Tambah kontak baru" langsung).
- **Nota penjualan** (halaman penuh, FAB "Nota penjualan"):
  - pelanggan (opsional bila lunas), tanggal, jatuh tempo (opsional, muncul bila belum lunas);
  - baris produk: produk â†’ satuan (default satuan utama) â†’ jumlah â†’ harga terisi, bisa diubah; total baris dan HPP/untung baris tampil kecil;
  - tambah baris, diskon;
  - ringkasan: subtotal, diskon, total, untung nota;
  - "Dibayar sekarang" berupa tiga pilihan cepat (Lunas / Belum dibayar / Sebagian + nominal);
  - Simpan tetap di bawah;
  - validasi dari repository, pesan lewat snackbar, halaman tidak tertutup bila gagal.
- **Daftar nota** di tab Catatan:
  - filter Semua / Belum lunas / Lewat jatuh tempo;
  - kartu menampilkan nama, tanggal, total, status (warna + teks), dan sisa;
  - ketuk â†’ detail nota dengan tombol Bayar (sheet pendek: nominal default sisa, tanggal), Ubah, dan Hapus (konfirmasi).
- **Beranda:** kartu "Tagihan" berisi belum dibayar pelanggan, utang ke pemasok, dan jumlah lewat jatuh tempo. Ketuk â†’ daftar terfilter.
- Form uang masuk tidak lagi menampilkan pilihan produk (Â§11). Teks kecil "Jual produk? Pakai Nota penjualan" dengan tautan.

Tes Compose:
- nota 2 produk: total, diskon, dan untung tampil benar; simpan dengan "Belum dibayar" tanpa pelanggan â†’ pesan;
- bayar dari detail mengubah status.

Ambil screenshot emulator untuk dicek tata letaknya.

### Task 6: UI titip jual dan belanja tempo

- **Titip** (halaman penuh dari FAB "Titip ke toko" atau dari detail kontak):
  - pilih toko (kontak), baris produk dengan satuan, jumlah, dan harga setor per satuan;
  - simpan.
- **Daftar titipan** di tab Produk (bagian "Dititipkan"): per toko, per produk sisa (dalam satuan jual terbesar + sisa), dengan tombol:
  - Laku (halaman pendek: jumlah laku per baris, dibayar sekarang) â†’ nota;
  - Tarik (sheet: jumlah) â†’ stok kembali.
- **Belanja tempo** di form uang keluar: pilihan "Dibayar nanti" untuk kategori biaya. Bila dipilih, muncul pemasok (wajib, pemilih kontak) dan jatuh tempo (opsional); event-nya `CREDIT_PURCHASE`.
- **Daftar utang** (dari kartu Tagihan): sisa per belanja, Bayar (sheet).
- **Detail stok produk** menampilkan "Di gudang N Â· Dititipkan M".

Tes Compose untuk titip â†’ laku â†’ tarik, dan belanja tempo â†’ bayar.

### Task 7: Template v5: kontak, nota, titip, tempo (DEC-012)

Lembar baru dalam `REQUIRED`, ditulis juga oleh template kosong:
- **Kontak**: Nama, Pelanggan (Ya/kosong), Pemasok (Ya/kosong), No. HP, Alamat, Catatan, Diarsipkan.
- **Nota**: No. Nota (kunci di file, mis. id pendek), Tanggal, Pelanggan, Jatuh Tempo, Diskon (Rp), Catatan, Titip (No. Titip atau kosong), HPP Perkiraan (Ya/kosong).
- **Baris Nota**: No. Nota, Produk, Satuan, Jumlah, Harga Satuan (Rp), HPP Baris (Rp).
- **Titip**: No. Titip, Tanggal, Toko, Produk, Satuan, Jumlah, Harga Setor (Rp).
- **Transaksi** mendapat kolom: No. Nota (pembayaran), Kontak, Jatuh Tempo, Melunasi (rujukan ke belanja tempo, mis. nomor baris lain) dan Jenis "Dibayar nanti".

Ketentuan kolom dan validasi:
- Pilih representasi rujukan yang lossless dan dapat divalidasi; catat pilihannya di DEC-029.
- Gerakan tarik masuk ke lembar Produksi & Stok dengan Jenis "Tarik titip".
- Validasi: rujukan nota/titip/kontak harus ada; jumlah laku â‰¤ titip; pembayaran â‰¤ total. Pesan per sel.

Tes round trip: data golden SalesFlowTest â†’ ekspor â†’ impor ke unit baru â†’ nota, status, saldo titip, utang, dan untung sama.
Tambahkan juga tes penolakan rujukan rusak. Konversi aset simulasi (`androidTest/assets/simulasi`) dan generator `make_sims.py` ikut lembar baru.

### Task 8: Uji alur pengusaha di emulator, dokumen, penutupan

Checkpoint ditulis per langkah ke `docs/audit/2026-10-29_UJI_FASE2.md`:
- **CP1** kontak Toko A (pelanggan) dan Pemasok Kemasan.
- **CP2** nota 2 produk belum dibayar â†’ Beranda Tagihan.
- **CP3** bayar sebagian lalu lunas.
- **CP4** titip 10 dus ke Toko B â†’ stok/dititipkan.
- **CP5** laku 6 â†’ nota, setor.
- **CP6** tarik 4.
- **CP7** belanja kemasan tempo â†’ utang â†’ bayar.
- **CP8** Laporan bulan ini: untung = hitungan tangan, selisih belanja produksi terjelaskan.
- **CP9** ekspor â†’ impor ulang tanpa kehilangan.
- **CP10** data dari Fase 1 (penjualan uang masuk berproduk) tampil sebagai nota lunas "HPP perkiraan".

Dokumen yang diperbarui: DOMAIN_MODEL (Kontak, Nota, Titip, aturan untung), PRODUCT_REQUIREMENTS, roadmap (F2 selesai),
WORK_IN_PROGRESS, DEC-029 â†’ Implemented, CHANGELOG. Lalu verifikasi akhir (unit, lint, assembleDebug/Release, connected),
review akhir seluruh branch (model terkuat), satu gelombang perbaikan, merge, push, dan pointer superproject.

---

## Self-review

- **Cakupan spec:**
  - #2: piutang, sebagian, titip, utang pemasok, tanpa pinjaman (T2, T3, T5, T6).
  - #3 + F2-1: T1, T4.
  - #6 snapshot: T2, T3.
  - #7: T3, T6.
  - #17: T3, T5.
  - F2-2 kontak: T5. F2-3: T3. F2-4: T2. F2-5: T3, T6. F2-6: T3, T6. F2-7: T1, T3.
  - #18 notifikasi tetap di F6; jatuh tempo sudah disimpan di sini.
- **Kriteria F2 di roadmap:** golden di T1 (untung) dan T3 (alur nota/titip/tempo), dicek lagi di emulator T8.
- **Konsistensi tipe:** `SaleRecord`, `SaleDraft`, `SaleLineDraft`, `ConsignmentLineDraft`, `ContactDraft` didefinisikan di T3 dan
  dipakai di T5â€“T7. `ProfitCalculator.month` dipakai di T4 dan `PlanAssembly`.
- **Detail UI dan SQL** dijabarkan sebagai spesifikasi, bukan kode utuh, sama seperti Fase 1. `createSql` disalin dari `14.json`.

## Status

Diperbarui di setiap commit tugas.

- 2026-10-29: rencana ditulis setelah grill F2-1..F2-7. Belum ada kode.
- 2026-10-29 16:19: Task 1 selesai. feat(domain): sales, profit and consignment math. Unit test 107/0 (96 + 11 baru); golden untung Oktober -310.000, selisih produksi +760.000; RED = gagal kompilasi sebelum implementasi.
