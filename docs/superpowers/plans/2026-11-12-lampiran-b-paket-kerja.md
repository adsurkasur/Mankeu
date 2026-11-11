# Lampiran B: paket kerja (2026-11-12)

Induk: `2026-11-12-rencana-induk-mvp.md`. Spek layar: Lampiran A. Tes: Lampiran C. Prompt: Lampiran D.

## Cara membaca satu paket

- **Butuh:** paket atau keputusan (K-n, tabel 4.16 rencana induk) yang harus selesai dulu. Keputusan belum dijawab
  berarti paket tidak dikerjakan.
- **Berkas:** yang disentuh. Path relatif terhadap `app/src/main/java/com/mankeu/` kecuali ditulis lengkap. Rentang
  baris berlaku di HEAD `7fe55ee` dan bisa bergeser; cari nama fungsinya.
- **Tes dulu:** ditulis dan terlihat gagal sebelum kode. Nama tes mengikat.
- **Selesai bila:** semua butir bisa dicek orang lain.
- **Bukti nyata:** dilakukan di emulator atau dengan membaca hasil, dan ditulis di laporan paket. Tes hijau saja
  belum memenuhi.
- **Titik simpan:** tempat aman untuk commit bila sesi hampir habis.

Aturan tetap semua paket (tidak diulang di tiap paket): Lampiran D §1.

Paket review (bertanda "hanya baca") dikerjakan **agen atau sesi lain** dari yang mengerjakan paket yang ditinjau.
Reviewer menulis `docs/superpowers/reports/PK-x.y-review.md` dengan temuan Critical / Important / Minor, masing-masing
`berkas:baris`, bukti, dan perbaikan konkret. Reviewer tidak mengubah kode.

---

## Tahap 0: pengaman

### PK-0.1 Tes manifest (K)

- **Tujuan:** kehilangan izin INTERNET tidak bisa lolos lagi.
- **Berkas:** baru `app/src/test/java/com/mankeu/ManifestTest.kt`.
- **Tes dulu:** `manifest declares INTERNET`: membaca `src/main/AndroidManifest.xml` (pola baca berkas seperti
  `presentation/ScreenLanguageTest.kt`) dan menegaskan ada `android.permission.INTERNET`. Buktikan gagal dengan
  menghapus baris izin sementara, lalu kembalikan.
- **Selesai bila:** tes hijau di branch; terlihat merah saat baris dihapus (tulis keluarannya di laporan).
- **Bukti nyata:** `./gradlew.bat :app:processDebugMainManifest`, lalu cari `INTERNET` di
  `app/build/intermediates/merged_manifests/debug/**/AndroidManifest.xml`.
- **Risiko:** tes membaca manifest sumber, bukan hasil gabungan. Cukup, karena penyebab dulu adalah izin yang
  hanya datang dari pustaka.

### PK-0.2 Hotfix INTERNET ke `main` (K) · Butuh K-15a = A, PK-0.1

- **Tujuan:** build dari `main` bisa cadangan dan sinkron Drive.
- **Langkah:** dari `main` buat branch `hotfix-internet`; tambahkan baris izin ke manifest dan salin
  `ManifestTest.kt`; jangan cherry-pick `e96144c` (commit itu juga membawa dokumen). Jalankan unit test dan
  `assembleDebug`. Gabung fast-forward ke `main`, push, naikkan pointer superproject. Kembali ke
  `fase2-nota-piutang` dan merge `main` ke dalamnya (konflik manifest: pertahankan satu baris izin).
- **Selesai bila:** `git show main:app/src/main/AndroidManifest.xml` memuat INTERNET; `ManifestTest` ada di `main`.
- **Bukti nyata:** manifest gabungan debug dari `main` memuat INTERNET.
- **Risiko:** masuk Google dan token Drive tetap [belum] sampai PK-5.4.

### PK-0.3 Review independen Paket B (hanya baca)

- **Bahan:** `git diff 337dde4..b217794`, `docs/superpowers/reports/B-laporan.md`, DEC-031,
  `presentation/UnifiedLedger.kt`, `UnifiedLedgerTest.kt`, `CatatanUnifiedLedgerTest.kt`.
- **Wajib diperiksa:**
  1. Total harian sama dengan uang nyata hari itu untuk: nota lunas hari yang sama; nota dengan uang muka lalu
     pelunasan hari lain; dua pembayaran pada hari yang sama dengan nota; pembayaran bertanggal sama tetapi nota
     diubah tanggalnya; nota dihapus sesudah dibayar.
  2. Tidak ada pembayaran yang hilang dari semua filter (diserap, tetapi notanya tersaring keluar).
  3. Jumlah total harian seluruh daftar sama dengan `MonthlySummary.totalCashInRupiah` dan `totalCashOutRupiah` bulan itu (DEC-033, 2026-11-12: seluruh kas nyata; angka operasional lama tetap terpisah).
  4. Batas muat (`RECENT_EVENT_LIMIT`) tidak memotong separuh hari.
- **Selesai bila:** `PK-0.3-review.md` tertulis; tiap skenario di butir 1 punya putusan benar atau salah dengan bukti.

### PK-0.4 Review independen Paket N dan ukur ulang jank (hanya baca, emulator)

- **Bahan:** `git diff 95d0062..337dde4`, `docs/superpowers/reports/N-laporan.md`, `docs/audit/2026-11-09_BUG_BASH.md`.
- **Langkah:** ukur `dumpsys gfxinfo com.mankeu` (reset, aksi, baca) untuk: buka sheet tambah, ketik di form jual,
  gulir Catatan 200 baris, buka Beranda. Tiga kali tiap aksi, catat persen janky dan persentil 90.
- **Selesai bila:** `PK-0.4-review.md` memuat tabel angka dan penyebab yang ditelusuri (bukan ditebak) untuk aksi
  di atas 15 % janky; daftar pertanyaan untuk pemilik tentang jitter di HP-nya.
- **Catatan jujur:** angka emulator bukan angka HP murah. Putusan akhir di PK-5.4.

### PK-0.5 Perbaikan temuan PK-0.3 dan PK-0.4 (S) · Butuh keduanya

- **Langkah:** tiap temuan Critical dan Important: tes merah dulu, perbaiki di akar, tes hijau. Minor yang murah
  ikut. Minor yang ditinggal ditulis di register rencana induk §6 dengan alasan.
