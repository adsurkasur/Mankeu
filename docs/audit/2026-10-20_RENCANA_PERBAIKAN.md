# Rencana Perbaikan Pasca-Audit — Mankeu

> **Status**: diusulkan (belum dikerjakan). Sumber temuan: [`2026-10-20_AUDIT_UIUX_BISNIS_DAN_SISTEM.md`](2026-10-20_AUDIT_UIUX_BISNIS_DAN_SISTEM.md) (ID `A-xx`).
> **Dasar keputusan**: K-1…K-5 (`[DEC-011]`) dan K-6…K-13 tentang template Excel (`[DEC-012]`) dari pemilik, 20 Okt 2026, di `logs/DECISION_LOG.md`.
> **Aturan kerja**: setiap fix ditutup dengan pemeriksaan yang bisa dijalankan (unit test / androidTest / langkah emulator) dan entri log sesuai `AGENTS.md`.

## Status pelaksanaan (diperbarui 20 Okt 2026, 20:00)

| Fix | Status | Catatan |
|---|---|---|
| F-01 Template tunggal | ✅ selesai | Importer hanya baca template; ekspor = template terisi (+ lembar Laporan/Kelayakan); tolak + unduh template; impor ke unit terpilih; duplikat boleh dengan peringatan. Workbook bukti lama dipindah ke `docs/archive/evidence/verification-workbook-legacy/`; studi kasus baru dalam format template **belum** dibuat. |
| F-02 Migrasi 7→8 | ✅ selesai | Termasuk tabel & trigger sync (DEC-013) dan `stock_items.importBatchId`. Diuji `MigrationTest` dan pada data emulator nyata. |
| F-03 Hapus konsep non-kas | ✅ selesai | Sisa: string tak terpakai (masuk F-25). |
| F-08 Validasi impor | ✅ selesai | Identitas usaha dari nama file dihapus. Lembar `Parameter BEP` diganti `Produk` + `Resep Produk` (template v3); resep yang menyebut produk di luar berkas, sifat biaya, berulang, dan umur pakai yang salah ditolak per sel. |
| F-09 Modal/prive/aset lewat kategori | ✅ selesai | Peran `CAPITAL_IN`/`DRAWING`/`ASSET`; form memetakan otomatis. Pengelompokan visual "Modal & Aset" di pemilih: urutan sudah, judul grup belum. |
| F-10 Kategori dasar | 🟡 sebagian | 11 default & pencocokan impor selesai. Kelola kategori (ganti nama, gabung, kelompok per peran, konfirmasi arsip) belum. |
| F-19 Form catat konsisten | 🟡 sebagian | Tombol Batal/Simpan kini tetap terlihat saat keyboard terbuka (permintaan pemilik). Judul ganda belum dirapikan. |
| F-25 Bersih-bersih | 🟡 sebagian | 116 string tak terpakai dihapus (lint UnusedResources 117 → ~1). Dependensi navigation-compose & kode stok tak terpakai belum. |
| Uji manual | ✅ | `2026-10-20_UJI_MANUAL_EMULATOR.md`: 12 skenario lolos, 1 tak teruji (Back di emulator), 3 bug ditemukan & diperbaiki. |
| F-06, F-11 | ✅ diganti & selesai | Oleh Rencana Usaha & BEP (DEC-014). R-1…R-5 selesai 21 Okt 2026 dengan bukti di §8 desain; R-6 sebagian (PDF belum diperiksa langsung). Bagian rencana dari F-04 ikut selesai: Proyeksi melapor dari rencana di semua layar. |
| Lainnya | ⏳ belum | F-04 (sisi perbandingan), F-05, F-07, F-12…F-18, F-20…F-24. |

## 0. Prinsip yang mengikat semua fix

