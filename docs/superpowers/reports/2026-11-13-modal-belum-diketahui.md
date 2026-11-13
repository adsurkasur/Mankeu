# Modal penjualan belum diketahui — 2026-11-13 11:51 WIB

## Masalah dan perubahan

- [terverifikasi] `MonthlySummary` sudah membawa jumlah penjualan yang modalnya belum diketahui, tetapi Beranda, Laporan, kartu BEP dan ekspor masih memperlakukan laba parsial sebagai angka lengkap.
- [terverifikasi] Pada basis ITEM_COST, jika bulan tersebut memiliki baris penjualan tanpa modal, Beranda dan laporan menahan angka laba dan menyebut penyebabnya; kartu BEP tidak menunjukkan kesimpulan, dan laporan xlsx maupun PDF menghilangkan laba/HPP/selisih yang tidak lengkap serta memberi keterangan. Perbandingan Beranda dengan bulan lalu juga tidak memakai bulan lalu yang modalnya tidak lengkap. Arus kas dan data transaksi tetap tersedia.
- [terverifikasi] Pada basis SIMPLE, modal per barang tidak dipakai untuk menghitung laba; tes domain memastikan adanya baris tanpa modal tidak menahan laba SIMPLE. Bulan lain tanpa baris tersebut tidak ditandai.
- [terverifikasi] `OneNumberTest.unknownSaleCostHidesProfitOnHomeReportAndWorkbook` memeriksa hasil Room, Beranda, laporan, kartu BEP, teks penjelasan serta absennya laba pada XML xlsx, dan PDF berhasil dibuat dengan header PDF. Golden Bu Sri yang modalnya diketahui tetap lulus. PDF belum diperiksa secara visual atau diekstrak untuk memastikan isi seluruh halamannya.
- [terverifikasi] `:app:testDebugUnitTest` 194/194 dan `:app:lintDebug` berhasil; assemble debug/androidTest berhasil. Pengujian instrumentasi memakai AVD sintetis `emulator-5558`.

## Batas

- Kasus kategori lama tanpa ID yang ambigu atau tidak cocok belum ditandai dan masih dapat membuat belanja dihitung tidak sesuai maksud. PK-1.2 tetap checkpoint; jangan menyimpulkan seluruh angka finansial siap rilis.
- Penjualan dengan modal tidak diketahui belum dilengkapi alur untuk mengisi modal historis dari peringatan; seluruh kombinasi edit, diskon, ekspor PDF, backup dan sync belum diverifikasi. Format sheet utama impor tidak diubah; uji round-trip khusus dengan penjualan tanpa modal belum dilakukan.
- Hasil ini menutup salah satu paparan data `costKnown` yang disebut dalam `PK-1.2-laporan.md`, bukan seluruh syarat PK-1.2, PK-1.6, atau MVP.