- **Selesai bila:** tiap temuan berstatus beres atau terdaftar; suite penuh hijau; angka jank sebelum dan sesudah
  tercatat untuk perbaikan jank.

### PK-0.6 Kata terlarang dengan daftar dasar (K)

- **Tujuan:** kata akuntansi tidak bertambah, dan jumlahnya hanya boleh turun.
- **Berkas:** `app/src/test/java/com/mankeu/presentation/ScreenLanguageTest.kt`; baru
  `app/src/test/resources/kata_terlarang_dasar.txt`.
- **Tes dulu:** `layar tidak memuat kata terlarang di luar daftar dasar`. Daftar kata dan pengecualian: Lampiran C
  §1.1. Daftar dasar berisi `nama_string|kata` untuk semua pelanggaran yang ada hari ini (dibuat sekali dengan
  menjalankan tes dan menyalin keluarannya). Tes gagal bila ada pelanggaran di luar daftar dasar, **dan** bila ada
  baris daftar dasar yang sudah tidak melanggar (memaksa daftar menyusut).
- **Selesai bila:** tes hijau; menambah string berisi "piutang" membuat tes merah (buktikan, lalu kembalikan).
- **Risiko:** string laporan dan PDF memang boleh formal. Pengecualian lewat awalan nama string, bukan lewat daftar
  dasar.

### PK-0.7 Tombol mati wajib beralasan (S)

- **Tujuan:** tidak ada tombol Simpan abu-abu tanpa penjelasan (cek-lis 5, U4).
- **Berkas:** `presentation/components/form/FullPageForm.kt` (parameter `saveEnabled`), `presentation/components/MankeuSheet.kt`,
  semua pemanggilnya (cari `saveEnabled =` dan `confirmEnabled`).
- **Tes dulu:** di `FormComponentsTest`: form dengan `saveBlockedReason = "Isi harganya dulu."` menampilkan teks
  itu dengan `testTag("save_blocked_reason")` dan tombol mati; dengan `null` tombol hidup dan teks tidak ada.
- **Langkah:** ganti `saveEnabled: Boolean` dengan `saveBlockedReason: String?`. Parameter lama **dihapus**, jadi
  setiap pemanggil harus memberi alasan atau gagal kompilasi. Untuk form yang ada sekarang tulis alasan menurut
  isian pertama yang kurang. Lakukan hal yang sama untuk tombol konfirmasi `MankeuSheet`.
- **Selesai bila:** tidak ada lagi `enabled = false` tanpa alasan pada tombol utama form dan sheet
  (`grep -n "saveEnabled\|enabled = " presentation/screens` ditinjau satu per satu, hasilnya di laporan).
- **Bukti nyata:** emulator, barang baru, kosongkan hasil per batch: alasan tampil di atas Simpan. Satu screenshot.
- **Titik simpan:** sesudah komponen dan tesnya; lalu per layar.

### PK-0.8 Alat hitung ketukan dan kerangka "tak ada jalan buntu" (K)

- **Berkas:** baru `app/src/androidTest/java/com/mankeu/usability/TapCounter.kt`, `NoDeadEndTest.kt`.
- **Isi:** `TapCounter` membungkus `performClick` dan `performTextInput` dan menghitung keduanya terpisah.
  `NoDeadEndTest` membuka tiap tujuan utama pada data kosong dan menegaskan ada node `empty_action` yang bisa
  diklik. Daftar tujuan: Lampiran C §1.3. Tujuan yang belum lolos hari ini diberi `@Ignore("PK-2.x")` dengan
  nomor paket yang akan memperbaikinya.
- **Selesai bila:** tes berjalan; jumlah `@Ignore` dan alasannya tercatat di laporan.

---

## Tahap 1: model

### PK-1.1 Domain: jenis barang dan `unitCost` (K)

- **Berkas:** `domain/ProductCosting.kt`; `app/src/test/java/com/mankeu/domain/ProductCostingTest.kt`.
- **Tes dulu** (`ProductCostingTest`):
  - resep ada: `unitCost` = hasil `cost()` sekarang (golden Rp6.000 tetap);
  - susut 20 dari 400: 2.400.000 ÷ 380 = Rp6.316;
  - TRADED tanpa resep, harga beli terakhir Rp13.000 per kg: Rp13.000, `known = true`;
  - tanpa resep, tanpa belanja, modal ketikan Rp5.000: Rp5.000;
  - tidak ada apa pun: `known = false`, biaya 0;
  - susut sama dengan atau melebihi hasil: ditolak dengan pesan.
- **Langkah:** tambah `enum class ItemKind { MADE, TRADED, SERVICE }`; `ProductSpec` mendapat `kind`,
  `batchLoss: BigDecimal`, `manualCostRupiah: Long?`; fungsi `unitCost(spec, materialPrices, lastPurchaseCostRupiah: Long?): UnitCostResult(costRupiah, known, source)`
  dengan urutan di rencana induk §3.1. `cost()` memakai hasil bersih.
- **Selesai bila:** tes baru hijau; seluruh tes domain lama hijau tanpa diubah.

### PK-1.2 Schema 14 di tempat (S) · Butuh K-1, K-2, K-3, K-4, PK-1.1

- **Tujuan:** semua kolom model baru masuk sebelum schema 14 dirilis.
- **Kolom** (yang bertanda keputusan hanya bila jawabannya meminta):

| Tabel | Kolom | Bawaan | Catatan |
|---|---|---|---|
| `businesses` | `kind` TEXT NULL | NULL | MAKER, TRADER, SERVICE, FARM, MIXED |
| `businesses` | `profitBasis` TEXT NOT NULL | `'ITEM_COST'` | K-1 = A. Usaha baru dibuat `'SIMPLE'` oleh kode, usaha lama tetap ITEM_COST |
| `products` | `kind` TEXT NOT NULL | `'MADE'` | |
| `products` | `tracksStock` INTEGER NOT NULL | 1 | |
| `products` | `manualCostRupiah` INTEGER NULL | NULL | |
| `products` | `batchLossText` TEXT NOT NULL | `'0'` | |
| `sale_lines` | `productId` menjadi NULL | | Tabel dibuat di migrasi 13 ke 14, jadi cukup ubah `CREATE TABLE` |
| `sale_lines` | `name` TEXT NOT NULL | `''` | Nama baris bebas; untuk baris berproduk nama produk saat itu |
| `sale_lines` | `costKnown` INTEGER NOT NULL | 1 | |
| `financial_events` | `contentText` TEXT NULL | NULL | Isi satuan beli saat belanja barang dagangan (snapshot) |
| `financial_events` | `payMethod` TEXT NULL | NULL | K-2 = B |
| `sales` | `orderStatus` TEXT NULL, `promisedDateEpochDay` INTEGER NULL | NULL | K-3 = B |
| `stock_items` | `priceQuantityText` TEXT NOT NULL | `'1'` | K-4 = A |