1. **Murni kas (K-1).** Hanya 5 jenis transaksi: `CASH_INCOME`, `CASH_EXPENSE`, `OWNER_CAPITAL`, `OWNER_DRAWING`, `ASSET_PURCHASE_CASH` (+ `REVERSAL` internal). Tidak ada konsep pinjaman, utang/piutang, cicilan, bunga, tempo, atau jatuh tempo di kode, skema, UI, maupun importer. Pengguna tetap bebas membuat kategori sendiri dan memakainya sesuai logikanya; aplikasi tidak menebak.
2. **Satu angka, satu sumber.** Setiap besaran (biaya, laba, kas, rencana) dihitung oleh satu fungsi dan dipakai semua layar, ekspor, dan PDF.
3. **Form tetap dua tombol** (Uang masuk / Uang keluar). Jenis lain ditentukan oleh peran kategori (K-2).
4. **Kategori dasar sedikit, kustom untuk sisanya (K-5).**
5. **Tidak ada data pengguna yang hilang diam-diam.** Apa pun yang dibuang atau diabaikan menghasilkan notifikasi persisten berisi jumlah, nilai, dan sumbernya.

## 1. Urutan fase

```
Fase 1  Murni kas + template Excel  F-01 F-02 F-03
Fase 2  Angka benar                 F-04 F-05 F-06 F-07 F-08
Fase 3  Input yang hilang           F-09 F-10 F-11 F-12
Fase 4  Sederhanakan & periode      F-13 F-14 F-15 F-16 F-17 F-18 F-19 F-20
Fase 5  Bahasa & konsistensi        F-21 F-22 F-23 F-24
Fase 6  Bersih-bersih sistem        F-25
```

Fase 1 harus selesai dulu: semua fase berikutnya menganggap tipe non-kas sudah tidak ada. Dalam satu fase, fix boleh dikerjakan paralel kecuali yang ditandai "butuh".

---

## Fase 1 — Murni kas & template Excel tunggal

### F-01 · Satu template Excel milik Mankeu untuk impor & ekspor · menutup A-02 (sumber), A-09, SYS-5, K-1, K-6…K-11
**Prinsip (K-6)**: Mankeu yang menentukan format. Pengguna mengunduh template, mengisinya, dan mengimpornya. Importer **tidak** menebak bentuk Excel lain.

**Format tunggal (K-6)**: template kosong = file ekspor tanpa isi. Ekspor menghasilkan template yang sudah terisi, jadi ekspor → impor ulang selalu utuh. Lembar (K-10):

| Lembar | Kolom (urutan & judul persis) | Masuk ke |
|---|---|---|
| `Petunjuk` | teks panduan + penanda `MANKEU_TEMPLATE` dan versi format | (hanya penanda) |
| `Transaksi` | Tanggal · Keterangan · Kategori · Pemasukan (Rp) · Pengeluaran (Rp) — gaya buku kas (K-11) | buku Realisasi |
| `Rencana Anggaran` | Bulan (YYYY-MM) · Kategori · Item · Jumlah · Satuan · Harga Satuan (Rp) · Total (Rp) | buku Proyeksi (RAB) |
| `Parameter BEP` | Parameter · Nilai — baris tetap: Satuan produk, Harga jual per satuan, Biaya variabel per satuan, Biaya tetap per bulan, Target volume per bulan | parameter BEP unit usaha (F-06) |
| `Stok Barang` | Nama · Satuan · Harga Beli (Rp) · Jumlah Stok · Batas Minimum | barang di tab Stok |
| `Kategori` | Nama · Peran — referensi saja, ditulis oleh aplikasi | (tidak dibaca) |

**Aturan baca**:
- Satu baris Transaksi mengisi **tepat satu** kolom uang. Jenis transaksi mengikuti kolom + peran kategori (tabel F-09); misalnya "Setoran Modal Pemilik" di Pemasukan → `OWNER_CAPITAL`.
- Kategori yang belum ada **dibuat otomatis sebagai kategori kustom** (K-12). Perannya mengikuti kolom (Pemasukan → Pemasukan, Pengeluaran → Pengeluaran; di RAB: Pengeluaran). Pencocokan memakai nama ternormalisasi (huruf kecil, spasi rapat), tanpa fuzzy. Pratinjau menampilkan daftar "kategori baru".
- Barang stok yang namanya sudah ada dilewati dengan peringatan (bukan ditimpa). Parameter BEP menimpa nilai lama setelah dikonfirmasi di pratinjau.
- **Bukan template / kolom diubah → ditolak (K-7)**: pesan "File ini bukan template Mankeu", daftar lembar/kolom yang kurang atau berubah, dan tombol **Unduh template**. Tidak ada penebakan kolom, bulan, tahun, atau kategori.
- Tidak ada kolom untuk kredit, tempo, pinjaman, atau bunga, jadi konsep non-kas tidak bisa masuk lewat impor (K-1). Jika pengguna menuliskannya sebagai pemasukan/pengeluaran biasa dengan kategorinya sendiri, itu logika pengguna.

