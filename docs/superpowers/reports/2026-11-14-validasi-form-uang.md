# Validasi form uang — 2026-11-14 15:04 WIB

- [terverifikasi] Jumlah uang, kuantitas belanja bahan dan harga satuan menampilkan pesan pada kolom setelah kehilangan fokus, lalu menghapusnya saat valid. Simpan dengan kolom tersebut invalid mengarahkan fokus ke kolom pertama; nilai dan alur simpan tidak diubah. Kondisi lain tetap memakai alasan umum.
- [terverifikasi] Tiga tes baru gagal sebelum perbaikan dan lulus setelahnya. Seluruh `MoneyFormsTest` lulus 11/11 pada AVD sintetis `emulator-5558`; tes mencakup penyimpanan dan koreksi kategori. `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:testDebugUnitTest` dan `:app:lintDebug` lulus; JVM 196/196.
- [belum] Seluruh form, kondisi lintas kolom, audit visual seluruh layar, aksesibilitas, HP fisik, upgrade 13→14 melalui UI, backup/sync dan seluruh gerbang MVP. Hasil ini tidak menyatakan aplikasi siap produksi.
