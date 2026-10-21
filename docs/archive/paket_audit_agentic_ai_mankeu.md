# Paket Instruksi & Standar Skill Agentic AI: Audit Keuangan & Codebase "Mankeu"

> **Dokumen riwayat.** Paket instruksi audit 2026-10-20. Skill aktifnya ada di `.agents/skills/umkm-financial-auditor/SKILL.md`; bagian lain menggambarkan kode sebelum DEC-011. Keadaan sekarang (Room schema 11): `README.md`, `docs/DOMAIN_MODEL.md`, `docs/V1_IMPLEMENTATION_SPEC.md`, `docs/diagrams/`, dan keputusan di `logs/DECISION_LOG.md`.

Paket ini dirancang untuk diumpankan langsung ke **Agentic AI** (seperti Claude Code, Cursor, Codex, Gemini CLI, atau Wilson) yang bertugas melakukan **audit mendalam dan refactoring** terhadap repositori **Mankeu** (`https://github.com/adsurkasur/Mankeu`).

---

## 1. STANDAR SKILL FINANSIAL UMKM (`SKILL.md`)
*(Simpan sebagai `.agents/skills/umkm-financial-auditor/SKILL.md` atau masukkan ke konfigurasi agent)*

```yaml
---
name: umkm-financial-auditor
description: Audit codebase aplikasi keuangan UMKM terhadap standar MVP: BEP Unit & Rupiah, R/C Ratio, B/C Ratio, HPP dasar, pembersihan fitur pinjaman/bunga, dan penambahan manajemen stok minimal.
---

# Standar Evaluasi Finansial MVP UMKM

Gunakan tolok ukur formula dan prinsip berikut saat mengaudit logika bisnis dalam codebase:

## 1. Pemisahan Struktur Biaya & HPP
* **Biaya Tetap (Fixed Cost / FC):** Beban bulanan yang tidak terpengaruh volume penjualan (sewa tempat, gaji pokok, retribusi, listrik dasar).
* **Biaya Variabel (Variable Cost / VC per Unit):** Biaya langsung per unit produk (bahan baku, kemasan/packaging, upah borongan langsung, fee transaksi jika ada).
* **HPP Satuan (COGS per Unit):** Total Biaya Variabel per unit produk.

## 2. Titik Impas / Break Even Point (CVP Analysis)
* **Margin Kontribusi Satuan (Unit Contribution Margin):**
  $$CM = P - VC$$
  *(di mana $P$ = Harga Jual per Unit, $VC$ = Biaya Variabel per Unit)*
* **Rasio Margin Kontribusi (Contribution Margin Ratio):**
  $$CMR = \frac{CM}{P} = \frac{P - VC}{P}$$
* **BEP dalam Satuan Unit:**
  $$BEP_{\text{unit}} = \frac{Total\ FC}{P - VC} = \frac{Total\ FC}{CM}$$
* **BEP dalam Nilai Rupiah / Omzet:**
  $$BEP_{\text{rupiah}} = \frac{Total\ FC}{CMR} = BEP_{\text{unit}} \times P$$

## 3. Rasio Kelayakan & Efisiensi Usaha UMKM
* **Revenue-Cost Ratio ($R/C$ Ratio):**
  $$R/C = \frac{\text{Total Revenue (TR)}}{\text{Total Cost (TC)}}$$
  * $R/C > 1$: Usaha efisien dan menguntungkan.
  * $R/C = 1$: Impas.
  * $R/C < 1$: Usaha merugi / tidak efisien.
* **Benefit-Cost Ratio ($B/C$ Ratio):**
  $$B/C = \frac{\text{Keuntungan Bersih (Net Profit)}}{\text{Total Cost (TC)}} = \frac{TR - TC}{TC} = (R/C) - 1$$
  * $B/C > 0$: Layak dan memberikan nilai tambah laba terhadap modal yang dikeluarkan.

## 4. Standar Manajemen Persediaan (Stok) Minimal UMKM
* **Formula Alur Stok:**
  $$\text{Stok Akhir} = \text{Stok Awal} + \text{Barang Masuk} - \text{Barang Keluar}$$
* **Fitur Minimal:**
  1. Master barang (nama, satuan: pcs/cup/kg, harga beli dasar).
  2. Kuantitas stok terkini (*Current Stock*).
  3. Peringatan stok minimum (*Low Stock Alert*).
  4. Penyesuaian stok opname manual.

## 5. Aturan Eliminasi Fitur (Fitur Tidak Relevan):
* **HAPUS:** Fitur pinjaman uang (*loan amount*) dan kalkulasi bunga (*interest rate / basis points*). Fitur ini tidak relevan untuk pencatatan operasional harian UMKM mikro dan membebani domain model.
```