- **Berkas:** `data/Entities.kt`, `domain/StockManagement.kt` (entitas stok), `data/Migration13To14.kt`,
  `app/schemas/**/14.json` (dibuat ulang lewat build, jangan disunting tangan), `backup/BackupUpgrade.kt`,
  `sync/SyncSchema.kt` dan `AppDatabase.DATA_TABLES` bila daftar kolom dituliskan, `data/LegacySaleShape.kt` (baris
  hasil konversi mengisi `name` dan `costKnown`).
- **Tes dulu:** `Migration13To14Test`: basis data schema 13 berisi data golden naik ke 14 dan `createSql` sama
  dengan `14.json`; kolom baru berisi bawaan; usaha lama `profitBasis = ITEM_COST`. `BackupUpgradeTest`: cadangan
  schema 13 dan cadangan schema 14 bentuk lama (tanpa kolom baru, dari build branch sebelum paket ini) terbaca.
  `SyncEngineTest`: baris dengan kolom baru terkirim dan diterima.
- **Selesai bila:** tiga tes di atas hijau; suite penuh hijau; laporan memuat daftar kolom akhir.
- **Bukti nyata:** pasang APK branch lama di emulator, isi data keripik, pasang APK baru di atasnya (`adb install -r`):
  app terbuka, angka Beranda sama. Catat angkanya.
- **Risiko:** HP pemilik mungkin berisi basis data schema 14 bentuk lama dari build branch. Karena schema 14 belum
  dirilis, itu diterima: tulis di laporan bahwa build debug lama perlu hapus data atau pulih dari cadangan.
- **Titik simpan:** sesudah entitas dan migrasi hijau; lalu backup; lalu sinkron.

### PK-1.3 Baris nota bebas (S) · Butuh PK-1.2

- **Berkas:** `data/SalesRecord.kt` (`SaleLineDraft`), `data/FinanceRepository.kt` (`saveSale`, `noteLine`,
  `writeSale`, kira-kira baris 1092 sampai 1260; pembacaan stok dari `sale_lines`; `profitInputsOf`),
  `data/FinanceDao.kt`, `domain/SalesMath.kt` (`SaleLineInput.productId`).
- **Tes dulu** (`SaleNoteFlowTest` atau tes repository baru `FreeLineSaleTest`):
  - nota satu baris bebas "Jahit seragam" 1 × Rp1.500.000, dibayar Rp750.000 atas nama Bu Ani: tersimpan, belum
    bayar Rp750.000, stok produk mana pun tidak berubah;
  - nota campuran: produk Oli 1 × Rp55.000 dan baris bebas "Jasa ganti oli" Rp20.000: total Rp75.000, modal hanya
    dari baris Oli;
  - baris bebas tanpa nama ditolak dengan pesan;
  - golden Oktober keripik (`SalesFlowTest`, `MonthlySummaryTest`) tidak berubah.
- **Langkah:** `SaleLineDraft.productId` menjadi `String?` dengan `name: String`. Baris bebas: `unitName` boleh
  kosong, `contentText = "1"`, `hppRupiah = 0`, `costKnown = 1`. Baris berproduk: modal dari `unitCost`; bila
  `known = false` maka `hppRupiah = 0`, `costKnown = 0`. Cari semua pemakai `SaleLineEntity.productId` dan
  `SaleLineDraft.productId` (`grep -rn "productId" data presentation exporter importer sync`) dan tangani nilai
  kosong; tulis daftarnya di laporan.
- **Selesai bila:** tes hijau; daftar pemakai `productId` di laporan, masing-masing dengan cara penanganannya.
- **Bukti nyata:** belum ada layar. Bukti lewat tes repository dan pembacaan diff ekspor: baris bebas tidak boleh
  membuat ekspor crash (jalankan `ImportCommitTest`).
- **Risiko:** X5. Ekspor Excel baris bebas baru lengkap di PK-4.1; sampai saat itu ekspor boleh melewati baris
  bebas **dengan pemberitahuan**, tidak boleh diam-diam.

### PK-1.4 Stok menurut jenis; belanja barang dagangan (S) · Butuh PK-1.2, K-8

- **Berkas:** `data/FinanceRepository.kt` (`products`, `stockItems` baris kira-kira 934 sampai 950 sebagai pola,
  `recordEvent`, `recordProduction`, `saveProduct`), `data/ProductRecord.kt` (`ProductDraft`), `data/FinanceDao.kt`,
  `domain/ProductCosting.kt` (`ProductMovementType` mendapat `PURCHASE`).
- **Tes dulu** (`ProductStockFlowTest` atau baru `TradedGoodsTest`):
  - Beras, TRADED, satuan dasar kg, satuan "karung" isi 25. Beli 2 karung total Rp650.000: sisa 50 kg, modal
    Rp13.000 per kg. Jual 3 kg @ Rp15.000: sisa 47 kg, modal baris Rp39.000;
  - belanja kedua 1 karung Rp350.000: modal menjadi Rp14.000 per kg (K-8 = A); nota lama tetap Rp39.000;
  - mengubah isi karung menjadi 50 sesudahnya tidak mengubah sisa dari belanja lama (memakai `contentText`);
  - "Bajak sawah", SERVICE: jual 2 hektar, tidak ada gerakan stok, tidak ada peringatan menipis, `recordProduction`
    ditolak dengan pesan;
  - MADE dengan `tracksStock = false` (warung): jual tidak membuat sisa minus.
