# Laporan Perapihan Fase 1 (P1 sampai P10)

Tanggal: 2026-10-29. Branch: `fase1-perapihan` (dari main `f49cf1c`), belum di-push. Rencana dan Status per item:
`docs/superpowers/plans/2026-10-29-fase1-perapihan.md`. Keputusan: addendum DEC-028 di `logs/DECISION_LOG.md`.
Status klaim: semua angka tes di bawah **verified** (dijalankan di sesi ini); uji tangan dilakukan di emulator-5554 (Android 11),
bukan HP fisik.

## Ringkasan

| # | Hasil | Commit |
|---|---|---|
| P1 | Selesai: halaman penuh "Riwayat stok" per produk, hapus produksi/hitung stok dengan konfirmasi | `79bd0ec` |
| P2 | Selesai: kolom "Diarsipkan" di Stok Barang, status arsip bertahan | `3401de7` |
| P3 | Selesai: undo impor menyimpan produk/bahan yang sudah dipakai catatan sesudah impor | `eb8c379` |
| P4 | Selesai, lebih luas dari rencana: kunci alami anak produk kembar + hapus-duluan | `5530db0` |
| P5 | Selesai: nama produk kembar dalam satu file ditolak per sel | `a09fce4` |
| P6 | **Tidak ada bug.** Perubahan pertama dikembalikan; temuan berasal dari keadaan emulator | `24f64db`, `a308254`, `7abad87` |
| P7 | Selesai: dua tes Compose, terbukti gagal bila perbaikannya dibuang | `83e9c9e` |
| P8 | Selesai untuk generator aset tes; dua generator lama dibiarkan sebagai bukti historis | `8b0f3ce` |
| P9 | Selesai: "Transaksi tersimpan." + tes bahasa layar | `ab7d69a` |
| P10 | Anggapan rencana salah; diperbaiki: ekspor menulis harga bahan saat ini | `4e06461` |

Schema tetap 13, template tetap v5. Tidak ada dependensi baru.

## Per item

