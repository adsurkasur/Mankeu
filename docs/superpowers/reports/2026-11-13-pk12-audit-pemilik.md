# PK-1.2 dan audit pemilik: checkpoint 2026-11-13 11:21 WIB

## Status dan bukti

- [terverifikasi] Branch `fase2-nota-piutang` berawal di `5f81c45`; perubahan pada handoff dipertahankan. Perubahan ini merupakan checkpoint, bukan bukti PK-1.2 selesai atau siap rilis.
- [terverifikasi] Join percobaan pada handoff menggandakan Rp50.000 menjadi Rp100.000 ketika dua kategori dengan nama sama berbeda peran. Tes Room gagal sebelum perbaikan; query sekarang memakai ID bila tersedia, atau satu kandidat nama tepat setelah trim dan case-fold SQLite, pada usaha dan peran yang sesuai. Jika kandidat lebih dari satu, tidak ada join; transaksi tidak digandakan. Tes kategori cocok, lintas peran, dan ambigu lulus pada AVD terisolasi. SQLite `lower()` bukan case folding Unicode lengkap.
- [terverifikasi] Pencocokan nama adalah perkiraan, bukan bukti identitas historis; nama dan penanda kategori yang kemudian diubah dapat mengubah klasifikasi laporan. Transaksi tanpa kecocokan atau dengan kandidat ambigu masih diperlakukan sebagai biaya lain tanpa peringatan. Seed kategori bebas pada laporan upgrade belum dibuktikan benar. **Jangan anggap hasil ini lengkap atau siap rilis.** Diperlukan mekanisme yang menandai ketidakpastian, koreksi kategori dan pembuktian upgrade end-to-end; jangan menebak bahwa biaya tersebut pasti stok.
- [terverifikasi] Tes fokus awal lima kasus (dua kategori, ProductEditor, StockHistory, OneNumber) lulus 5/5 pada emulator-5558. StockHistory dan ProductEditor menggunakan label aktual. OneNumber menggulir tab dan mengecualikan label tab “Untung dan uang” dari pemeriksaan isi komparasi. Test kasus ambigu juga lulus; fixture manual cost serta tes lain dinilai terpisah dalam log sesi.
- [terverifikasi] JVM `:app:testDebugUnitTest` 193 tes, 0 gagal; `:app:lintDebug` dan assemble debug/androidTest berhasil. Hasil ini tidak menggantikan tes instrumentasi penuh dan upgrade seed bebas.
- [dilaporkan] Pemilik menyatakan belum ada rilis produksi dan memberi keleluasaan memilih pendekatan kategori; izin ini tidak membuktikan identitas kategori pada data historis.

## Audit bahasa

- [terverifikasi] Pilihan `en` tersimpan dipakai saat `attachBaseContext` sebelum perubahan; itu dapat menyebabkan English, tanpa membuktikan preferensi pada perangkat yang diamati pemilik. Pemeriksaan literal sederhana pada resource Indonesia dan `Text` Compose tidak menemukan kata English umum yang dicari; ini bukan inventaris menyeluruh.
- [terverifikasi] Untuk kondisi sementara, locale Activity dipaksa Indonesia dan pemilih bahasa disembunyikan. Terjemahan, kode pemilih, dan nilai preferensi yang tersimpan tetap ada sesuai V-4. Tes preferensi `en` memeriksa locale dan string `Kembali` tanpa menghapus preferensi tersebut. [belum] Semua layar, bahasa dari data pengguna, library sistem, aksesibilitas, dan HP nyata.
- [pertanyaan produk, belum ditetapkan] Saat bahasa Inggris diaktifkan kembali, apakah A) pulihkan otomatis pilihan tersimpan, atau B) tetap Indonesia sampai pengguna memilih lagi? Implementasi saat ini mempertahankan nilai agar kedua pilihan tetap mungkin.

## Audit dialog stok

- [terverifikasi] Pada AVD 320 × 640, sebelumnya Kembali dan Catat belanja sejajar, sedangkan Stok saja di bawah. Sesudah perubahan, dua pilihan pencatatan lalu Kembali berada pada tiga baris penuh. Data uji sintetis. Bukti: `evidence/2026-11-13-dialog-stok-sebelum.png` dan `evidence/2026-11-13-dialog-stok-sesudah.png`.
- [terverifikasi] Tes susunan gagal sebelum perubahan dan lulus setelahnya. Tes jalur belanja, stok saja, dan kembali mempertahankan fungsi sebelumnya, termasuk jumlah yang sudah diisi. [belum] Tablet, font 200%, TalkBack dan penggunaan di HP nyata.
- [keputusan UX] Kedua pilihan pencatatan dibuat sama lebar dan berurutan agar mudah dibandingkan; Kembali diletakkan setelah keduanya. Callback dan isian tidak diubah.

## Lanjutan

- Tuntaskan representasi dan peringatan biaya kategori lama yang tidak pasti hingga Beranda, laporan dan ekspor, lalu ulangi seed upgrade 13→14 termasuk nama bebas. Tinjau efek perubahan nama/atribut kategori dan isolasi usaha.
- Tinjau kembali PlanAssembly, stok menurut jenis, sinkronisasi dengan APK lama, backup dan uji migrasi menyeluruh sesuai rencana; jangan mengambil paket menunggu K-8 tanpa keputusan.
- Audit bahasa secara menyeluruh dan uji layout serta aksesibilitas pada variasi ukuran. `emulator-5556` tidak digunakan untuk instrumentasi atau penghapusan data. AVD khusus `Mankeu_Codex_20261113b` dipakai pada `emulator-5558` dan ditampilkan atas permintaan pemilik.