---

## 2. BENCHMARK STUDI KASUS RIIL UMKM (LEVEL MIKRO)

Gunakan angka studi kasus ini sebagai *test suite* atau verifikasi kalkulasi pada domain layer:

### Profil Usaha: "Kedai Kopi & Teh Susu Berkah"
* **Kategori:** Kuliner / Minuman Mikro (Sistem Cash).
* **Harga Jual ($P$):** Rp 10.000 / cup.

### Struktur Biaya:
1. **Biaya Variabel per Cup ($VC$ / HPP Satuan):**
   * Bubuk Kopi/Teh + Susu: Rp 3.500
   * Gelas Cup + Sedotan + Tutup: Rp 1.000
   * Kantong Plastik / Seal: Rp 500
   * **Total $VC$:** **Rp 5.000 / cup**
2. **Biaya Tetap Bulanan ($FC$):**
   * Sewa lapak/booth: Rp 1.000.000
   * Listrik & Air: Rp 200.000
   * Gaji pembantu operasional: Rp 800.000
   * **Total $FC$:** **Rp 2.000.000 / bulan**

### Hasil Perhitungan Standar:
* **Margin Kontribusi ($CM$):** $\text{Rp } 10.000 - \text{Rp } 5.000 = \text{Rp } 5.000\text{ / cup}$
* **Rasio Margin Kontribusi ($CMR$):** $\frac{5.000}{10.000} = 0{,}5\ (50\%)$
* **BEP Unit:** $\frac{\text{Rp } 2.000.000}{\text{Rp } 5.000} =$ **400 cup / bulan** *(sekitar 13–14 cup per hari)*
* **BEP Rupiah:** $\frac{\text{Rp } 2.000.000}{0{,}5} =$ **Rp 4.000.000 / bulan**

### Simulasi Penjualan Aktual (Bulan Berjalan: 700 cup):
* **Total Revenue (TR):** $700 \times \text{Rp } 10.000 = \text{Rp } 7.000.000$
* **Total Cost (TC):** $\text{Rp } 2.000.000\ (FC) + (700 \times \text{Rp } 5.000\ (VC)) = \text{Rp } 5.500.000$
* **Laba Bersih:** $\text{Rp } 7.000.000 - \text{Rp } 5.500.000 = \text{Rp } 1.500.000$
* **R/C Ratio:** $\frac{\text{Rp } 7.000.000}{\text{Rp } 5.500.000} =$ **1,27** $(> 1 \rightarrow \text{Efisien/Menguntungkan})$
* **B/C Ratio:** $\frac{\text{Rp } 1.500.000}{\text{Rp } 5.500.000} =$ **0,27** $(> 0 \rightarrow \text{Layak})$

---

## 3. CHECKLIST AUDIT & PANDUAN REFACTORING CODEBASE MANKEU

Instruksikan Agentic AI untuk memeriksa titik-titik kode berikut:

### 1. Eliminasi Fitur Pinjaman & Bunga (Deprecation)
* Periksa `app/src/main/java/com/mankeu/domain/BusinessAnalysis.kt`:
  * Temukan parameter `loanAmount` dan `interestRateBP` di data class `AnalysisAssumptions`.
  * Temukan rumus kalkulasi bunga: `loanAmount * interestRateBP / 10000`.
  * **Tindakan:** Tandai untuk dihapus (*prune*) dari domain model dan antarmuka input karena tidak relevan bagi UMKM.
