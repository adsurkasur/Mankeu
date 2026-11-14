# Checkpoint kategori lama dan contoh validasi — 2026-11-14 14:50 WIB

## Tambahan 2026-11-14 15:56 WIB

- [terverifikasi] Migrasi 13→14 menjaga nilai, label, ID kategori null dan tautan stok pada dua belanja sintetis. Tes terpisah mengoreksi kategori melalui Catatan: ketidakpastian ringkasan hilang, uang keluar tetap Rp50.000 dan tidak ada transaksi ganda. Kedua tes lulus pada emulator-5558.
- [belum] Migrasi sampai koreksi pada database yang sama, hasil visual laporan/PDF, backup/sync dan kesiapan produksi.


## Hasil

- [terverifikasi] Belanja lama tanpa ID kategori yang bukan pelunasan ditandai tidak pasti meskipun nama cocok. Pada ITEM_COST laba dan BEP ditahan dari layar dan laporan; SIMPLE tetap menghitung laba kas, tetapi BEP ditahan bila sifat biaya belum pasti. Transaksi dan jumlah uang tidak dihapus atau diubah oleh penanda ini.
- [terverifikasi] Saat mengubah pengeluaran lama tanpa ID, pengguna harus memilih kategori secara eksplisit. Pemilih ditampilkan juga pada belanja yang terhubung bahan; tes memeriksa jumlah, harga dan stok tetap. Tes repository memeriksa bahwa menetapkan ID menghilangkan ketidakpastian tanpa menggandakan uang keluar. Belum ada bukti perjalanan koreksi lengkap melalui Catatan pada database hasil upgrade.
- [terverifikasi] Kuantitas gerakan stok dan nama kategori: pesan muncul setelah kolom kosong ditinggalkan, hilang ketika valid, dan percobaan Simpan yang kosong mengarahkan fokus ke kuantitas. Dua kolom ini adalah contoh awal DEC-042; form lain belum seluruhnya diubah.
- [terverifikasi] Pilihan stok menggunakan kontrol tersambung, tombol tambah satuan berada di samping dropdown dengan nama aksesibilitas, kategori memakai Tetap/Variabel, penjelasan modal barang berada pada pilihannya, dan baris kategori dipisahkan. Pilihan biaya tersimpan diuji pada data sintetis.
- [terverifikasi] `ScreenLanguageTest` 8/8: pengecualian istilah “Variabel” hanya pada dua resource kategori yang dipilih pemilik; penggunaan di label lain tetap terdeteksi. String English terkait titik impas tidak lagi memakai istilah terlarang pada layar.

## Bukti

- [terverifikasi] AVD sintetis yang dipakai `emulator-5558`, Android 11, 1080 × 2400 px/420 dpi; emulator pengguna `emulator-5556` tidak digunakan. Tes instrumentasi gabungan 7/7 mencakup tiga kasus kategori, tiga kasus gerakan stok dan peringatan kategori lama di layar/xlsx. Tes kuantitas stok dan komponen sheet 5/5; suite `MoneyFormsTest` 8/8. Tes terarah empat kasus koreksi kategori, belanja baru dan repository lulus 4/4. Beberapa kasus beririsan dan tidak dijumlahkan sebagai tes unik.
- [terverifikasi] JVM `:app:testDebugUnitTest` 196 tes, 0 gagal; `:app:lintDebug`, `:app:assembleDebug` dan `:app:assembleDebugAndroidTest` berhasil. `git diff --check` lulus dengan peringatan normalisasi newline Git.
- [belum] Uji pengguna dan HP fisik; font 200%, tablet dan TalkBack; PDF visual; pemulihan backup/sinkron build lama; upgrade APK 13→14 dengan kategori bebas dan seluruh alur koreksi melalui layar; seluruh kombinasi data dan seluruh instrumentasi. Status PK-1.2 dan MVP tetap checkpoint.

## Lanjutan

1. Perluas validasi DEC-042 ke form dan field lain: status belum disentuh, blur, koreksi, Simpan ke kesalahan pertama, keyboard dan fokus; jangan menganggap implementasi dua kolom sebagai aturan global yang sudah selesai.
2. Selesaikan inventaris dan perbandingan visual Produk, Pengaturan serta Pusat Bantuan, lalu kerjakan kelompok layar menurut `docs/superpowers/plans/2026-11-13-konsistensi-antarmuka.md`.
3. Buktikan koreksi kategori lama melalui Catatan sampai angka di Beranda/laporan/ekspor dan ulangi upgrade 13→14, backup dan sync sesuai gerbang. Nama kategori tidak membuktikan identitas historis.
