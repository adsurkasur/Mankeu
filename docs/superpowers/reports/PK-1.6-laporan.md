# PK-1.6 — Dua cara hitung untung

**Status: selesai pada domain, repository, ringkasan, dan verifikasi layar/ekspor terarah.** Tidak ada perubahan schema. [terverifikasi]

## Hasil terhadap paket

- SIMPLE: penjualan Rp850.000 dikurangi belanja bahan Rp500.000 dan listrik Rp50.000 menghasilkan untung Rp300.000; HPP efektif Rp0. [terverifikasi]
- ITEM_COST dengan data sama dan HPP terjual Rp400.000 menghasilkan untung Rp400.000 dan selisih belanja produksi Rp100.000. [terverifikasi]
- ITEM_COST menghitung baris `costKnown=0`; ringkasan bulan menampilkan jumlah baris dan nilai penjualan terkait. Tes repository mengubah satu baris sintetis menjadi unknown dan memverifikasi 1 serta Rp55.000. [terverifikasi]
- Golden keripik Oktober tetap ITEM_COST: laba −Rp310.000, HPP Rp4.440.000. [terverifikasi]
- `MonthlySummary` meneruskan basis, HPP efektif, jumlah/nilai tanpa modal dan semua angka untung. OneNumberTest kini mencakup ITEM_COST serta SIMPLE pada ringkasan, Beranda, Laporan, kartu BEP dan ekspor. [terverifikasi]
- Usaha baru tetap SIMPLE; migrasi usaha lama tetap ITEM_COST. `setProfitBasis` menulis pilihan pada usaha. [terverifikasi]
- Pencarian kode menunjukkan layar dan eksportir tidak membaca `profitBasis` secara langsung. [terverifikasi]

## Perbaikan yang ditemukan selama verifikasi

Uji emulator SIMPLE awalnya menemukan kartu BEP masih mengurangi HPP mentah dan karena itu menampilkan −Rp310.000 saat ringkasan menghitung −Rp1.070.000. `PlanAssembly.actualMonth` sekarang memakai HPP efektif dan memasukkan belanja produksi ke biaya aktual pada basis SIMPLE; OneNumberTest menangkap kesamaan hasilnya. Perubahan basis juga mengamati tabel `businesses` agar alur ringkasan diperbarui. [terverifikasi]

## Bukti

- `:app:testDebugUnitTest --tests com.mankeu.domain.ProfitCalculatorTest --tests com.mankeu.domain.MonthlySummaryTest`: 16 tes, 0 gagal. [terverifikasi]
- AVD `emulator-5556`: `FreeLineSaleTest#businessProfitBasisIsReadByTheSingleMonthlySummary` dan `#freeAndProductLinesShareOneNoteAndOnlyProductCarriesCost` lulus; `OneNumberTest#goldenOctoberReadsOneSummaryOnEveryScreenAndInTheExport` dan `#simpleBasisReadsOneSummaryOnEveryScreenAndInTheExport` lulus (4 kasus terarah). [terverifikasi]
- `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, dan `git diff --check` lulus. [terverifikasi]
- Pemeriksaan teks `profitBasis` di `presentation` dan `exporter` tidak menemukan pembacaan langsung. [terverifikasi]

## Batas dan pertanyaan terbuka

- Suite penuh, lint, ekspor PDF, seluruh alur edit/hapus, sinkronisasi, backup/restore, dan uji HP nyata tidak dijalankan pada paket ini. [belum]
- Nilai `salesWithoutCostRupiah` saat ini menjumlahkan subtotal baris unknown sebelum diskon nota; belum ada aturan di paket untuk membagi diskon antarbari. [terverifikasi sebagai perilaku implementasi; keputusan produk belum]
- Pilihan untuk nilai tersebut: (A) tampilkan nilai subtotal sebelum diskon, seperti sekarang; (B) alokasikan diskon nota secara proporsional ke baris lalu tampilkan nilai neto. [belum diputuskan; tidak menghalangi paket karena rencana tidak menetapkan pembagian diskon]
- Integrasi `costKnown` ke alur produk/biaya yang tertunda di PK-1.2 masih terbuka. Paket ini memastikan ringkasan dapat membaca penanda yang tersimpan, bukan menutup seluruh checkpoint PK-1.2. [terverifikasi]
- Depresiasi tetap mengurangi laba di dua basis; semua nominal uang tetap `Long` rupiah dan tidak ada pembulatan uang baru pada jalur ini. [terverifikasi]