**Fungsi impor = input massal (K-9)**: baris ditambahkan ke **unit usaha yang sedang dipilih**. Impor tidak membuat usaha/unit baru dan tidak memakai nama file sebagai nama usaha. Jika belum ada unit, pengguna diminta membuat dulu. Pindah HP dan pemulihan tetap lewat cadangan terenkripsi `.aabk`.

**Duplikat (K-13)**: file yang sama persis (SHA-256 sama dengan batch yang masih tersimpan) **boleh** diimpor lagi, dengan peringatan jelas di pratinjau ("File ini pernah diimpor pada …; transaksinya bisa dobel"). `require(!dao.hasImported…)` di `commitImport` diganti peringatan.

**Tetap dipertahankan**: pratinjau 3 langkah, commit atomik, batch yang bisa dibatalkan, notifikasi impor.

**Dihapus (K-8)**: dukungan workbook referensi 5 lembar dan semua penebakan: `parseRabBudget`, `parseDailyLedger`, `recognizeRoles`, `parseAssumptions`, `parseGenericBudget`, `findDeterministicEnrichments`, `inferMonthFromRow`, `inferYear`, `aggregationCompatible`, pemetaan kata non-kas di `ImportModels.kt:63-80`, `SemanticMatcher.kt`, `CategoryClassifier.kt` (bila tidak dipakai di luar impor), dan `MsmeTemplateExporter.kt` (template kini = `FinanceWorkbookExporter.template()`). Perkiraan: importer ±900 → ±250 baris, ±300 baris lain hilang.

**Dokumen yang harus ikut diperbarui**: `AGENTS.md` bagian *Import and enrichment rules* (sudah diperbarui 20 Okt), `docs/archive/REFERENCE_WORKBOOK_CONTRACT.md`, `docs/archive/IMPORT_RECONCILIATION_AND_NOTIFICATIONS.md`, dan `docs/archive/SEMANTIC_MATCHING.md` diberi status *superseded*. Workbook bukti `docs/evidence/verification-workbook/` dibuat ulang dalam format template (`scripts/generate_verification_workbook.py`).

**Uji**:
- `CanonicalRoundTripTest` menjadi uji inti: ekspor bundel lengkap (4 lembar) → impor → kandidat identik.
- Template kosong → impor → 0 baris, tanpa error.
- File tanpa penanda / kolom tertukar / lembar hilang → ditolak dengan daftar yang tepat.
- Baris berisi Pemasukan dan Pengeluaran sekaligus → error di sel itu.
- Kategori baru dibuat dengan peran benar.
- Impor file sama dua kali → pratinjau memberi peringatan, commit kedua berhasil.
- `GoldenWorkbookEndToEndTest` dan `VerificationWorkbookTest` ditulis ulang di atas template.

### F-02 · Migrasi skema 7→8: buang data & kolom non-kas · menutup A-02 (data), A-03, SYS-1, SYS-9, SYS-10
- **Migrasi `MIGRATION_7_8`**:
  1. Simpan ringkasan per tipe (jumlah baris, total rupiah, rentang tanggal) ke satu `import_notices` baru berjudul "Transaksi non-kas lama dihapus (model kas murni)".
  2. Hapus `financial_events` bertipe non-kas berikut pembaliknya (`reversesEventId` menunjuk ke baris itu). Jurnal dan posting ikut terhapus lewat CASCADE.
  3. Rebuild `financial_events` tanpa `dueDateEpochDay` dan `obligationId`; `counterparty` ikut dibuang (tidak ada di form sejak pure-cash).
  4. Rebuild `scenario_assumptions` tanpa `otherFinancingCostRupiah`.
  5. Arsipkan kategori ber-peran `LIABILITY` dan kategori default "Biaya Bunga Pinjaman" **hanya jika tidak dipakai transaksi mana pun**. Kategori yang dipakai pengguna dibiarkan (K-1: logika pengguna).