* Periksa Room DB Entity & DAO terkait pinjaman:
  * Hapus kolom atau tabel yang menyimpan data pinjaman modal berbunga.

### 2. Koreksi Rumus BEP & Penambahan Rasio di Domain Layer
* Periksa `BusinessAnalysisCalculator` di `BusinessAnalysis.kt`:
  * Kode saat ini menggunakan penyederhanaan: `totalCost / price`.
  * **Koreksi:** Ubah ke rumus standar: `fixedCost / (price - variableCostPerUnit)`.
  * Tambahkan fungsi perhitungan $R/C$ Ratio ($TR / TC$) dan $B/C$ Ratio ($NetProfit / TC$).

### 3. Penambahan Fitur Manajemen Stok Minimal (Permintaan Dosen)
* Codebase saat ini tidak memiliki tabel stok barang.
* Rancang Room Entity sederhana:
  * `StockItemEntity`: `id`, `name`, `unit` (pcs/kg/cup), `basePrice` (HPP), `currentQuantity`, `minAlertQuantity`.
  * `StockMovementEntity`: `id`, `itemId`, `movementType` (IN / OUT), `quantity`, `date`, `notes`.
  * DAO untuk menambah barang masuk, memotong barang saat penjualan tercatat, dan menampilkan peringatan stok menipis.

### 4. Perampingan UI (Presentation Layer)
* Sederhanakan ViewModel dan Form Input Jetpack Compose agar hanya meminta:
  1. Input Transaksi Kas Harian (Masuk/Keluar).
  2. Input Biaya Tetap & Biaya Variabel Produk.
  3. Input Data Stok Masuk/Keluar.
* Tampilkan kartu ringkasan instan: Status BEP bulan ini dan Skor R/C Ratio.

---

## 4. MASTER PROMPT EKSEKUSI UNTUK AGENTIC AI

Salin prompt ini ke terminal AI agent kamu (Claude Code, Cursor, dll.) di repositori lokal:

```text
Kamu adalah Senior Android & Financial Architect bertugas mengaudit dan merancang refactoring untuk repositori Mankeu ini.

Tugas Audit & Perbaikan:
1. HAPUS FITUR PINJAMAN & BUNGA:
   - Identifikasi semua kemunculan `loanAmount`, `interestRateBP`, dan perhitungan bunga di `app/src/main/java/com/mankeu/domain/BusinessAnalysis.kt`, data layer, dan presentation layer.
   - Buatkan rencana pembersihan/penghapusan field ini agar domain model bersih dari beban yang tidak relevan.

2. KOREKSI RUMUS BEP & TAMBAHKAN R/C & B/C RATIO:
   - Perbaiki implementasi BEP di `BusinessAnalysis.kt` agar menggunakan rumus CVP riil:
     * BEP Unit = Fixed Cost / (Price - Variable Cost)
     * BEP Rupiah = Fixed Cost / Rasio Margin Kontribusi
   - Tambahkan implementasi R/C Ratio (Total Revenue / Total Cost) dan B/C Ratio (Laba Bersih / Total Cost).
   - Validasi kode menggunakan angka benchmark: P=10.000, VC=5.000, FC=2.000.000 -> BEP Unit harus tepat 400.

3. RANCANG MANAJEMEN STOK MINIMAL (PERMINTAAN DOSEN):
   - Konfirmasi bahwa saat ini belum ada entitas stok di Room DB.
   - Buatkan rancangan kode Kotlin untuk Room Entity (`StockItemEntity`), DAO, dan Use Case stok minimal (Stok Awal + Masuk - Keluar) tanpa membuat arsitektur menjadi rumit.

4. SUSUN LAPORAN AUDIT & ROADMAP REFACTORING:
   - Buat file audit summary di `logs/ACTIVITY_LOG.md` sesuai konvensi AGENTS.md proyek ini.
   - Berikan diff atau draf kode Kotlin bersih untuk use cases baru tersebut.
```
