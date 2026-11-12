# PK-3.1: domain kalkulator resep

Tanggal: 2026-11-13. Dasar: `c74b747`; paket model schema 14 sebelumnya masih berstatus checkpoint.

## Hasil

- [verified] `UnitConversion` mengubah gram/g/gr, ons, kilogram/kg, mililiter/ml, dan liter/l/lt dengan BigDecimal. Nama satuan identik setelah trim dan penyamaan kapital mempertahankan kuantitas, termasuk satuan kustom; satuan berbeda yang tidak dikenal dan lintas dimensi menghasilkan null.
- [verified] `ingredientCost` menghitung harga × jumlah dipakai ÷ jumlah pembelian, HALF_UP sekali ke rupiah. `suggestedPrice` membulatkan ke atas ke kelipatan Rp100. Input negatif dan penyebut nol ditolak.
- [verified] `whatIf` menghitung sebelum dan sesudah perubahan untuk bahan terpilih atau seluruh biaya termasuk biaya bebas. Persentase perubahan negatif sampai −100% diterima. Resep tidak lengkap tetap tidak lengkap; tidak menganggap bahan tanpa harga bernilai nol yang diketahui.
- [verified] Review menemukan perbedaan pada biaya pecahan: dua baris masing-masing 0,3 × Rp5 sebelumnya dibulatkan masing-masing menjadi Rp2, sehingga batch Rp4 alih-alih Rp3. `ProductCosting.cost` kini menjumlahkan biaya desimal terlebih dahulu dan menghitung tiap satuan langsung dari jumlah itu; `lineCost` lama tetap menyediakan angka rupiah per baris. `whatIf` memakai sumber desimal yang sama. Perubahan ini mengoreksi aturan satu pembulatan dan memengaruhi resep dengan biaya baris pecahan; nominal tersimpan tidak diubah.
- [verified] Tiga pemakai yang sebelumnya membulatkan batch sebelum menghitung jumlah jual (`FinanceRepository.writeSale`, `LegacySaleShape.convert`, dan preview di `SaleNoteScreens`) menggunakan `ProductCosting.hpp(spec, prices, content)` agar snapshot dan preview membulatkan sekali di akhir.
- [verified] `priceImpact` membandingkan hasil biaya lengkap, termasuk status bahan belum berharga. Dialog dampak harga tidak menampilkan angka sebelum/sesudah sebagai diketahui jika salah satu biaya belum lengkap; pesan tersedia dalam dua bahasa. Editor resep menjelaskan selisih pembulatan jika total dari bahan yang ditampilkan berbeda dari jumlah baris bulat, termasuk pada resep yang belum lengkap. Pesan tidak tampil bila jumlahnya sama.

## Bukti

Semua Gradle lewat PowerShell dengan `$env:Path = ($env:Path -split ';' | Where-Object { $_ -notmatch '"' }) -join ';'`.

| Pemeriksaan | Hasil |
|---|---|
| API belum tersedia | RED kompilasi karena `RecipeCalculator` dan `UnitConversion` belum ada; bukan bukti kegagalan perilaku. |
| Stub API | RED perilaku: 4 tes, 3 gagal; konversi dan aritmetika belum diterapkan. |
| Dua biaya pecahan | RED: biaya dua baris 1,5 menghasilkan Rp4, bukan Rp3. Setelah perbaikan unit test hijau. Perubahan 0% sama dengan sebelum; satu baris 1,5 naik 100% menjadi Rp3. |
| Dampak harga | RED: batch tetap Rp1 namun HPP satuan isi 100 berubah Rp100 menjadi Rp101. RED lain: harga bahan baru membuat resep lengkap walau nominal bulat masih Rp0. Keduanya kini terdeteksi. |
| Satuan identik | RED `butir` ke `BUTIR`, lalu GREEN setelah identitas trim/case-insensitive ditambahkan. `sendok` ke `kg` tetap null. |
| Notifikasi modal belum lengkap | RED Compose pada stub, lalu GREEN dua keadaan lengkap dan belum lengkap. |
| `ExactRecipeCostFlowTest` | GREEN Room pada emulator: resep 0,3 × Rp5, hasil 2, jual 3 satuan, HPP tersimpan Rp2 (2,25 dibulatkan sekali). |
| `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` | GREEN: 30 suite JVM, 189 tes, 0 gagal/error/dilewati; lint 0 error, 87 warning; build debug sukses. |
| Instrumented terfokus | GREEN 6/6: `ProductRecipeRoundingTest` tiga tes, `PriceImpactRowsTest` dua tes dan `ExactRecipeCostFlowTest` satu tes. Tes editor membuktikan dua baris Rp2, total Rp3 dan penjelasan; juga memeriksa resep tidak lengkap dan keadaan tanpa selisih. |
| `git diff --check` | Lulus. |

Golden lain: resep keripik Rp2.400.000; Rp6.000/bungkus dan Rp240.000/dus; susut 20 menghasilkan Rp6.316 dan Rp252.632; harga saran 30% Rp7.800/Rp312.000 dan Rp8.300 dari Rp6.316; minyak naik 10% menghasilkan Rp2.450.000/Rp6.125/Rp245.000; semua naik 10% menghasilkan Rp2.640.000/Rp6.600. Konversi dan harga kemasan mencakup tepung, gula, minyak, telur dan garam di Lampiran C §4.2.

## Lingkup dan batas

[verified] Instrumented memakai basis data dalam memori dan data sintetis. Emulator semula tidak terhubung; AVD `Medium_Phone` yang sudah ada dinyalakan. Setelah tes, APK sebelum perubahan schema 14 dipasang kembali dan backup asli dipulihkan. Verifikasi tar gabungan sempat melewatkan berkas shm; pemeriksaan SHA-256 secara individual menunjukkan 4/4 sama. Pemulihan ulang setelah tes notifikasi menghasilkan verifikasi helper 4/4 sama.

[verified] Satu perintah Gradle penuh sempat masih berjalan ketika RED terfokus berikutnya dimulai. Kedua proses berakhir; verifikasi akhir di atas dijalankan ulang secara serial. Kegagalan awal tes notifikasi sesudah implementasi berasal dari matcher teks yang menuntut kesamaan penuh padahal nama produk dan pesan digabung; matcher substring diperbaiki dan tes dijalankan ulang.

[unknown] Belum diuji seluruh suite instrumented, migrasi/restore dan sync ulang untuk perubahan pembulatan ini, perangkat fisik, dan validasi praktisi. Paket PK-1.2 tetap checkpoint; PK-3.1 tidak menyelesaikan aturan stok, basis untung, UI kalkulator, ataupun pilihan pemilik K-8/K-14. `lineCost` masih membulatkan untuk tampilan per baris; penjumlahan resep memakai biaya mentah. Editor menjelaskan ketika jumlah baris dan total berbeda; penyajian pada layar kalkulator baru tetap harus diverifikasi saat layar itu dibangun.

[proposed] Review independen perlu memeriksa perubahan `ProductCosting` dan tiga pemakai snapshot/preview sebelum commit. Laporan, log aktivitas, keputusan arsitektur, panduan domain, changelog bila perlu, dan commit atomik ditangani agen induk.