- **Restore cadangan lama** (`EncryptedBackupManager`): setelah memulihkan snapshot skema ≤7, jalankan pembersihan yang sama (satu fungsi bersama, dipanggil dari migrasi dan restore). Perbarui komentar `OLDEST_RESTORABLE_SCHEMA` dan `docs/DATABASE_MIGRATIONS.md`.
- **Uji**: `MigrationTest` (Room `MigrationTestHelper`, androidTest) 7→8 dengan baris campuran → baris kas utuh, saldo kas = jumlah kas saja, notifikasi berisi angka yang benar, kolom hilang. `LedgerIntegrityTest.backupTakenOnSchemaThree…` ditambah satu baris `CREDIT_SALE` → setelah restore baris itu tidak ada.

### F-03 · Hapus konsep non-kas dari kode · menutup SYS-1, SYS-4, SYS-12 · butuh F-02
- Hapus 8 tipe non-kas dari `FinancialEventType`, akun `ACCOUNTS_RECEIVABLE/PAYABLE`, `LOAN_PAYABLE`, `INTEREST_EXPENSE`, `ObligationKind/Status/Balance`, field `receivable/payableOutstandingRupiah`, cabang terkait di `LedgerPolicy`, `PostingReportCalculator`, `MonthlyCashFlowCalculator`, `BusinessWords`, `EntryScreen` (form kredit: pihak, jatuh tempo, `kind.credit`), eksporter (kolom "Pihak", "Jatuh Tempo", "ID Kewajiban"), dan PDF.
- Hapus peran `CategoryRole.LIABILITY`.
- **Uji**: seluruh `testDebugUnitTest` + `compileDebugAndroidTestSources` hijau. Hapus `ObligationBalanceTest`. `grep -rniE "loan|pinjam|piutang|utang|obligation|receivable|payable|interest|bunga" app/src/main` hanya boleh menyisakan komentar sejarah migrasi.

---

## Fase 2 — Angka benar

### F-04 · Satu sumber "ringkasan rencana" · menutup A-01
- Buat satu fungsi `planSummary(book)`: jika buku Proyeksi punya RAB → `BudgetPlanCalculator.summarize`, jika tidak → posting. Pakai di Beranda, `ComparisonSection`, ekspor Excel ("Laporan", "Perbandingan"), dan PDF. `AppViewModel.projectionSummary` diganti dengan ini.
- Perbandingan juga per kategori: baris RAB per kategori vs realisasi per kategori.
- **Uji**: unit test `PlanSummary`: RAB tanpa transaksi → pendapatan/biaya/laba sesuai RAB. Emulator: tab Rencana vs Realisasi menampilkan Proyeksi Rp168.000.000 untuk data contoh.

### F-05 · Satu angka total biaya · menutup A-06
- Komposisi Biaya, kartu Biaya, Laba Rugi, dan kolom "Total Biaya" BEP memakai **satu** query/kalkulator: total `CASH_EXPENSE` per kategori (setelah F-03, itu identik dengan beban operasional). Di mode Proyeksi, baris RAB ber-peran Aset tidak masuk biaya operasional.
- Donut menampilkan maksimal 5 kategori + "Lainnya" agar jumlah irisan = total.
- **Uji**: unit test bahwa jumlah irisan donut = `summary.operatingExpenseRupiah` untuk buku Realisasi dan Proyeksi contoh.

### F-06 · Metode BEP benar & satu set parameter · menutup A-07, A-14
- **Biaya Tetap (FC) wajib diisi eksplisit, per bulan.** Fallback ke total beban kumulatif dihapus. Tambahkan tombol bantu "Hitung dari rata-rata biaya bulanan selain Bahan Baku & Stok" yang mengisi kolom (bisa diedit), bukan diam-diam.
- **Satu set parameter BEP per unit usaha** (disimpan di buku Proyeksi). Tab Kelayakan di mode Realisasi memakai parameter yang sama dan menampilkan **capaian aktual** (omzet/volume bulan berjalan vs BEP), bukan menghitung ulang dengan parameter terpisah. Toggle mode tidak lagi mengubah parameter.
- **Validasi lunak**: peringatan jika harga jual > 20× biaya variabel, harga ≤ biaya variabel (sudah ada), atau FC = 0.
- Satukan `calculateCvp` dan logika BEP di `calculate` (SYS-6). B/C ratio tetap ditampilkan (permintaan akademik) dengan teks "= R/C − 1".
- **Uji**: `BusinessAnalysisTest`: tanpa FC → BEP null + alasan; contoh Kedai Kopi tetap BEP 400 cup / Rp4.000.000; FC tidak pernah memakai total beban kumulatif.