- **Langkah:** belanja (`CASH_EXPENSE` atau `CREDIT_PURCHASE`) dengan `productId` milik barang TRADED menambah stok
  produk sebesar jumlah × `contentText`, dibaca dari catatan seperti stok bahan (tanpa baris gerakan tersimpan).
  Belanja itu memakai jenis pengeluaran bertanda `includedInHpp`. Stok hanya dihitung bila `tracksStock`.
- **Selesai bila:** tes hijau; menghapus belanja mengembalikan sisa barang.
- **Risiko:** `financial_events.productId` dulu dipakai untuk uang masuk berproduk (data lama, F2-4). Bedakan dari
  jenis catatan; tulis aturan pembedanya di laporan.

### PK-1.5 "Simpan sebagai barang" dan nama mirip (S) · Butuh PK-1.3, K-5 = A

- **Berkas:** `data/FinanceRepository.kt` (fungsi baru `promoteLineName`, `saleNameSuggestions`), `data/FinanceDao.kt`.
- **Tes dulu** (`PromoteLineTest`):
  - dua nota baris bebas "keripik" dan "Keripik " menghasilkan satu saran "keripik" dengan harga terakhir;
  - `promoteLineName("keripik", kind = MADE, unit = "bungkus")` membuat satu barang, `tracksStock = false`, harga
    dari baris terakhir, dan kedua baris lama tertaut ke barang itu dengan modal tidak berubah;
  - memanggilnya lagi untuk "KERIPIK" tidak membuat barang kedua;
  - ajakan muncul pada pemakaian kedua, tidak pada yang pertama.
- **Langkah:** pencocokan nama memakai fungsi normalisasi yang sudah ada di repository (`normalize`). Barang hasil
  promosi tidak menghitung stok sampai pengguna menyalakannya, supaya penjualan lama tidak membuat sisa minus.
- **Selesai bila:** tes hijau; satu transaksi basis data per promosi.

### PK-1.6 Dua cara hitung untung (S) · Butuh PK-1.3, K-1 = A

- **Berkas:** `domain/ProfitCalculator.kt` (`ProfitInputs`, `MonthProfit`), `domain/MonthlySummary.kt`,
  `data/FinanceRepository.kt` (`profitInputsOf`, `createBusinessWithUnit`, fungsi baru `setProfitBasis`),
  `exporter/*` hanya bila menghitung sendiri (seharusnya tidak).
- **Tes dulu** (`ProfitCalculatorTest`, `MonthlySummaryTest`, `OneNumberTest`):
  - SIMPLE: jualan Rp850.000, belanja bahan (jenis bertanda modal) Rp500.000, listrik Rp50.000: untung Rp300.000;
  - data yang sama dengan ITEM_COST dan modal terjual Rp400.000: untung Rp400.000, selisih belanja Rp100.000;
  - ITEM_COST dengan satu baris `costKnown = 0`: `MonthProfit.salesWithoutCostCount = 1` dan jumlah rupiahnya;
  - golden Oktober keripik (ITEM_COST) tidak berubah: untung −310.000, modal 4.440.000;
  - `OneNumberTest` dijalankan untuk kedua cara.
- **Langkah:** `ProfitInputs` mendapat `basis`. Pada SIMPLE: modal terjual dianggap 0 dan semua belanja mengurangi
  untung. Penyusutan alat tetap berlaku di kedua cara. `MonthlySummary` meneruskan `basis`,
  `salesWithoutCostCount`, `salesWithoutCostRupiah`.
- **Selesai bila:** tes hijau; tidak ada layar atau eksportir yang membaca `profitBasis` langsung (cari dan
  buktikan di laporan).
- **Risiko:** X6, X7.

### PK-1.7 Skenario persona sebagai tes (S) · Butuh PK-1.3 sampai PK-1.6

- **Berkas:** baru `app/src/androidTest/java/com/mankeu/PersonaScenarioTest.kt` (tingkat repository, basis data di
  memori, tanpa UI; pola `SalesFlowTest` dan `GoldenData.kt`).
- **Isi:** sepuluh skenario dengan angka di Lampiran C §2. Bagian yang menunggu keputusan (untung per musim P5,
  status pesanan P6, potongan P8, jaminan P10) ditulis sebagai fungsi tes terpisah ber-`@Ignore` dengan nomor
  keputusannya.
- **Selesai bila:** sepuluh skenario hijau; empat fungsi tes diabaikan dengan alasan tertulis.

### PK-1.8 Review independen Tahap 1 (hanya baca) · Butuh PK-1.7

- **Bahan:** diff sejak commit sebelum PK-1.1.
- **Wajib diperiksa:** migrasi dan `14.json`; BackupUpgrade; sinkron (kolom baru, urutan induk dulu); setiap pemakai
  `productId` yang kini boleh kosong; uang tanpa floating point; pembulatan satu kali; `OneNumberTest` untuk dua
  cara; hapus nota dan hapus belanja mengembalikan stok; undo impor tidak terganggu.

### PK-1.9 Perbaikan temuan PK-1.8 (S)

Sama dengan PK-0.5. Perbaikan yang menyentuh migrasi, uang, atau sinkron mendapat tinjauan ulang singkat.

---

## Tahap 2: Pintu A

Semua paket layar: teks dari Lampiran A, lewat `Vocabulary`, dengan `testTag` yang disebut di sana.

### PK-2.1 Layar pertama (S) · Butuh K-6, PK-1.2

- **Spek:** Lampiran A §3.
- **Berkas:** baru `presentation/screens/WelcomeScreen.kt`; `presentation/MankeuApp.kt` (cabang "belum ada usaha");
  `presentation/AppViewModel.kt`; `data/FinanceRepository.kt` (`createBusinessWithUnit` menerima jenis usaha dan
  menulis `profitBasis = SIMPLE`); `res/values/strings.xml`, `res/values-en/strings.xml`.
- **Tes dulu** (baru `WelcomeScreenTest`): instal bersih menampilkan lima kartu dan satu kotak; "Mulai" tanpa
  pilihan menampilkan alasan; pilih kartu, ketik nama, "Mulai": satu usaha dengan jenis itu, Beranda tampil, tidak
  ada teks "unit" atau "cabang"; tepat 3 interaksi (`TapCounter`).
