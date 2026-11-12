# PK-4.7 — Cadangan dan pindah HP

Tanggal kerja: 2026-11-13 (Asia/Jakarta). Paket tidak memiliki dependensi keputusan K.

## Hasil

- **[terverifikasi]** Dialog cadangan yang sudah ada kini menerangkan tiga langkah dalam bahasa sederhana: buat cadangan berpassword di HP lama, simpan berkas dan password terpisah, lalu pilih berkas untuk dipulihkan di HP baru. Format `.aabk` dan mekanisme enkripsinya tidak diubah. Teks tersedia dalam Bahasa Indonesia dan Inggris.
- **[terverifikasi]** Beranda kondisi tanpa usaha menyediakan CTA “Sudah pernah pakai Mankeu? Ambil data lama” yang diarahkan ke chooser backup yang sama.
- **[terverifikasi]** Tes instrumented `BackupMoveFlowTest.savedFileRestoresTheSameMonthlySummaryAndBuyerListAfterErase` lulus pada `Mankeu_PK12_Proof` (AVD sintetis): membuat data usaha, produk, nota dan pembeli; menghasilkan berkas terenkripsi, menghapus seluruh tabel lewat `eraseAllData`, memulihkan dari bytes berkas dengan password, lalu membandingkan `MonthlySummary` bulan Oktober 2026 dan daftar pembeli persis dengan keadaan awal.
- **[dilaporkan]** Dialog pulih yang ada memberi pratinjau sebelum konfirmasi dan menyimpan backup pemulihan otomatis dengan password yang sama; sesi ini tidak mengubahnya.
- **[belum]** Tes Compose khusus CTA dan teks bernomor dicoba, namun tidak stabil/tidak menemukan konten yang diharapkan pada fixture uji, sehingga tidak dipertahankan sebagai tes hijau. Navigasi end-to-end UI, pemilih berkas Android, `pm clear` lalu pulih dari berkas pada instal bersih, dua perangkat fisik, serta Drive belum terbukti. Langkah pemulihan dari layar pertama secara literal juga belum ada sebagai WelcomeScreen; saat ini CTA diletakkan pada empty state Beranda yang sudah ada. B-4.7 menyebut WelcomeScreen, sedangkan implementasi produk belum memilikinya. PK-2.1 perlu membawa CTA ini jika layar pertama baru dibangun.
- **[belum]** Tidak menjalankan seluruh suite, lint, ataupun verifikasi Drive. Tidak ada schema atau kode finansial yang disentuh.

## Perubahan

- `HomeScreen`: callback dan CTA pemulihan pada kondisi usaha kosong; `MankeuApp`: routing ke `DataFocus.BACKUP`.
- `values/strings.xml`, `values-en/strings.xml`: CTA dan petunjuk bernomor pada chooser.
- `BackupMoveFlowTest`: bukti restore round-trip dengan DB in-memory dan file cadangan terenkripsi.

## Verifikasi dan keterbatasan

Perintah terarah: `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mankeu.data.BackupMoveFlowTest` — **BUILD SUCCESSFUL**, 1 test lulus di `Mankeu_PK12_Proof`. Percobaan tes UI `HomeBackupEntryTest` gagal karena konten tidak ditemukan dalam fixture Compose/Activity; berkas percobaan dihapus dan kegagalan tidak dihitung sebagai bukti produk gagal maupun berhasil. Emulator data pengguna tidak dibersihkan. Tes backup menghapus hanya database Room in-memory yang dibuat oleh tes.

Pertanyaan produk yang belum diatur: apakah pada rancangan layar pertama PK-2.1 CTA “Ambil data lama” harus menjadi tombol terpisah atau masuk ke pilihan “Mulai/Pulihkan”? Pilihan tidak ditentukan di paket ini; untuk implementasi sekarang CTA ditempatkan sebagai tombol kedua pada empty state Beranda.