### F-07 · Format angka Indonesia untuk volume · menutup A-08
- `quantityFormat` memakai locale bahasa aplikasi (`id-ID` untuk Indonesia), sama seperti `toRupiah`. Tanggal juga mengikuti bahasa aplikasi (A-31).
- **Uji**: unit test `toDisplayQuantity(BigDecimal("1363")) == "1.363"` dan `BigDecimal("2.5") == "2,5"` untuk locale id.

### F-08 · Validasi parameter dari impor · menutup A-09 (sisa) · butuh F-01, F-06
- Identitas usaha tidak lagi diambil dari file (F-01: impor selalu ke unit terpilih). Default "Usaha Tani" / "Lahan Utama" dihapus dari `AppViewModel.commitImport`.
- Nilai dari lembar `Parameter BEP` melewati validasi lunak F-06. Nilai mencurigakan ditampilkan sebagai peringatan di pratinjau dan perlu dikonfirmasi sebelum disimpan.
- **Uji**: unit test validasi; impor dengan harga jual 100 jt dan biaya variabel 2 jt → peringatan muncul.

---

## Fase 3 — Input yang hilang

### F-09 · Modal, prive, aset lewat kategori · menutup A-04, K-2
- **Peran kategori** (final): `REVENUE` (Pemasukan), `EXPENSE` (Pengeluaran), `CAPITAL_IN` (Setoran modal), `DRAWING` (Ambil pribadi), `ASSET` (Beli aset). Migrasi string peran: `EQUITY` → `CAPITAL_IN`.
- **Pemetaan otomatis** di `record()`:

  | Tombol | Peran kategori | Tipe yang disimpan |
  |---|---|---|
  | Uang masuk | Pemasukan | `CASH_INCOME` |
  | Uang masuk | Setoran modal | `OWNER_CAPITAL` |
  | Uang keluar | Pengeluaran | `CASH_EXPENSE` |
  | Uang keluar | Ambil pribadi | `OWNER_DRAWING` |
  | Uang keluar | Beli aset | `ASSET_PURCHASE_CASH` |

- Pemilih kategori dikelompokkan: "Operasional" di atas, lalu "Modal & Aset" dengan teks bantu satu baris ("tidak dihitung sebagai laba/rugi").
- **Uji**: unit test pemetaan (5 kombinasi); androidTest: catat "Setoran Modal Pemilik" Rp10 jt → kas +10 jt, laba tetap, arus pembiayaan +10 jt.

### F-10 · Kategori dasar + kustom · menutup A-17, A-18, K-5 · butuh F-09
- **Default untuk usaha baru** (11 kategori, `BusinessTemplate.kt`):
  - Pemasukan: *Penjualan & Jasa*, *Pendapatan Lain*
  - Pengeluaran: *Bahan Baku & Stok*, *Gaji & Upah*, *Sewa Tempat*, *Listrik, Air & Internet*, *Transportasi*, *Biaya Lain-lain*
  - Modal & Aset: *Setoran Modal Pemilik* (masuk), *Ambil untuk Pribadi (Prive)* (keluar), *Beli Peralatan & Aset* (keluar)
- Usaha lama: kategori yang ada tidak diubah (data pengguna); hanya yang tidak terpakai dan non-kas diarsipkan (F-02).
- **Impor**: aturan kategori mengikuti F-01 (nama ternormalisasi, tanpa fuzzy; yang belum ada dibuat sebagai kategori kustom dan ditampilkan di pratinjau).
- **Kelola kategori**: dikelompokkan per peran, bisa **ganti nama** dan **gabung** (pindahkan semua transaksi/RAB ke kategori lain lalu arsipkan), arsip dengan konfirmasi, pilihan peran hanya 5 di atas.
- **Uji**: unit test template (11 item, peran benar); androidTest gabung kategori → transaksi pindah, total tidak berubah.

