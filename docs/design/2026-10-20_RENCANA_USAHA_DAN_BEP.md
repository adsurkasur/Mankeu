# Rencana Usaha, Produk & BEP Menyeluruh — Desain

> **Status**: disetujui pemilik (brainstorm 20 Okt 2026, `[DEC-014]`). R-1…R-6 diimplementasikan dan diuji 21 Okt 2026 (lihat §8); dasar BEP dan kebutuhan modal disempurnakan di DEC-016.
> **Menggantikan** F-06 (metode BEP) dan F-11 (editor RAB) di `docs/audit/2026-10-20_RENCANA_PERBAIKAN.md`, serta bagian rencana dari F-04.
> **Rujukan akademik**: `docs/archive/paket_audit_agentic_ai_mankeu.md` (benchmark Kedai Kopi: BEP 400 cup / Rp4.000.000, R/C 1,27, B/C 0,27).

## 1. Masalah

- Kalkulator BEP sekarang meminta empat angka ringkas (harga, biaya variabel/unit, biaya tetap/bulan, target) yang diketik terpisah dan **tidak tersambung ke RAB maupun transaksi**. Padahal UMKM nyata punya banyak produk dan banyak pos biaya. Contoh dosen sendiri berupa rincian: kopi 3.500 + cup 1.000 + plastik 500; sewa + listrik + gaji.
- "Saldo kas awal" dan "Modal sendiri" adalah asumsi ketik yang tumpang tindih dengan transaksi "Setoran Modal Pemilik" (yang sudah bisa dicatat sejak DEC-011).
- Baris RAB dan transaksi tidak tahu apakah sebuah biaya bersifat **tetap** atau **variabel**, sehingga BEP tidak bisa dihitung dari data.

## 2. Keputusan pemilik

| # | Pertanyaan | Keputusan |
|---|---|---|
| B-1 | Sifat biaya ditentukan di mana | **Per kategori** (sifat bawaan) **dan bisa diubah per baris** RAB/transaksi |
| B-2 | Beberapa produk | **Multi-produk + bauran penjualan** (sales mix) |
| B-3 | Saldo kas awal & modal sendiri | **Dihapus dari kalkulator**. Realisasi: transaksi Setoran Modal. Proyeksi: baris rencana Setoran Modal. Kebutuhan modal **dihitung** dari defisit kas terdalam |
| B-4 | Aset dalam BEP | **Ya, sebagai penyusutan**: harga aset ÷ umur pakai (bulan) masuk biaya tetap bulanan |
| B-5 | Definisi produk | **Kartu produk + resep**: nama, satuan, harga jual, target terjual/bulan, rincian biaya variabel per unit |
| B-6 | Biaya variabel untuk BEP Realisasi | **Resep (standar) + tampilkan selisih** terhadap pengeluaran variabel nyata |
| B-7 | Produk & jumlah saat catat penjualan | **Opsional**. Bila kosong, bauran memakai rencana |
| B-8 | Resep memotong stok bahan | **Belum** (tahap berikutnya) |
| B-9 | Letak masukan | **Satu halaman "Rencana Usaha"** di tab Catatan saat mode Proyeksi. Laporan hanya menampilkan hasil |
| B-10 | Cara isi biaya tetap | **Per bulan, berulang** ("Sewa Rp1.000.000 / bulan") |

## 3. Model data (skema 9)

| Perubahan | Isi |
|---|---|
| **Tabel baru `products`** | `id`, `projectId` (per unit usaha), `name`, `unit`, `priceRupiah`, `targetPerMonthText`, `sortOrder`, `archived`, waktu |
| **Tabel baru `product_cost_components`** | `id`, `productId`, `name`, `amountRupiah`, `yieldText` (default "1"; mis. Rp15.000 untuk 50 cup → Rp300/cup), `categoryId?` (untuk analisis selisih), `sortOrder` |
| `categories.costNature` | `FIXED` / `VARIABLE` / null (null untuk pemasukan, modal, prive, aset). Bawaan: Bahan Baku & Stok = VARIABLE, Transportasi = VARIABLE, Gaji & Upah / Sewa / Listrik / Lain-lain = FIXED. Kategori kustom memilih saat dibuat |
| `budget_items` | `costNature?` (override), `recurrence` (`MONTHLY` / `ONCE`), `usefulLifeMonths?` (baris aset) |
| `financial_events` | `costNature?` (override), `productId?` (penjualan; jumlah memakai `quantityText` yang sudah ada), `usefulLifeMonths?` (beli aset) |
| `scenario_assumptions` | **dihapus**. Isinya dimigrasi (lihat §7) |
| Sync | Tabel baru masuk `AppDatabase.DATA_TABLES`; migrasi memanggil `SyncSchema.createTriggers` |

