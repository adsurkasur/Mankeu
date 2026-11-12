# PK-1.2 — checkpoint perubahan schema

**Status: berhenti di PK-1.2.** Perubahan ini belum siap dirilis atau dinyatakan selesai. Integrasi perilaku produk, laporan, dan rencana masih memerlukan paket berikutnya dan keputusan K8. [terverifikasi]

## Perubahan

- Schema tetap 14. Kolom baru untuk jenis usaha dan produk, stok, biaya manual, susut, isi/metode pembayaran, serta nama dan status biaya baris nota ditambahkan pada entity, migrasi 13→14, dan schema Room. `productId` baris nota dapat kosong. [terverifikasi]
- Backup lama yang sudah bernomor 14 dinormalisasi dengan nilai bawaan tanpa menimpa nilai yang ada. Konversi backup 13 membawa nama dan status biaya baris lama. [terverifikasi]
- Edit transaksi mempertahankan isi dan metode pembayaran. Edit metadata nota mempertahankan snapshot baris dan `costKnown`; nota dengan baris bebas ditolak dengan alasan yang terlihat sampai editor mendukungnya. [terverifikasi]
- Perhitungan biaya resep memakai hasil bersih setelah susut. Biaya manual yang sudah tersimpan digunakan untuk penjualan produk/jasa yang didukung; sumber biaya yang tidak diketahui ditandai `costKnown=false`. Penjualan barang dagangan tanpa resep ditolak dengan alasan eksplisit sambil menunggu K8. [terverifikasi]
- Sync membawa kolom baru dan baris bebas; filter stok hanya mengambil baris yang memiliki produk. [terverifikasi]

## Bukti pemeriksaan

- Uji instrumentasi terarah: **37/37 lulus**, mencakup migrasi 13→14, backup terenkripsi schema 14 lama, perubahan nota dan transaksi, sync, serta alur editor. Hasil `:app:connectedDebugAndroidTest`, Medium_Phone, 12 November 2026. [terverifikasi]
- Sebelum perbaikan, uji terarah membuktikan isi transaksi hilang, HPP susut menghasilkan 6.000 alih-alih 6.316, dan edit baris bebas tidak ditolak; setelah perbaikan skenario tersebut lulus. Pengujian backup lama dan migrasi juga gagal sebelum perubahan lalu lulus sesudahnya. [terverifikasi]
- Build, lint, dan uji JVM pernah lulus sebelum perubahan terakhir pada snapshot dan biaya manual; suite tersebut belum dijalankan ulang setelah perubahan terakhir. Kompilasi dan 37 uji instrumentasi terakhir lulus. [terverifikasi]
- APK lama dipasang kembali dan empat berkas data asli emulator dibandingkan satu per satu dengan cadangan privat berdasarkan SHA-256; semuanya cocok. Pemeriksaan tar gabungan sempat melewatkan berkas shm, sehingga hasil pemulihan didasarkan pada pemeriksaan langsung setiap berkas. Tidak ada data asli yang dimasukkan ke repo. [terverifikasi]
- Pemilik menyampaikan jank sudah tidak terlihat dalam pengalamannya; angka emulator sebelumnya bersifat historis dan bukan pengukuran baru pada HP. [user-confirmed]

## Penghalang dan batas pemeriksaan

- `costKnown=false` belum diteruskan secara aman ke `MonthlySummary`, laporan, dan ekspor; angka laba dapat tetap tampak lengkap saat biaya belum diketahui. Jangan rilis hasil ini sebagai angka final. [terverifikasi]
- `PlanAssembly` masih memakai biaya resep dan dapat salah untuk biaya manual. Stok dan produksi belum sepenuhnya menghormati `SERVICE`/`tracksStock=false`; integrasi PK-1.3/1.4/1.6 masih diperlukan. [terverifikasi]
- Editor belum dapat mengubah nota yang memuat baris bebas; penyimpanan ditolak untuk melindungi data sampai dukungan editor tersedia. Kebijakan biaya barang dagangan menunggu K8. [terverifikasi]
- Saat checkpoint awal, upgrade APK nyata dari instalasi schema 13 belum dijalankan; lihat hasil lanjutan di bawah. Kompatibilitas sync dua arah dengan build lama belum dibuktikan; jangan menyimpulkan bahwa build lama aman menerima schema baru. [belum]
- Pengujian menyeluruh semua layar, seluruh kombinasi tipe usaha, laporan dan ekspor belum dilakukan. [belum]

## Lanjutan 13 November 2026

- APK dari arsip main schema 13 dibangun dan dipasang pada AVD sintetis terpisah `Mankeu_PK12_Proof`; instrumentasi seed memakai database aplikasi sebenarnya dan memeriksa versi 13. Seed: belanja bahan Rp50.000, produksi 10 bungkus, penjualan Rp80.000, HPP resep Rp50.000. Beranda lama menunjukkan kas Rp30.000 dan untung/rugi kas Rp30.000. [terverifikasi]
- APK branch dipasang di atasnya dengan `install -r`, aplikasi terbuka, database menjadi versi 14, `integrity_check` berhasil dan `foreign_key_check` kosong. Nota hasil migrasi bernama Keripik, HPP Rp50.000, `costKnown=1`; usaha lama `ITEM_COST`. Beranda baru menunjukkan uang di tangan Rp30.000 tetapi untung bulan ini **-Rp20.000**, sehingga pemeriksaan kesamaan angka belum lulus. [terverifikasi]
- Pada seed pertama, belanja tercatat dengan `categoryId=null` dan nama kategori bebas; biaya Rp50.000 tidak memiliki penanda `includedInHpp`, sedangkan HPP nota juga Rp50.000. Keduanya masuk perhitungan laba, sesuai selisih yang diamati. Perilaku ini tetap perlu ditinjau untuk data lama tanpa kategori; kesesuaian dengan aturan produk belum diputuskan. [terverifikasi]
- Putaran kedua memakai seed yang sama, tetapi pembelian menunjuk ID kategori bawaan `Bahan Baku & Stok`. Beranda schema 13 menampilkan kas dan untung/rugi kas masing-masing Rp30.000; setelah `install -r` APK branch, Beranda schema 14 menampilkan uang di tangan dan untung bulan ini masing-masing Rp30.000. Ini memenuhi golden dengan kategori baku, tetapi tidak menyelesaikan selisih seed pertama. [terverifikasi]