### F-11 · Editor RAB di mode Proyeksi · menutup A-05, K-4
- Saat mode **Proyeksi**, tab Catatan menjadi **"Rencana Anggaran"**: daftar item RAB per kategori (subtotal per kategori, total pemasukan target, total biaya, laba rencana), tombol tambah, ketuk untuk ubah, arsip.
- Sheet item: kategori, nama item, jumlah + satuan + harga satuan (total otomatis, atau isi total langsung), bulan rencana. Memakai `createBudgetItem/updateBudgetItem/archiveBudgetItem` yang sudah ada.
- FAB di mode Proyeksi = "Tambah item rencana". Transaksi proyeksi lama hasil impor tetap terbaca dan dihitung sesuai aturan F-04.
- **Uji**: androidTest buat 3 item RAB → `planSummary` sesuai; emulator: tab perbandingan berubah setelah item ditambah.

### F-12 · Stok tersambung ke kas (opsional) · menutup A-10, K-3 · butuh F-09
- **Stok Masuk**: centang "Catat juga sebagai uang keluar" (default aktif), nominal = jumlah × harga beli (bisa diedit), kategori default *Bahan Baku & Stok*. Satu transaksi Room membuat `CASH_EXPENSE` + `stock_movements` dengan `referenceEventId` (kolom sudah ada).
- **Uang keluar** berkategori Pengeluaran: baris opsional "Tambah ke stok: [barang] [jumlah]" bila usaha punya barang.
- **Uang masuk** berkategori Pemasukan: baris opsional "Kurangi stok: [barang] [jumlah]" (stok keluar karena terjual).
- **Pembatalan**: membalik/menghapus transaksi yang tertaut menawarkan "Batalkan juga mutasi stok" dan mencatat mutasi kebalikannya (tidak mengedit riwayat).
- Perbarui DEC-010 butir 4 (kini opsional tertaut, tidak lagi murni inventory-only).
- **Uji**: unit/androidTest: stok masuk 10 kg @Rp15.000 dengan centang → 1 transaksi Rp150.000 + stok +10; batal → stok kembali, kas kembali.

---

## Fase 4 — Sederhanakan & periode

### F-13 · Pemilih periode · menutup A-11
- Chip periode di Beranda dan Laporan: **Bulan ini** (default) · Bulan lalu · Pilih bulan · Semua. Pilihan disimpan per sesi.
- **Saldo kas selalu total sampai hari ini** (uang di tangan). Pemasukan, pengeluaran, laba, komposisi, dan arus kas mengikuti periode.
- `observePostingRows` / kalkulator menerima rentang tanggal (posting perlu `dateEpochDay` lewat join event).
- **Uji**: unit test kalkulator dengan rentang; emulator: data contoh "Bulan ini" (Okt 2026) → Rp0 dengan pesan kosong yang jelas, "Pilih bulan: April 2026" → angka April.

### F-14 · Beranda = "kondisi sekarang" · menutup A-12, A-19, A-25, A-27 · butuh F-13
- Susunan: Kas sekarang (hero) → **Bulan ini**: Masuk · Keluar · Laba (satu kartu, tiga angka) → Capaian vs rencana bulan ini (bila ada RAB) → Stok menipis (hanya bila ada) → Notifikasi belum ditinjau (dengan tombol "Tandai sudah dibaca").
- Hapus grid 4 kartu yang menduplikasi Laporan. Hapus chip "Mode Aktual/Rencana"; judul hero cukup "Kas".
- Pesan tren memakai bulan terakhir yang benar-benar ada aktivitasnya.
- **Uji**: emulator screenshot sebelum/sesudah; unit test pemilihan "bulan terakhir beraktivitas".

### F-15 · Laporan = rincian · menutup A-16, A-24
- Laba Rugi per kategori (Pendapatan per kategori, Biaya per kategori, Laba), mengikuti periode. Donut komposisi pindah ke sini.
- Label diseragamkan: "Biaya Operasional" di mana pun (bukan "Total Biaya Produksi"); "Omzet" tanpa kata "Proyeksi" di mode Realisasi; "Margin per unit" dengan rumus singkat.
- **Uji**: unit test jumlah baris kategori = total; emulator.