Periode rencana memakai `projects.startDateEpochDay`/`endDateEpochDay` yang sudah ada. Halaman Rencana Usaha memintanya bila kosong.

## 4. Perhitungan (domain murni, satu kalkulator)

Semua uang `Long` rupiah; perhitungan antara `BigDecimal`; pembulatan sekali di akhir.

**Rencana (per bulan)**
- `FC` = Σ biaya tetap berulang/bulan + Σ biaya tetap sekali ÷ jumlah bulan periode + Σ penyusutan (harga aset ÷ umur pakai).
- Per produk *i*: `P_i`, `VC_i = Σ (amount ÷ yield)`, `CM_i = P_i − VC_i`, target `q_i`.
- Bauran `w_i = q_i ÷ Σq`. Bila tidak ada target, BEP dihitung per produk seolah dijual sendiri, dan bauran ditandai "belum diisi".
- `CM rata-rata = Σ w_i·CM_i` → **BEP total unit** = `FC ÷ CM rata-rata`; **BEP per produk** = BEP total × `w_i`; **BEP rupiah** = Σ BEP_i × `P_i`.
- `TR = Σ q_i·P_i`, `TVC = Σ q_i·VC_i`, `TC = FC + TVC`, laba = `TR − TC`, **R/C** = `TR ÷ TC`, **B/C** = laba ÷ `TC`.
- Arus kas rencana per bulan termasuk setoran modal dan pembelian aset (kas keluar penuh di bulan beli). **Kebutuhan modal** = defisit kumulatif terdalam.

**Realisasi (bulan berjalan, "Status BEP bulan ini")**
- `FC aktual` = pengeluaran bulan itu yang sifatnya FIXED + penyusutan aset yang sudah dibeli.
- Volume terjual per produk dari uang masuk yang menyebut produk. Sisa omzet tanpa produk dibagi menurut bauran rencana.
- BEP aktual memakai `VC` resep dan bauran aktual. Status = omzet aktual vs BEP rupiah.
- **Selisih biaya variabel** = pengeluaran VARIABLE nyata − Σ (terjual × VC resep). Positif berarti boros dibanding resep.

**Uji wajib**:
- Benchmark Kedai Kopi tepat 400 cup / Rp4.000.000, dan dengan 700 cup terjual: R/C 1,27, B/C 0,27.
- Contoh dua produk dengan bauran.
- Penyusutan (Rp12.000.000 ÷ 24 bulan = Rp500.000 masuk FC).
- Komponen ber-yield (Rp15.000 ÷ 50 = Rp300/unit).
- Tanpa target → BEP per produk.
- Harga ≤ VC → produk ditandai merugi per unit, tanpa crash.

## 5. Antarmuka

- **Catatan (mode Proyeksi) → "Rencana Usaha"**:
  - Periode
  - **Produk**: kartu berisi harga, biaya variabel/unit, margin, target; ketuk untuk mengubah resep
  - **Biaya Tetap per Bulan**
  - **Biaya Sekali Bayar**
  - **Aset & Penyusutan** (harga, umur pakai, penyusutan/bulan)
  - **Setoran Modal Rencana**
  - Ringkasan kecil di atas: BEP, laba/bulan, kebutuhan modal

  Semua lembar memakai pola `MankeuSheet` (aksi tetap di atas keyboard).
- **Laporan → Kelayakan & BEP**: hanya hasil, dengan **tabel rincian format dosen** (biaya variabel per komponen, biaya tetap per pos, CM, CMR, BEP unit & rupiah per produk dan total, R/C, B/C). Kalkulator ketik lama dihapus.
- **Beranda (Realisasi)**: kartu **Status BEP bulan ini**: omzet vs BEP rupiah, progres unit per produk, selisih biaya variabel.
- **Form catat**:
  - Uang masuk: baris opsional "Produk & jumlah" (nominal terisi harga × jumlah).
  - Uang keluar: sifat biaya kategori tampil dan bisa diubah di "Tambah rincian".
  - Kategori Beli Aset: isian umur pakai (bawaan 36 bulan).
- **Kelola kategori**: pilihan sifat biaya untuk kategori pengeluaran.

## 6. Template Excel (perubahan DEC-012)

