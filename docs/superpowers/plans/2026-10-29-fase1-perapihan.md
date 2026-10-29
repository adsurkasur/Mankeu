# Perapihan Temuan Fase 1

> Untuk agen: kerjakan per item, uji dulu (tes gagal → perbaiki → lulus), satu commit per kelompok dengan entri log.
> Status tiap item ditulis di bagian **Status** paling bawah segera setelah selesai, supaya pekerjaan bisa dilanjutkan
> bila sesi terputus.

**Asal:** review akhir Fase 1 dan uji emulator (`docs/audit/2026-10-29_UJI_FASE1.md`). Item ini sebelumnya diparkir.
Pemilik meminta (2026-10-29) supaya temuan yang perlu dirombak atau dibetulkan memang dikerjakan, bukan dibiarkan.
**Spec:** `docs/superpowers/specs/2026-10-29-redesign-spec.md`. **Keputusan terkait:** DEC-012, DEC-028.

## Batasan

- Sama dengan Global Constraints rencana Fase 1 (`2026-10-29-fase1-produk-hpp.md`): uang `Long` rupiah, pembulatan
  setengah ke atas, kuantitas `BigDecimal`, bahasa sehari-hari, string ID + EN, log di commit yang sama.
- Schema tetap 13 kecuali item memaksa. Bila schema harus naik, migrasi + `BackupUpgrade` + tes migrasi wajib.
- Template Excel tetap v5 (belum pernah dirilis), jadi kolom atau lembar baru tidak perlu menaikkan versi.

## Item

| # | Temuan | Keputusan | Kriteria selesai |
|---|---|---|---|
| P1 | Entri Produksi/Hitung stok yang salah tidak bisa dihapus atau diubah | Tiap produk punya "Riwayat stok": daftar produksi, hitung stok, dan penjualan (penjualan hanya tampil, diubah lewat Catatan). Produksi dan hitung stok bisa dihapus dengan konfirmasi. Mengubah = hapus lalu catat ulang (cukup untuk sekarang) | Tes repository: hapus satu produksi → stok turun sesuai. Tes UI: riwayat tampil dan hapus bekerja |
| P2 | Bahan yang diarsipkan kembali aktif setelah ekspor → impor | Lembar Stok Barang mendapat kolom "Diarsipkan" (`Ya` / kosong). Impor mempertahankan status arsip | Tes round trip: bahan arsip tetap arsip, tautan resep/belanja tetap ada |
| P3 | Undo impor menghapus produk dan bahan beserta data yang dicatat **setelah** impor (produksi, penjualan, belanja yang menautkan) | Undo tidak menghapus produk/bahan impor yang sudah dipakai data lain di luar batch. Produk/bahan itu dilepas dari batch (`importBatchId = null`) dan tetap ada; pemberitahuan undo menyebut namanya | Tes: impor → catat produksi + penjualan → undo → produk, produksi, penjualan tetap ada; produk yang tidak dipakai tetap terhapus |
| P4 | Sinkron: produk kembar dari dua HP menggandakan satuan jual dan baris resep | Tambah kunci alami `product_units` (`productId`, `name`) dan `product_recipe_lines` (`productId`, `name`) di `SyncEngine.NATURAL_KEYS`, sehingga baris anak dari produk kembar ikut digabung | Tes SyncEngine: dua HP membuat produk + satuan "bungkus" yang sama offline → setelah sinkron satu produk dengan satu "bungkus" |
| P5 | Nama produk kembar dalam satu file impor | Ditolak dengan pesan per sel ("produk \"X\" ditulis dua kali di lembar Produk") | Tes importer |
| P6 | Tombol Back tidak menutup sheet Usaha/Unit (sudah ada sebelum Fase 1) | `BackHandler` menutup sheet lebih dulu | Tes UI atau uji emulator, dicatat |
| P7 | Dua cabang form catat tanpa tes otomatis: produk tanpa satuan jual (isi lama tidak boleh tertinggal); bahan terpilih lalu pindah ke Uang masuk (kolom satuan harus terbuka) | Tambah tes Compose untuk keduanya; buat fixture minimal bila belum ada | Dua tes lulus, gagal bila perbaikannya dibuang |
| P8 | Generator `docs/evidence/*/make_sims.py` masih menulis template v4 | Perbarui ke v5 (termasuk Satuan Jual, Resep per batch, Produksi & Stok, kolom Transaksi baru) dan pastikan hasilnya sama dengan aset `app/src/androidTest/assets/simulasi/` (atau aset dibuat ulang dari generator dan `expected.json` tetap lulus) | Menjalankan generator lalu tes skala → lulus |
| P9 | Pesan "Transaksi tersimpan; jurnal debit dan kredit seimbang." memakai bahasa akuntan (#19) | Ganti menjadi "Transaksi tersimpan." (EN "Transaction saved."); cek pesan sukses lain yang menyebut jurnal/debit/kredit dan ganti juga | grep tidak menemukan "debit"/"kredit" di pesan layar (kecuali PDF/Rincian analisis) |
| P10 | Harga bahan bersama dihitung dari belanja di seluruh usaha, sedangkan ekspor satu unit hanya membawa belanja unit itu | Diterima sebagai perilaku benar: harga bahan bersama memang harga terakhir di usaha. Ekspor menulis harga bahan saat ini sebagai "Harga Beli", jadi setelah impor harganya sama. Tambahkan tes yang membuktikan harga setelah impor = harga saat ekspor; bila tidak sama, perbaiki | Tes round trip harga bahan bersama |

## Status

Diperbarui setiap item selesai (tanggal, commit, bukti).

- **P9** selesai 2026-10-29 11:19, commit `fix(mankeu): everyday save message (P9)`. Bukti: unit `ScreenLanguageTest` merah (4 string: `msg_event_saved` dan `backup_choose_body`, ID + EN), lalu hijau. Tambahan: deskripsi cadangan tidak lagi menyebut "jurnal". Tidak diubah: pesan penjaga "Jurnal tidak seimbang" di `FinanceRepository` (invarian internal, tidak muncul pada simpan normal).
- **P5** selesai 2026-10-29 11:20, commit `fix(import): refuse a product named twice in one file (P5)`. Bukti: unit `TemplateImportTest.a product written twice in the Produk sheet is named` merah (tanpa error), lalu hijau (21/21). Nama dibandingkan tanpa beda huruf besar/kecil dan spasi tepi, sama seperti pencocokan Satuan Jual.
- **P2** selesai 2026-10-29 11:24, commit `fix(template): archived materials stay archived through export and import (P2)`. Bukti: merah dengan field model saja (unit 2 gagal; connected `anArchivedMaterialStaysArchivedAndLinkedThroughExportAndImport` expected 1 but was 0), hijau: unit 95/0, connected ImportCommitTest + ScaleSimulationTest 14/0. Nilai selain "Ya"/kosong ditolak per sel. Aset simulasi diberi header F1 saja (dibuat ulang di P8). Addendum DEC-028.
- **P10** selesai 2026-10-29 11:26, commit `fix(export): write the current material price (P10)`. **Penyimpangan:** anggapan rencana ("ekspor menulis harga bahan saat ini") ternyata salah; ekspor menulis harga yang diisi saat bahan dibuat. Tes connected `aSharedMaterialKeepsItsCurrentPriceThroughExportAndImport` merah (expected 27500 but was 20000), diperbaiki di `exportedStock`, lalu hijau (ImportCommitTest + ScaleSimulationTest + ProductStockFlowTest 20/0). Addendum DEC-028.