- **Selesai bila:** tes hijau; sheet bertumpuk lama tidak lagi bisa dicapai dari instal bersih (L02, R22).
- **Bukti nyata:** `adb shell pm clear com.mankeu`, buka app, lalui layar pertama. Dua screenshot.

### PK-2.2 Kosakata dan dialog keluar form (S) · Butuh K-7, PK-2.1

- **Berkas:** `presentation/BusinessWords.kt` (objek `Vocabulary`), `strings.xml` dua bahasa,
  `presentation/components/form/FullPageForm.kt` (dialog keluar).
- **Tes dulu:** `VocabularyTest` (unit): tiap kunci Lampiran A §2 punya string untuk lima jenis dan dua bahasa;
  tidak ada yang kosong. `FormComponentsTest`: dialog keluar menampilkan "Simpan" bila bisa disimpan, dan alasan
  bila belum.
- **Selesai bila:** tes hijau; `grep -n "when (.*kind" presentation/screens` kosong (percabangan kata hanya di
  `Vocabulary`).

### PK-2.3 Beranda kosong dan berisi (S) · Butuh PK-2.2, PK-1.6

- **Spek:** Lampiran A §4.
- **Berkas:** `presentation/screens/HomeScreen.kt` (`FirstSteps` baris kira-kira 365 diganti; `ProfitCard`;
  urutan kartu), `AppPreferences.kt` (kartu ajakan yang ditutup), `strings.xml`.
- **Tes dulu** (`HomeScreenTest`): data kosong menampilkan `door_sell`, `door_buy`, dua pintu Hitung, dan tidak
  menampilkan "Buat produk"; sesudah satu catatan jual, kartu untung menampilkan angka `MonthlySummary` dan baris
  cara hitung; kartu "Belum kubayar" tidak tampil tanpa data; hanya satu kartu ajakan tampil.
- **Selesai bila:** tes hijau; `NoDeadEndTest` untuk Beranda aktif kembali.
- **Bukti nyata:** emulator, mode terang dan gelap, satu screenshot masing-masing.

### PK-2.4 "Aku jual" (S) · Butuh PK-1.3, PK-2.2

- **Spek:** Lampiran A §5.
- **Berkas:** baru `presentation/screens/QuickSaleScreen.kt`; `MankeuApp.kt` (navigasi); `AppViewModel.kt`
  (`saleNameSuggestions`, simpan lewat `saveSale`); `strings.xml`. `SaleNoteScreens.kt` tetap untuk membuka dan
  mengubah nota lama.
- **Tes dulu** (baru `QuickSaleTest`): ketik nama dan harga lalu Simpan menghasilkan nota lunas satu baris bebas;
  memilih chip barang mengisi harga dan Simpan dalam 3 interaksi; "Belum" tanpa pembeli menampilkan alasan;
  mengetik nama pembeli baru membuat kontak dan tagihan; "Jualan hari ini" menyembunyikan jumlah; "Urungkan"
  menghapus nota; angka "2,5" diterima.
- **Selesai bila:** tes hijau; uang lewat `sanitizeRupiah`, jumlah lewat `parseQuantity` (tunjukkan barisnya di
  laporan).
- **Bukti nyata:** emulator: tiga penjualan (bebas, barang, belum bayar); Beranda dan Catatan menampilkannya.
- **Titik simpan:** sesudah alur lunas; lalu belum bayar; lalu baris kedua.

### PK-2.5 "Aku beli" (S) · Butuh PK-1.4, PK-2.2

- **Spek:** Lampiran A §6.
- **Berkas:** baru `presentation/screens/QuickBuyScreen.kt`; `MankeuApp.kt`; `AppViewModel.kt`; `strings.xml`.
  `MoneyForms.kt` tetap untuk mengubah catatan lama.
- **Tes dulu** (baru `QuickBuyTest`): nama dan jumlah uang lalu Simpan membuat pengeluaran; "Bahan atau barang
  jualan" memakai jenis bertanda modal; "Alat" menanyakan lama pakai dan membuat pembelian alat; memilih barang
  dagangan membuka jumlah dan menambah sisa barang; harga satuan tampil dihitung app.
- **Selesai bila:** tes hijau; untung pada cara Sederhana berkurang sebesar belanja (cek lewat `MonthlySummary`).

### PK-2.6 Siapa belum bayar (K) · Butuh PK-2.4

- **Spek:** Lampiran A §7. **Berkas:** `HomeScreen.kt` (`BillsCard`), `CustomerScreen.kt`, `SaleNoteScreens.kt`
  (`PaySheet`), `strings.xml`.
- **Tes dulu:** dari Beranda, terima bayar penuh dalam paling banyak 3 ketukan; sheet menyebut alasan bila jumlah
  melebihi sisa; kata "piutang" dan "tagihan" tidak tampil.
- **Selesai bila:** tes hijau.

### PK-2.7 Catatan kosong dan tombol tambah (K) · Butuh PK-2.4, PK-2.5

- **Spek:** Lampiran A §13. **Berkas:** `presentation/screens/EntryScreen.kt`, `presentation/components/RecordFab.kt`.
- **Tes dulu** (`CatatanUnifiedLedgerTest`): keadaan kosong punya dua `empty_action`; pilihan pertama sheet tambah
  membuka "Aku jual", bukan "Uang masuk lain"; teks "unit usaha dan buku" tidak ada.

### PK-2.8 Editor barang bertahap (S) · Butuh PK-1.4, PK-2.2

- **Spek:** Lampiran A §8.1. **Berkas:** `presentation/screens/ProductEditorScreen.kt`, `ProductsScreen.kt`,
  `StockSection.kt` (sembunyikan stok dan hasil buat menurut jenis), `strings.xml`.
- **Tes dulu** (`ProductEditorTest`): editor terbuka dengan 3 isian; nama dan harga cukup untuk Simpan; untuk
  SERVICE tidak ada teks sisa barang, hasil buat, atau bahan; "Atur lebih lanjut" menampilkan bagian menurut tabel
  jenis; barang lama (keripik dengan resep) terbuka dan tersimpan tanpa kehilangan resep, satuan, dan rencana.
- **Selesai bila:** tes hijau; `theDailySaleIsFiveTapsWithNoTyping` dan tes editor lama tetap hijau atau diganti
  dengan alasan tertulis.
