# PK-1.3 — Baris nota bebas

**Status: selesai pada lapisan domain dan repository; layar untuk membuatnya belum tersedia.** Schema 14 sudah menyediakan `sale_lines.productId` nullable, sehingga tidak ada perubahan schema pada paket ini. [terverifikasi]

## Perilaku yang dibuat

- `SaleLineDraft` menerima `productId=null` dan nama baris. Repository memangkas nama, menolak nama kosong, jumlah tidak valid/nol, dan harga nol. Isi satuan disimpan sebagai `1`; satuan boleh kosong. HPP baris bebas Rp0 dan diketahui (`costKnown=true`). [terverifikasi]
- Nota bebas dan campuran memakai jumlah, diskon, pembayaran, piutang, dan `MonthlySummary` yang sama seperti nota berproduk. HPP hanya berasal dari baris produk. [terverifikasi]
- Pembacaan penjualan untuk stok sudah memfilter `productId IS NOT NULL`; tes membuktikan penjualan bebas tidak mengurangi produk. Jalur titipan menolak baris tanpa produk. [terverifikasi]
- Nota dengan baris bebas tetap tidak bisa diubah melalui editor lama. Guard di UI dan repository menolak perubahan dengan alasan; tes membuktikan snapshot baris, pembayaran, dan catatan tetap sama. [terverifikasi]
- Workbook sekarang selesai dibuat tetapi tidak memuat detail baris nota bebas karena kontrak ekspor lengkap ada di PK-4.1. Repository menghitung baris yang tertinggal dan ViewModel menampilkan pemberitahuan setelah file ditulis. Data nota tetap tersimpan di aplikasi. [terverifikasi]

## Pemakai `productId` yang ditinjau

- `FinanceRepository.saveSale` / `noteLine` / `writeSale`: ID nullable, validasi nama serta hitungan, snapshot nama, HPP Rp0 untuk jasa bebas; baris berproduk tetap memakai perhitungan modal yang ada. [terverifikasi]
- `FinanceDao.observeProductSaleLines`: hanya membaca baris yang memiliki produk; agregat sales untuk laba tetap menghitung semua total dan HPP nota. [terverifikasi]
- `FinanceRepository` titipan, `ConsignmentLineEntity`, dan `requireWithinConsignment`: membutuhkan produk dan menolak baris bebas, karena sisa titipan dihitung per produk. [terverifikasi]
- `UnifiedLedger` dan layar detail nota: nama produk dipakai bila ada; untuk baris tanpa produk dipakai snapshot `name`. Form edit lama tetap nonaktifkan Simpan untuk nota tersebut. [terverifikasi]
- `BackupUpgrade`/sinkronisasi: bentuk schema 14 sudah membawa kolom nullable dan tes sinkronisasi schema memuat baris dengan `productId=null`. Backup yang dibuat aplikasi menyimpan seluruh tabel. [terverifikasi]
- `FinanceWorkbookExporter`: kontrak saat ini menyusun transaksi dari financial events dan belum mengirim baris nota bebas; warning setelah ekspor menyebut detail yang tidak masuk. `MankeuTemplate` tidak diubah atau ditebak. [terverifikasi]
- `SalesMath`: ID tidak dipakai untuk matematika; subtotal, diskon, total dan status tetap rupiah `Long`, kuantitas desimal diparse oleh `TransactionCalculation`. [terverifikasi]

## Bukti uji

- RED: tes baru gagal kompilasi karena `SaleLineDraft` masih mengharuskan ID produk dan tidak memiliki nama baris. Setelah meluruskan konstruktor fixture, kegagalan hanya pada API yang memang belum tersedia. [terverifikasi]
- GREEN `FreeLineSaleTest`: **4/4 lulus** pada AVD kosong `Mankeu_PK12_Proof`. Meliputi nota bebas bayar sebagian, campuran barang+jasa dengan HPP hanya pada barang, nama wajib, dan penolakan edit tanpa kehilangan snapshot/pembayaran/catatan. Uji juga membuktikan stok tetap, satu angka `MonthlySummary`, ekspor workbook tidak crash, dan jumlah baris yang perlu diberitahukan terdeteksi. [terverifikasi]
- Regresi terarah `SalesFlowTest` **9/9**, `ImportCommitTest` **20/20** instrumentasi; `MonthlySummaryTest` **4/4**, `SalesMathTest` **8/8**, dan `BackupUpgradeTest` **5/5** JVM. [terverifikasi]
- `:app:lintDebug`: 0 error, 87 warning; `:app:assembleDebug` berhasil; `git diff --check` bersih. Lint dan build dijalankan sebelum penambahan satu tes guard edit; setelah tes tersebut, `FreeLineSaleTest` berjalan ulang 4/4. [terverifikasi]
- `Medium_Phone` tidak dipakai atau dihapus. Uji instrumentasi memakai AVD terpisah; tidak ada data pengguna di fixture. [terverifikasi]

## Yang belum selesai / batas bukti

- Layar yang memungkinkan pengguna memasukkan baris bebas belum dibangun; alur ini masih hanya tersedia di API repository. [belum]
- Penyunting nota dengan baris bebas sengaja ditolak sampai editor mendukung pengeditan tanpa kehilangan isian. [terverifikasi]
- Detail baris bebas belum dapat dibawa dalam workbook; warning baru terlihat setelah file selesai dibuat. Implementasi lossless menunggu PK-4.1. [terverifikasi]
- PDF saat ini adalah laporan turunan dan tidak menjadi ekspor baris nota; perilaku PDF tidak diuji khusus oleh paket ini. [belum]
- Integrasi `costKnown=false` ke `MonthlySummary`, laporan dan ekspor untuk baris produk bermodal belum diketahui tetap terbuka dari PK-1.2/PK-1.6; hasil finansial lintas semua kasus belum dinyatakan lengkap. [terverifikasi]
- Suite penuh belum dijalankan ulang; hanya tes terarah di atas yang menjadi bukti paket. [belum]