### F-16 · Satu cara membatalkan · menutup A-15
- Baris transaksi hanya punya **satu** tindakan terlihat: "Batalkan" (membuat pembalik). Hapus permanen dipindah ke menu ⋮ dengan konfirmasi.
- Pasangan asli + pembalik **disembunyikan dari daftar** secara default, dengan filter "Tampilkan yang dibatalkan". Yang asli tampil dicoret, pembalik tidak tampil sebagai baris sendiri.
- **Uji**: androidTest/Compose test daftar: setelah batalkan, baris hilang dari "Semua", muncul di filter "Dibatalkan".

### F-17 · Toggle mode hanya di layar yang memakainya · menutup A-13
- Sembunyikan toggle Proyeksi/Realisasi di tab Stok dan di tab Rencana vs Realisasi. Di Catatan, toggle menukar Transaksi ↔ Rencana Anggaran (F-11).
- **Uji**: Compose test keberadaan toggle per destinasi.

### F-18 · Satu pintu per fungsi · menutup A-20, A-28 (sebagian)
- Kelola usaha/unit hanya dari pemilih di header. Kartunya dihapus dari Berkas & Data.
- Cadangan lokal dan Google Drive disatukan di **Berkas & Data → Cadangan**. Pengaturan hanya menautkan ke sana.
- Tur hanya dari tombol "?" dan satu tombol di Pengaturan. Hapus "Mulai Tur" ganda.
- **Uji**: emulator walkthrough.

### F-19 · Form catat transaksi konsisten · menutup A-21
- Pindahkan ke pola `MankeuSheet` (aksi Batal/Simpan selalu terlihat, Back dan geser menutup). Hapus judul ganda: toggle cukup, deskripsi satu baris.
- **Uji**: Compose test Back menutup sheet; emulator.

### F-20 · Notifikasi impor bisa ditinjau · menutup A-19, A-26
- Setter `reviewed` di DAO + tombol "Tandai sudah dibaca". Beranda hanya menampilkan yang belum ditinjau; semua tetap ada di Berkas & Data → Riwayat impor.
- Teks notifikasi mengikuti navigasi terbaru (tanpa "Kelayakan dan Modal", tanpa pinjaman/bunga).
- **Uji**: androidTest tandai → hilang dari Beranda, tetap di riwayat.

---

## Fase 5 — Bahasa & konsistensi

### F-21 · Istilah tunggal "Unit usaha" · menutup A-22
- Glosarium: **Usaha** (bisnis) → **Unit usaha** (cabang/outlet/proyek). Ganti semua "Lahan", "Cabang / Unit", "proyek" di string, pesan error repository, dan dokumen.
- Tambahkan istilah ke `docs/DOMAIN_MODEL.md` (bagian glosarium).

### F-22 · Hilangkan jargon · menutup A-23

| Sekarang | Menjadi |
|---|---|
| Riwayat pengambilan | Riwayat impor |
| Balik batch | Batalkan impor ini |
| COMMITTED / REVERSED | Tersimpan / Dibatalkan |
| 101 catatan • Rp666.780.000 | 101 catatan diimpor |
| 58 catatan pada unit usaha dan buku yang aktif | 58 transaksi |
| Mode Aktual / Mode Rencana | (dihapus, F-14) |

### F-23 · Header & Pengaturan · menutup A-28, A-29
- Ikon header diberi label pendek/tooltip ("Data", "Pengaturan") atau "📁" dipindah ke dalam Pengaturan, sehingga header berisi: pemilih usaha · ? · ⚙.
- Urutan Pengaturan: Tampilan → Bahasa → Getar & Suara → Panduan → Akun & cadangan cloud (opsional) → Kontak.

### F-24 · Dokumentasi & tur Stok · menutup A-30
- Langkah tur Stok (kartu ringkasan, tambah barang, ketuk barang).
- Perbarui `docs/diagrams/02,03,05,09` dan `preview.html` ke 4 tab, editor RAB, dan alur stok↔kas.

---

## Fase 6 — Bersih-bersih sistem