- **Risiko:** kehilangan isian yang tidak ditampilkan saat menyimpan. Tes terakhir di atas menjaganya.

### PK-2.9 Satuan milik usaha (S) · Butuh PK-2.8

- **Spek:** Lampiran A §8.2. **Berkas:** baru `presentation/components/form/UnitPicker.kt` (di atas `PickerPage`);
  `ProductEditorScreen.kt` (`COMMON_BASE_UNITS`), `StockSection.kt` (`COMMON_UNITS`), layar jual dan beli;
  `FinanceRepository.kt` (fungsi `unitsInUse(businessId)` yang membaca dari produk, satuan jual, bahan, catatan).
- **Tes dulu:** satuan yang diketik di satu barang muncul sebagai pilihan di barang berikutnya; "Karung" memilih
  "karung" yang ada; kotak pencarian kosong saat dibuka (U5).
- **Selesai bila:** tes hijau; dua daftar tetap di kode hanya menjadi saran awal `Vocabulary`.

### PK-2.10 Isi modal barang dan pindah cara hitung (S) · Butuh PK-1.6, PK-2.3

- **Spek:** Lampiran A §4.4 dan §10. **Berkas:** `HomeScreen.kt`, baru `presentation/screens/ItemCostScreen.kt`
  (daftar barang tanpa modal, satu angka per barang), `ManageScopeScreen.kt` (pengaturan cara hitung),
  `FinanceRepository.kt` (`setProfitBasis`, `backfillLineCost`).
- **Tes dulu:** mengisi modal Rp5.000 menawarkan "Pakai juga untuk 3 penjualan sebelumnya"; menerima mengisi modal
  baris itu dan menandai notanya perkiraan; dialog pindah cara menampilkan dua angka yang sama dengan
  `MonthlySummary` kedua cara; sesudah pindah, Beranda dan Laporan menampilkan angka baru yang sama.

### PK-2.11 Tes perjalanan pertama (K) · Butuh PK-2.3 sampai PK-2.7

- **Berkas:** baru `app/src/androidTest/java/com/mankeu/usability/FirstSaleJourneyTest.kt`.
- **Isi:** Lampiran C §1.4. Instal bersih sampai penjualan pertama: paling banyak 8 ketukan dan 3 isian diketik.
  Lalu "untungku bulan ini" terlihat di Beranda tanpa ketukan, dan "siapa belum bayar" dalam 1 ketukan.
- **Selesai bila:** tes hijau; semua `@Ignore` di `NoDeadEndTest` hilang.

### PK-2.12 Review Tahap 2 dan uji tangan (hanya baca, emulator) · Butuh PK-2.11

- **Langkah:** ulangi 14 skenario U1 sampai U14 audit pengguna awam di instal bersih, untuk tiga jenis usaha
  (MAKER, TRADER, SERVICE). Tiap skenario: lolos atau gagal terhadap 12 butir cek-lis, satu screenshot.
- **Selesai bila:** `docs/audit/2026-11-XX_UJI_ULANG_AWAM.md` tertulis per skenario (simpan sesudah tiap skenario);
  review kode tertulis.

### PK-2.13 Perbaikan temuan PK-2.12 (S)

### PK-2.14 Uji pengguna putaran 1 (pemilik) · Butuh PK-2.13, K-15c

- **Agen menyiapkan:** APK debug, skrip dan lembar catatan (Lampiran C §3), satu halaman cara memasang.
- **Pemilik melakukan:** enam sesi. **Agen sesudahnya:** merangkum ke `docs/audit/2026-11-XX_UJI_PENGGUNA_1.md`,
  mengusulkan perbaikan sebagai paket baru bernomor PK-2.15 dan seterusnya, dan memperbarui rencana induk.
- **Selesai bila:** rangkuman tertulis dengan angka per peserta; paket perbaikan terdaftar.

---

## Tahap 3: Pintu B

### PK-3.1 Domain kalkulator resep (S) · tanpa prasyarat

- **Berkas:** baru `domain/RecipeCalculator.kt`, `domain/UnitConversion.kt`; tes `RecipeCalculatorTest`,
  `UnitConversionTest`.
- **Tes dulu:** semua golden Lampiran C §4.1 dan §4.2.
- **Isi:** `UnitConversion.convert(quantity, from, to): BigDecimal?` untuk gram, ons, kg, ml, liter (nama tanpa
  beda huruf besar kecil, juga "g", "gr", "l", "lt"); `RecipeCalculator.ingredientCost(priceRupiah, priceQuantity,
  usedQuantity)` dibulatkan sekali; `suggestedPrice(costRupiah, markupPercent)` dibulatkan ke atas ke Rp100;
  `whatIf(spec, prices, materialIds atau semua, percent)` mengembalikan modal sebelum dan sesudah per satuan jual.
  Semuanya fungsi murni di atas `ProductCosting`.
- **Selesai bila:** tes hijau; tidak ada `Double` atau `Float`.

### PK-3.2 Harga bahan per kemasan (S) · Butuh K-4 = A, PK-1.2

- **Berkas:** `domain/ProductCosting.kt` (`lineCost`, tipe harga bahan dari `Long` menjadi
  `MaterialPrice(rupiah, perQuantity)`), `data/FinanceRepository.kt` (`materialPrices` baris kira-kira 340,
  `materialPrice`), `data/PlanAssembly.kt`, `data/LegacySaleShape.kt`, `domain/StockManagement.kt`.
- **Tes dulu:** telur Rp28.000 untuk 30, dipakai 4: Rp3.733; bahan dengan belanja terakhir tetap memakai harga
  belanja (per 1); golden keripik tidak berubah; nilai bahan di gudang memakai harga per satuan yang sama.
- **Selesai bila:** tes hijau; `grep -rn "Map<String, Long>" data domain` untuk harga bahan kosong.

### PK-3.3 Layar kalkulator resep (S) · Butuh PK-3.1, PK-2.8 (dan PK-3.2 bila K-4 = A)

- **Spek:** Lampiran A §11. **Berkas:** baru `presentation/screens/RecipeCalculatorScreen.kt`; `HomeScreen.kt`
  (pintu), `ProductEditorScreen.kt` (jalan masuk), `AppViewModel.kt`, `strings.xml`.
