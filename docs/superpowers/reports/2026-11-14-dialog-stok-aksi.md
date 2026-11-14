# Dialog uang saat tambah stok — 2026-11-14 16:58 WIB

## Tambahan 2026-11-14 17:06 WIB

- [terverifikasi] Pada 720×1600/360 dpi (320 dp) dan font 130%, pilihan gerakan sebelumnya tidak sama tinggi dan teks terpotong; tinggi kini mengikuti skala font dan tanda pilihan tetap ditunjukkan latar. Aksi dialog dipendekkan menjadi Catat belanja dan Tambah stok saja. Tiga tes segmen/gerakan dan lima tes dialog lulus; tes beririsan.
- [terverifikasi] [Pilihan sebelum](evidence/2026-11-14-stok-ringkas-sebelum.png), [pilihan sesudah](evidence/2026-11-14-stok-ringkas-sesudah.png), [dialog sesudah](evidence/2026-11-14-dialog-ringkas-sesudah.png). AVD dikembalikan 1080×2400/420 dpi/font 100%.
- [belum] Font 200% ditolak image Android 11 ini; tema gelap, TalkBack dan HP fisik belum diperiksa.


- [user-confirmed] Pemilik meminta tiga aksi yang jelas, margin dari tepi layar, dan mempertahankan isian serta perilaku pencatatan.
- [terverifikasi] Pada AVD sintetis emulator-5558, 1080 × 2400 px, 420 dpi, tema terang dan ukuran font sistem default, dialog memiliki ruang di kedua sisi. Sebelum perubahan, ketiga aksi berupa teks; sesudahnya, catat belanja menjadi tombol utama, stok saja tombol berbingkai, dan Kembali tetap aksi sekunder. Tidak ada data finansial nyata pada gambar.
- [terverifikasi] Lima tes instrumentasi mencakup perpindahan ke belanja dengan isian, stok saja tanpa uang, kembali dengan jumlah tetap, tiga baris aksi dan margin; semuanya lulus. Build debug/androidTest lulus.
- [belum] Pemeriksaan tema gelap, font besar, layar ringkas lain, TalkBack dan HP fisik. AVD tidak membuktikan hasil pada perangkat fisik.

Bukti: [sebelum](evidence/2026-11-14-dialog-stok-sebelum.png), [sesudah](evidence/2026-11-14-dialog-stok-sesudah.png).