- `Parameter BEP` diganti dua lembar: **Produk** (Nama · Satuan · Harga Jual (Rp) · Target per Bulan) dan **Resep Produk** (Produk · Komponen · Biaya (Rp) · Untuk Berapa Unit). *Sejak template v5 (DEC-028): lembar Produk, Satuan Jual, Resep Produk, dan Produksi & Stok berbentuk lain; lihat `MankeuTemplate` di `ImportModels.kt`.*
- `Rencana Anggaran` diberi kolom **Sifat** (Tetap/Variabel), **Berulang** (Bulanan/Sekali), **Umur Pakai (bulan)**.
- `Transaksi` diberi kolom opsional **Produk** dan **Jumlah**.
- Ekspor dan PDF memuat tabel rincian BEP yang sama dengan layar.

## 7. Migrasi data lama (skema 8 → 9)

- `scenario_assumptions` buku Proyeksi yang berisi harga/target dikonversi menjadi:
  - satu produk "Produk utama" (harga, satuan, target);
  - satu komponen resep "Biaya variabel" (nilai VC lama);
  - satu baris biaya tetap bulanan "Biaya tetap (dari kalkulator lama)" (nilai FC lama, bila diisi).
- `ownerCapitalRupiah` > 0 → baris rencana Setoran Modal. `openingCashRupiah` **tidak** diubah menjadi transaksi, karena itu sama dengan mengarang catatan. Nilainya disebut di notifikasi agar pemilik mencatatnya sendiri bila perlu.
- Semua konversi dicatat dalam satu notifikasi sistem. Kategori bawaan lama mendapat `costNature` sesuai namanya; sisanya dibiarkan kosong dan ditandai "perlu dipilih" di Kelola kategori.

## 8. Tahapan implementasi

| Tahap | Isi | Selesai bila |
|---|---|---|
| R-1 | Kalkulator domain (rencana, realisasi, penyusutan, bauran) | Uji §4 lolos, termasuk benchmark dosen |
| R-2 | Skema 9 + repository + migrasi §7 | `MigrationTest` 8→9 dengan data asumsi lama; trigger sync aktif di tabel baru |
| R-3 | Halaman Rencana Usaha | Uji manual emulator: buat 2 produk + resep + biaya tetap + aset → BEP sesuai hitungan tangan |
| R-4 | Laporan Kelayakan & BEP + kartu Beranda | Tabel rincian cocok dengan kalkulator; status bulan ini muncul |
| R-5 | Form catat (produk & jumlah, sifat, umur aset) + realisasi & selisih | Uji manual: jual 700 cup → R/C 1,27 |
| R-6 | Template, ekspor, PDF | Round trip template utuh; PDF memuat rincian |

**Status 21 Okt 2026**

| Tahap | Status | Bukti |
|---|---|---|
| R-1 | selesai | `BusinessPlanTest` 9/9, `PlanAssemblyTest` 4/4 |
| R-2 | selesai | `MigrationTest.migrate8To9…` lolos; migrasi data skema 8 asli di emulator: asumsi lama menjadi "Produk utama", biaya tetap bulanan, dan Setoran Modal |
| R-3 | selesai | Emulator, unit baru "Kedai Benchmark": produk Kopi Susu (resep Rp21.000 untuk 6 cup + kemasan Rp1.500/cup → VC Rp5.000) dan biaya tetap Rp2.000.000 → BEP 400 cup / Rp4.000.000, laba Rp1.500.000, R/C 1,27, B/C 0,27. Aset dan 2 produk hanya diuji di unit test, belum di emulator |
| R-4 | selesai | Laporan → Kelayakan menampilkan kartu dan tabel rincian yang sama dengan halaman rencana; Realisasi menampilkan Status BEP Bulan Ini |
| R-5 | selesai | Emulator: uang masuk dengan produk Kopi Susu, volume 700 → nominal Rp7.000.000 terisi otomatis; ditambah belanja bahan Rp3.500.000 dan sewa+gaji Rp2.000.000 → Status BEP: impas Rp4.000.000, 700 cup, laba Rp1.500.000, R/C 1,27, tanpa selisih resep |
| R-6 | selesai | Round trip template v3 (produk, resep, sifat biaya, berulang, umur aset) di `TemplateImportTest` dan `ImportCommitTest`. PDF dan Excel diperiksa langsung 21 Okt 2026 (`docs/audit/2026-10-21_UJI_ALUR_LINTAS_USAHA.md`), termasuk round trip ekspor → impor yang identik |

Di luar lingkup tahap ini: resep memotong stok bahan (B-8), simulasi "bagaimana jika" interaktif tanpa menyimpan.