- **Tes dulu** (baru `RecipeCalculatorScreenTest`): di instal bersih dengan usaha saja, isi resep keripik golden:
  layar menampilkan Rp2.400.000 dan Rp6.000; pilih 30 %: Rp7.800; "Simpan sebagai barang" membuat barang dan
  bahan; "Aku jual" keripik lalu menghasilkan baris bermodal Rp6.000; sheet bahan "250 gram, Rp12.000 untuk 1 kg"
  menampilkan Rp3.000; "kalau minyak naik 10 %" menampilkan Rp6.125 dan tidak mengubah data.
- **Selesai bila:** tes hijau; layar tidak menghitung sendiri (semua angka dari `RecipeCalculator`).
- **Bukti nyata:** emulator, resep keripik dan satu resep kue dengan gram dan butir. Bandingkan dengan hitungan
  tangan yang ditulis di laporan.

### PK-3.4 Domain kelayakan (K) · Butuh K-14

- **Berkas:** baru `domain/Feasibility.kt`; tes `FeasibilityTest`.
- **Tes dulu:** golden Lampiran C §4.3.
- **Isi:** `Feasibility.of(items: List<FeasibilityItem(perMonth, priceRupiah, costRupiah)>, monthlyFixedRupiah,
  startupRupiah): FeasibilityResult(profitPerMonth, breakEvenPerItem, paybackMonths, verdict)`. `verdict`:
  `NOT_ASSESSABLE` bila biaya rutin 0, `FEASIBLE` bila untung > 0, `NOT_FEASIBLE` selain itu. Modal kembali
  dibulatkan ke atas ke bulan penuh.
- **Catatan:** `PlanCalculator` lama tetap ada untuk Rincian analisis dan PDF. Jangan dihapus di paket ini.

### PK-3.5 Layar kelayakan (S) · Butuh PK-3.4, PK-2.2

- **Spek:** Lampiran A §12. **Berkas:** `presentation/screens/PlanScreen.kt` (diganti; `PeriodSheet` dibuang dari
  layar), `presentation/components/PlanCards.kt`, `data/PlanAssembly.kt`, `strings.xml`.
- **Penyimpanan:** tanpa tabel baru. Jumlah per bulan di `products.targetPerMonthText`; biaya rutin sebagai
  `budget_items` buku PROJECTION dengan `recurrence = MONTHLY`; modal awal sebagai baris `ONCE` atau alat. Data
  periode lama dibiarkan di basis data.
- **Tes dulu** (`PlanProductsTest` diperbarui): tiga langkah menghasilkan halaman hasil golden; biaya rutin kosong
  menampilkan "Belum bisa dinilai" dengan tombol; tidak ada pemilih periode; kata "R/C" hanya di dalam Rincian
  analisis yang terlipat.
- **Selesai bila:** tes hijau; 6 rujukan `BookMode.PROJECTION/REALIZATION` di lapisan presentasi (audit T7) ditinjau
  dan yang tidak perlu dibuang; menutup R5 dan R8.

### PK-3.6 Review independen Tahap 3 (hanya baca) · PK-3.7 Perbaikan (S)

Fokus review: satu pembulatan; konversi satuan; saran harga; putusan layak; barang dari kalkulator sama dengan
barang dari editor; PDF tetap memakai angka `MonthlySummary`.

### PK-3.8 Uji praktisi (pemilik) · Butuh PK-3.7

Lampiran C §4.4. Agen menyiapkan lembar; pemilik menemui satu pembuat makanan; agen mencatat hasil dan selisih.
Selisih di atas Rp0 per satuan jual harus dijelaskan atau diperbaiki sebelum Tahap 5.

---

## Tahap 4: Excel dan data aman

Baca dulu: DEC-012, `importer/ImportModels.kt`, "Import and export rules" di `AGENTS.md`, Paket G rencana lama.

### PK-4.1 Kontrak v5 final dan ekspor (S) · Butuh Tahap 1 selesai

- **Berkas:** `importer/ImportModels.kt` (`MankeuTemplate`), `exporter/FinanceWorkbookExporter.kt`,
  `data/FinanceRepository.kt` (`buildProjectExportBundle`).
- **Lembar dan kolom baru** (versi tetap 5 karena belum dirilis):

| Lembar | Kolom |
|---|---|
| Petunjuk | baris kunci baru: "Jenis usaha", "Cara hitung untung" |
| Produk | tambah: Jenis, Hitung Stok, Modal per Satuan (Rp), Susut per Batch |
| Stok Barang | tambah: Harga untuk Jumlah (K-4 = A) |
| Kontak (baru) | Nama, Pembeli, Tempat Beli, No. HP, Alamat, Catatan, Diarsipkan |
| Nota (baru) | No. Nota, Tanggal, Pembeli, Jatuh Tempo, Diskon (Rp), Catatan, No. Titip, Modal Perkiraan |
| Baris Nota (baru) | No. Nota, Barang, Nama Baris, Satuan, Isi, Jumlah, Harga Satuan (Rp), Modal Baris (Rp), Modal Diketahui |
| Titip (baru) | No. Titip, Tanggal, Toko, Barang, Satuan, Isi, Jumlah, Harga Setor (Rp) |
| Transaksi | tambah: No. Nota, Kontak, Jatuh Tempo, Melunasi, Isi Satuan, Dibayar Lewat (K-2 = B) |
| Kategori | tambah: Biaya Produksi; lembar ini menjadi wajib dan dibaca |
| Produksi & Stok | jenis baru: Tarik titip |

- **Tes dulu** (`XlsxWorkbookWriterTest`, `TemplateImportTest`): template kosong memuat semua lembar dan judul
  kolom persis; ekspor data golden memuat nota, baris bebas, kontak, dan titip.
- **Selesai bila:** tes hijau; judul kolom hanya didefinisikan di `MankeuTemplate`. Menutup R3.

### PK-4.2 Impor lembar baru dengan pesan per sel (S) · Butuh PK-4.1

- **Berkas:** `importer/FinanceWorkbookImporter.kt`, `importer/ImportModels.kt` (model impor baru),
  `data/FinanceRepository.kt` (`commitImport`, `reviewImport`, `undoImportBatch`).