### P1 Riwayat stok
- **Perubahan:** tombol "Riwayat stok" di kartu produk membuka overlay halaman penuh (`Overlay.STOCK_HISTORY`, pola yang sama
  dengan editor produk; daftar panjang = halaman penuh, #13). Isi: stok sekarang, petunjuk "hapus lalu catat ulang", lalu
  produksi, hitung stok, dan penjualan terbaru di atas, masing-masing dengan stok sesudah baris itu. Produksi dan hitung stok
  punya tombol hapus dengan dialog konfirmasi; penjualan hanya tampil ("Ubah atau hapus penjualan di Catatan."). Mengubah =
  hapus lalu catat ulang. `ProductRecord.history` (`StockHistoryEntry`) jadi satu sumber untuk daftar dan `stock`.
  `FinanceRepository.deleteProductMovement` menolak id yang bukan baris produksi/hitung stok. String ID + EN.
- **Bukti:** RED = tes baru tidak terkompilasi (API belum ada). GREEN: unit `ProductRecordTest` (urutan, jumlah, stok sesudah
  tiap baris: 400, 320, 300, 290); connected `ProductStockFlowTest.aWrongProductionOrCountIsDeletedAndTheStockFollows`
  (hapus hitung stok 5 -> 1.190, hapus produksi salah -> 390, id penjualan ditolak); `StockHistoryScreenTest` 2 tes.
- **Uji tangan (emulator):** produk Keripik (400 bungkus per batch), Produksi 2x -> stok 800; Riwayat stok menampilkan dua baris
  "Produksi 1 batch, +400 bungkus, Stok jadi 400/800"; hapus yang atas -> dialog "Hapus Produksi 1 batch tanggal 29 Okt 2026?"
  -> Hapus -> "Catatan stok dihapus.", stok 400. PASS.
- **File:** `data/{ProductRecord,FinanceRepository,FinanceDao}.kt`, `domain/ProductCosting.kt`, `presentation/{AppViewModel,MankeuApp}.kt`,
  `presentation/screens/{ProductsScreen,StockHistoryScreen}.kt`, strings ID/EN, tes di atas.
- **Catatan:** commit P3 sempat membuat tes unit tidak terkompilasi (`FakeStockDao` belum punya dua method baru); diperbaiki di commit P1.

### P2 Bahan diarsipkan
- **Perubahan:** Stok Barang kolom keenam "Diarsipkan" ("Ya"/kosong, lainnya ditolak per sel). Ekspor menulis "Ya" untuk bahan
  arsip yang masih disebut resep/belanja; impor menyimpannya sebagai arsip dan tautan resep/belanja tetap mengarah ke sana.
- **Bukti:** RED (field model saja): 2 tes unit gagal; connected `anArchivedMaterialStaysArchivedAndLinkedThroughExportAndImport`
  expected 1 but was 0. GREEN: unit 95/0, connected ImportCommitTest + ScaleSimulationTest 14/0.
- **File:** `importer/{ImportModels,FinanceWorkbookImporter}.kt`, `exporter/FinanceWorkbookExporter.kt`, `data/FinanceRepository.kt`, tes.

### P3 Undo impor
- **Perubahan:** undo menghapus baris Produksi & Stok milik batch (id berawalan `<batchId>:` saat impor, tanpa naik schema),
  lalu menyimpan dan melepas (`importBatchId = null`) produk yang dipakai penjualan/produksi di luar batch dan bahan yang dipakai
  belanja di luar batch, resep produk di luar batch, atau gerakan stok bahan. Sisanya dihapus. Snackbar undo menyebut nama yang
  disimpan (string baru ID + EN).
- **Bukti:** RED: `undoKeepsImportedProductsAndMaterialsThatLaterRecordsUse` expected [Keripik, Minyak] but was []. GREEN:
  ImportCommitTest + LedgerIntegrityTest + SyncEngineTest 29/0.
- **Penyimpangan/keputusan kecil:** awalan id sebagai penanda (DEC-028 addendum). Batch lama (id tanpa awalan): produk yang punya
  baris produksi dianggap dipakai dan disimpan (arah aman).

### P4 Sinkron produk kembar
- **Perubahan:** kunci alami `product_units`/`product_recipe_lines` (`productId`, `name`) hanya untuk anak produk yang sudah menyerap
  kembarannya, dan hapus baris kedua tabel itu dijalankan sebelum upsert.
- **Bukti:** RED: dua "bungkus". Kunci alami saja (tanpa hapus-duluan) membuat edit berikutnya kehilangan baris resep
  (expected [(Bumbu, 2500000)] but was []), jadi hapus-duluan wajib. GREEN: SyncEngineTest + SyncControllerTest 14/0.
- **Batas:** dua baris resep bernama sama dalam satu produk kembar tetap tergabung.

### P5 Nama produk kembar dalam satu file
- **Perubahan:** baris kedua dan seterusnya ditolak: `Produk!A4: produk "teh" ditulis dua kali di lembar "Produk".`
- **Bukti:** RED lalu GREEN `TemplateImportTest` 21/21.

### P6 Back dan sheet Usaha/Unit
- **Hasil:** tidak ada bug di aplikasi. Di emulator yang baru di-reboot, tanpa perubahan kode, Back menutup "Tambah usaha", lalu
  "Usaha dan Cabang / Unit", dan juga sheet Produksi di tab Produk; app tetap terbuka. Kegagalan yang terlihat (di walk Fase 1
  dan awal sesi ini) datang dari keadaan emulator yang **tidak mengirim tombol apa pun ke app**: setelah suite connected panjang
  atau satu run tes yang berakhir `keyDispatchingTimedOut`, Back bahkan tidak memindah tab Produk ke Beranda; reboot memulihkannya.
- **Jejak:** `24f64db` menambah `SheetBackHandler`; `a308254` mengembalikannya setelah run pembanding (handler dimatikan) lulus;
  `7abad87` menghapus `SheetBackTest` karena di suite penuh gagal oleh fokus jendela (`RootViewWithoutFocusException`), bukan oleh app.
- **Bukti:** uji tangan di atas (dua kali, setelah reboot). DEC-028 addendum P6.

### P7 Tes form catat
- **Perubahan:** `EntryFormFallbackTest` (fixture kecil di file tes). (1) Kopi -> "cup"/10000, lalu Teh tanpa satuan jual -> "gelas"
  dan harga kosong. (2) Minyak di Uang keluar mengunci kolom satuan; setelah pindah ke Uang masuk kolom bisa diisi.
- **Bukti:** GREEN 2/0; dengan dua perbaikan dibuang dari `EntryScreen` keduanya gagal (expected (gelas, ) but was (cup, 10000);
  kolom satuan tetap terkunci). Kode produksi tidak berubah.

### P8 Generator simulasi
- **Perubahan:** `docs/evidence/2026-10-22_simulasi_skala_umkm/make_sims.py` menulis v5 langsung (kolom Transaksi baru, Produk dengan
  satuan dasar dan hasil per batch, Satuan Jual, Resep per batch, Produksi & Stok kosong, Stok Barang dengan Diarsipkan). Dijalankan
  dengan `python -I`; semua sel di semua lembar sama dengan aset lama, `expected.json` hanya beda `file_bytes`. Aset tes kini salinan
  keluaran generator.
- **Bukti:** ScaleSimulationTest + LedgerIntegrityTest + ViewModelFlowsTest 13/0.
- **Penyimpangan:** dua generator 2026-10-21 tetap v3/v4 (bukti historis; konversi mengubah angka BEP harapan), diberi catatan.

### P9 Pesan simpan
- **Perubahan:** "Transaksi tersimpan." / "Transaction saved."; deskripsi cadangan tidak lagi menyebut "jurnal". `ScreenLanguageTest`
  memeriksa kedua strings.xml. Pesan penjaga internal "Jurnal tidak seimbang" dibiarkan (tidak muncul pada simpan normal).
- **Bukti:** RED 4 string, lalu GREEN.

### P10 Harga bahan bersama
- **Perubahan:** anggapan rencana ("ekspor menulis harga saat ini") salah: ekspor menulis harga saat bahan dibuat. `exportedStock`
  kini menulis harga terkini (belanja terakhir di usaha, kalau tidak ada harga awal).
- **Bukti:** RED `aSharedMaterialKeepsItsCurrentPriceThroughExportAndImport` expected 27500 but was 20000; GREEN 20/0 (tiga kelas).

## Verifikasi akhir (kode pada `7abad87`, kode produksi sama dengan HEAD laporan ini)

- `:app:testDebugUnitTest`: 96 tes, 0 gagal (sebelum: 92).
- `:app:lintDebug`: 0 error (74 warning).
- `:app:assembleDebug` dan `:app:assembleRelease`: berhasil.
- `:app:connectedDebugAndroidTest` (setelah reboot emulator): 67 tes, 1 gagal, 2 skip (tes screenshot). Yang gagal:
  `ViewModelFlowsTest.syncRunsOnStartAndAgainShortlyAfterAChange` (tes waktu, menegaskan status `Ready` sesaat setelah upload;
  tidak disentuh perapihan ini). Dijalankan ulang sendiri: ViewModelFlowsTest 4/0; di run penuh sebelumnya (69 tes) tes itu lulus.
  Satu-satunya kegagalan lain di run itu adalah `SheetBackTest` yang kemudian dihapus (lihat P6).
- Uji tangan: P1 PASS, P6 tidak ada bug (lihat di atas).

## Kekhawatiran dan hal terbuka

1. **Emulator kehilangan pengiriman tombol** setelah suite panjang atau ANR tes; ini bisa menyesatkan uji Back/keyboard berikutnya.
   Sebelum menilai perilaku tombol, reboot emulator dan cek bahwa Back di tab non-Beranda kembali ke Beranda.
2. `ViewModelFlowsTest.syncRunsOnStartAndAgainShortlyAfterAChange` kadang gagal (waktu). Sebaiknya menunggu status `Ready`
   alih-alih menegaskannya langsung; tidak diubah di sini karena di luar lingkup.
3. P3: batch impor yang dibuat sebelum perubahan ini tidak punya awalan id, jadi undo-nya menyimpan produk yang punya produksi.
4. P4: dua baris resep bernama sama dalam satu produk kembar tetap tergabung saat sinkron.
5. Belum diverifikasi: HP fisik, Gboard, sinkron dua HP sungguhan.
6. Branch belum di-push dan pointer superproject belum diperbarui (tugas ini: commit saja).