### F-25 · Potong kode mati · menutup SYS-2, SYS-3, SYS-6, SYS-7, SYS-8, SYS-11
- Hapus 96 string tak terpakai (id + en), `ReportCalculator` (pindahkan test ke `PostingReportCalculatorTest`), dependensi `navigation-compose`, `GetStockSummaryUseCase`, `StockCalculation.calculateStockLevel`, `BusinessWords.eventLabel`, `TransactionCalculation.isConsistent`. `StockDao.observeMovements/getMovements/archiveStockItem` hanya dipertahankan jika riwayat mutasi dan arsip barang ditambahkan ke tab Stok. Usulan: tambahkan riwayat mutasi per barang sebagai bagian dari F-12.
- **Uji**: `assembleRelease` + lint tanpa `UnusedResources` baru; seluruh test hijau.

---

## 2. Definisi selesai (seluruh rencana)

1. `./gradlew testDebugUnitTest`, `compileDebugAndroidTestSources`, `assembleDebug`, `assembleRelease` hijau. *(Di mesin ini jalankan dengan PATH tanpa tanda kutip; lihat `logs/ACTIVITY_LOG.md` 20 Okt.)*
2. `am instrument` untuk `LedgerIntegrityTest`, `ImportCommitTest`, dan `MigrationTest` baru lolos di emulator.
3. Walkthrough emulator dengan data contoh, dicatat dengan screenshot:
   - Rencana vs Realisasi menampilkan angka RAB.
   - Total biaya identik di semua tempat.
   - BEP butuh FC eksplisit dan tampil "1.363".
   - Setoran modal tidak mengubah laba.
   - Stok masuk dengan centang membuat satu pengeluaran.
   - "Bulan ini" default.
   - Tidak ada kata pinjaman/utang/piutang/bunga di UI.
4. `grep` istilah non-kas di `app/src/main` bersih (kecuali komentar sejarah migrasi).
5. Log: `ACTIVITY_LOG`, `CHANGELOG` (Unreleased), `DECISION_LOG` (DEC-011 diperbarui status per fase), `docs/DATABASE_MIGRATIONS.md` (skema 8).

## 3. Risiko & mitigasi

| Risiko | Mitigasi |
|---|---|
| Migrasi 7→8 menghapus transaksi non-kas pengguna lama | Ringkasan per tipe disimpan sebagai notifikasi persisten; pengguna yang masih butuh data lama bisa memulihkan cadangan ke versi sebelumnya. Cantumkan di catatan rilis. |
| Kas hasil impor lebih kecil dari workbook (pelunasan piutang diabaikan, K-1) | Notifikasi impor menyebut jumlah dan total yang diabaikan secara eksplisit. |
| Perubahan peran kategori (`EQUITY` → `CAPITAL_IN`) mematahkan cadangan lama | Pemetaan peran dijalankan di migrasi dan setelah restore (fungsi pembersihan yang sama dengan F-02). |
| Filter periode mengubah angka yang sudah dikenal pengguna/dosen | Default "Bulan ini" ditampilkan jelas di chip; "Semua" tetap satu ketukan. |
| Lingkup besar untuk satu rilis | Fase 1–3 = rilis v0.1.0 (kebenaran angka + input dasar); Fase 4–6 = v0.1.1. |

## 4. Peta temuan → fix

| Temuan | Fix | Temuan | Fix |
|---|---|---|---|
| A-01 | F-04 | A-17 | F-10 |
| A-02 | F-01, F-02 | A-18 | F-10 |
| A-03 | F-02, F-03 | A-19 | F-14, F-20 |
| A-04 | F-09 | A-20 | F-18 |
| A-05 | F-11 | A-21 | F-19 |
| A-06 | F-05 | A-22 | F-21 |
| A-07 | F-06 | A-23 | F-22 |
| A-08 | F-07 | A-24 | F-15 |
| A-09 | F-08 | A-25 | F-14 |
| A-10 | F-12 | A-26 | F-20 |
| A-11 | F-13 | A-27 | F-14 |
| A-12 | F-14 | A-28 | F-18, F-23 |
| A-13 | F-17 | A-29 | F-23 |
| A-14 | F-06 | A-30 | F-24 |
| A-15 | F-16 | A-31 | F-07 |
| A-16 | F-15 | SYS-1…12 | F-02, F-03, F-06, F-25 |