- **Tes dulu:** tiap aturan validasi punya satu tes yang menegaskan teks pesannya: tanggal salah, uang berisi
  huruf, rujukan No. Nota tidak ada, pembeli tidak ada di Kontak, jumlah laku melebihi titip, pembayaran melebihi
  total, baris tanpa Barang dan tanpa Nama Baris. Bentuk pesan: "Lembar {lembar}, baris {n}, kolom {kolom}:
  tertulis '{nilai}'. {cara memperbaiki}."
- **Selesai bila:** tes hijau; impor tetap satu transaksi, satu batch, bisa dibatalkan, dengan pemberitahuan.

### PK-4.3 Kategori dibaca; pembayaran dan tempo (S) · Butuh PK-4.2

Menutup R2. Tes: tanda "Biaya Produksi" yang diubah di file bertahan sesudah impor; pembayaran nota dan pelunasan
belanja tempo terimpor dan tertaut.

### PK-4.4 Round trip golden (S) · Butuh PK-4.3

- **Berkas:** `ImportCommitTest.kt`, `GoldenData.kt`, aset `app/src/androidTest/assets/simulasi/*.xlsx`,
  `docs/evidence/2026-10-22_simulasi_skala_umkm/make_sims.py`.
- **Tes dulu:** untuk data tiap persona PK-1.7 dan data keripik: ekspor, impor ke usaha kosong, lalu bandingkan
  nota, status bayar, sisa titip, utang, sisa barang, sisa bahan, dan `MonthlySummary` tiap bulan. Semua sama.
- **Selesai bila:** tes hijau; `ScaleSimulationTest` dan `expected.json` tetap lulus.

### PK-4.5 Mode impor (S) · Butuh K-13, PK-4.4

- **Spek:** Lampiran A §14. **Berkas:** `data/FinanceRepository.kt`, `backup/EncryptedBackupManager.kt` (cadangan
  otomatis lokal), layar impor di `presentation/screens/MoreScreen.kt`, `logs/DECISION_LOG.md` (amandemen DEC-012).
- **Tes dulu** (K-13 = A): ekspor, ubah satu harga di file, impor "Ganti": jumlah catatan tetap, harga berubah,
  `MonthlySummary` mengikuti; "Batalkan" mengembalikan data persis; "Ganti" hanya menyentuh usaha terpilih, usaha
  lain utuh; file kosong tidak bisa dipakai untuk "Ganti"; "Tambahkan" berperilaku seperti sekarang.
- **Selesai bila:** tes hijau; DEC-012 diamandemen dengan tanggal dan rujukan K-13.
- **Risiko:** X10. Paket ini wajib mendapat review di PK-4.8.

### PK-4.6 Template ramah awam (S) · Butuh PK-4.1

Lampiran A §14 (Petunjuk, lembar Contoh yang diabaikan impor, tiga baris Excel di menu). Tes: lembar Contoh tidak
terimpor; template kosong yang diisi mengikuti Petunjuk terimpor tanpa galat (aset uji baru yang diisi tangan).

### PK-4.7 Cadangan dan pindah HP (S)

- **Berkas:** `presentation/screens/AccountSettingsScreen.kt`, `SyncCard.kt`, `MoreScreen.kt`,
  `backup/EncryptedBackupManager.kt`, `WelcomeScreen.kt` (tautan "Ambil data lama").
- **Isi:** satu jalan masuk "Simpan cadangan" dan "Pindah HP" dengan langkah bernomor berbahasa awam. Tanpa
  mengubah format cadangan.
- **Tes dulu:** buat data, simpan cadangan ke berkas, `eraseAllData`, pulihkan dari layar pertama: `MonthlySummary`
  dan daftar pembeli sama persis.
- **Bukti nyata:** emulator: `pm clear`, pulih dari berkas. Drive: [belum], masuk cek-lis PK-5.4.

### PK-4.8 Review independen Tahap 4 (hanya baca) · PK-4.9 Perbaikan (S)

Fokus: DEC-012 butir demi butir; "Ganti" dan pembatalannya; pesan per sel; R10 dan R11 rencana lama.

---

## Tahap 5: rapikan, uji, rilis

### PK-5.1 Bahasa awam layar lama (S)

Kosongkan `kata_terlarang_dasar.txt` dengan mengganti kata menurut Lampiran A §2 dan tabel P4 audit 2026-10-30.
Selesai bila daftar dasar kosong dan tes tetap hijau. Menutup T5.

### PK-5.2 Bantuan (S) · Butuh Tahap 2 dan 3

Ubah lewat `docs/guide/help_strings.py`. Artikel yang menjelaskan layar yang sudah tiada dihapus atau ditulis ulang
untuk: layar pertama, Aku jual, Aku beli, siapa belum bayar, kalkulator, kelayakan, Excel, cadangan. Jalankan
`scripts/capture_help_screenshots.ps1`. `GuidesTest` hijau. Menutup T6, R7.

### PK-5.3 Uji ulang seluruh app (emulator)

Paket E lama dengan data keripik, ditambah jalur warung (Sederhana) dan jasa. Ukur jank seperti PK-0.4. Hasil di
`docs/audit/2026-11-XX_UJI_ULANG_UX.md`, simpan per layar. Lulus bila tidak ada temuan Berat dan sasaran ketukan
Lampiran C §1.4 tercapai.

### PK-5.4 HP nyata murah (pemilik) · PK-5.5 Uji pengguna putaran 2 (pemilik)

Lampiran C §5 dan §3. Agen menyiapkan `docs/audit/CHECKLIST_HP_NYATA.md` dan lembar uji; pemilik menjalankan; agen
merangkum dan mengerjakan paket perbaikan yang lahir darinya.

### PK-5.6 Review akhir dan merge (hanya baca, lalu K)

Paket H lama. Review seluruh branch terhadap `main` oleh model terkuat. Sesudah perbaikan: perbarui DOMAIN_MODEL,
PRODUCT_REQUIREMENTS, roadmap, DEC menjadi Implemented, CHANGELOG. Merge ke `main`, push, pointer superproject.

### PK-5.7 Persiapan rilis internal (K)

Paket I lama. `versionCode` 8. Catatan rilis berbahasa awam, termasuk: perbarui semua HP yang memakai sinkron; build
debug lama perlu pulih dari cadangan. AAB dan unggah ke track internal oleh pemilik.
