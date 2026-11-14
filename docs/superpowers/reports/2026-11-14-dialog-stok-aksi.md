# Dialog uang saat tambah stok — 2026-11-14 16:58 WIB

- [user-confirmed] Pemilik meminta tiga aksi yang jelas, margin dari tepi layar, dan mempertahankan isian serta perilaku pencatatan.
- [terverifikasi] Pada AVD sintetis emulator-5558, 1080 × 2400 px, 420 dpi, tema terang dan ukuran font sistem default, dialog memiliki ruang di kedua sisi. Sebelum perubahan, ketiga aksi berupa teks; sesudahnya, catat belanja menjadi tombol utama, stok saja tombol berbingkai, dan Kembali tetap aksi sekunder. Tidak ada data finansial nyata pada gambar.
- [terverifikasi] Lima tes instrumentasi mencakup perpindahan ke belanja dengan isian, stok saja tanpa uang, kembali dengan jumlah tetap, tiga baris aksi dan margin; semuanya lulus. Build debug/androidTest lulus.
- [belum] Pemeriksaan tema gelap, font besar, layar ringkas lain, TalkBack dan HP fisik. AVD tidak membuktikan hasil pada perangkat fisik.

Bukti: [sebelum](evidence/2026-11-14-dialog-stok-sebelum.png), [sesudah](evidence/2026-11-14-dialog-stok-sesudah.png).
