<p align="center">
  <img src="docs/playstore/feature_graphic_1024x500.png" alt="Mankeu Feature Graphic" width="100%" />
</p>

<p align="center">
  <img src="docs/playstore/app_icon_512x512.png" alt="Mankeu Icon" width="84" height="84" style="border-radius: 18px;" />
</p>

<h1 align="center">Mankeu — Manajemen Keuangan & Perencanaan Usaha UMKM</h1>

<p align="center">
  <strong>Aplikasi pembukuan dan analisis kelayakan finansial mobile-first, 100% offline-first, dan privat untuk pelaku UMKM.</strong>
</p>

<p align="center">
  <a href="https://github.com/adsurkasur/Mankeu/actions/workflows/android.yml"><img src="https://github.com/adsurkasur/Mankeu/actions/workflows/android.yml/badge.svg" alt="Android CI" /></a>
  <a href="https://github.com/adsurkasur/Mankeu/actions/workflows/deploy_play_store.yml"><img src="https://github.com/adsurkasur/Mankeu/actions/workflows/deploy_play_store.yml/badge.svg" alt="Play Store Deploy" /></a>
  <a href="https://github.com/adsurkasur/Mankeu/releases"><img src="https://img.shields.io/badge/Release-v0.0.6-2E7D32?style=flat-square" alt="Release v0.0.6" /></a>
  <img src="https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Platform Android" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose Material 3" />
  <img src="https://img.shields.io/badge/Architecture-Offline--First_%7C_Room-black?style=flat-square" alt="Offline-First" />
  <img src="https://img.shields.io/badge/Security-AES--256_Encrypted-green?style=flat-square" alt="AES-256 Encrypted" />
</p>

---

## 📌 Sekilas Tentang Mankeu

**Mankeu** adalah aplikasi Android native yang dirancang khusus untuk memecahkan problematika pencatatan keuangan pada Usaha Mikro, Kecil, dan Menengah (UMKM). Berbeda dengan software akuntansi konvensional yang rumit atau aplikasi kasir sederhana yang tidak memiliki analisis bisnis, Mankeu menggabungkan kemudahan pencatatan harian dengan akuntansi berstandar industri dan simulasi kelayakan usaha:

- 🔒 **100% Offline-First & Privat**: Beroperasi penuh tanpa butuh koneksi internet, tanpa kewajiban login, dan tanpa ketergantungan pada server pihak ketiga. Data keuangan Anda aman di genggaman Anda.
- ⚖️ **Dual-Book Scenarios (Proyeksi vs Realisasi)**: Memisahkan secara tegas antara rencana/simulasi bisnis (*Plan*) dan realisasi operasional (*Actual*), tanpa saling mencemari data.
- 📊 **Rencana Usaha & Titik Impas**: Susun produk beserta resep biaya per unit, biaya tetap bulanan, aset, dan modal rencana; Mankeu menghitung BEP unit dan Rupiah, laba per bulan, R/C, B/C, dan kebutuhan modal.
- 💼 **Multi Usaha & Multi Unit**: Kelola beberapa usaha (toko, kuliner, bengkel, jasa, pertanian), masing-masing dengan unit atau cabang yang datanya terpisah.
- 📦 **Stok Barang**: Barang per unit atau dipakai bersama semua unit, dengan peringatan stok menipis.
- 📑 **Templat Excel (.xlsx) Mankeu**: Templat kosong dan berkas ekspor memakai format yang sama, sehingga hasil ekspor dapat diimpor kembali tanpa kehilangan data.
- 🛡️ **Cadangan Terenkripsi AES-256**: Simpan cadangan terenkripsi ke berkas lokal atau ke Google Drive pribadi Anda.

---

## 📱 Galeri Antarmuka

| Beranda & Kas Realisasi | Perencanaan & Kelayakan Usaha | Tema Gelap Dinamis | Multi Unit Usaha |
| :---: | :---: | :---: | :---: |
| <img src="docs/playstore/screenshots/01_beranda_realisasi.png" width="220" /> | <img src="docs/playstore/screenshots/02_proyeksi_anggaran.png" width="220" /> | <img src="docs/playstore/screenshots/03_mode_gelap.png" width="220" /> | <img src="docs/playstore/screenshots/04_unit_usaha_aktif.png" width="220" /> |

---

## 🚀 Fitur Unggulan

### 1. Pencatatan Kas yang Sederhana
- **Uang Masuk & Uang Keluar**: Dua tombol saja. Kategori yang dipilih menentukan jenisnya: pemasukan, pengeluaran, setoran modal, ambil pribadi (prive), atau beli aset.
- **Kas murni**: Mankeu hanya mencatat uang yang benar-benar berpindah. Tidak ada piutang, utang, pinjaman, cicilan, atau bunga (DEC-011).
- **Ubah dan Hapus**: Catatan yang salah isi diperbaiki langsung lewat **Ubah**; catatan yang tidak perlu dihapus. Jurnal ikut dibangun ulang sehingga laporan selalu seimbang (DEC-019).
- **Produk dan kuantitas opsional**: Uang masuk dapat menyebut produk; jumlah mengikuti kuantitas × harga.

### 2. Dua Buku: Realisasi dan Proyeksi
- **Realisasi**: Catatan kas nyata yang sudah terjadi.
- **Proyeksi (Rencana Usaha)**: Periode rencana, produk dengan resep biaya per unit (beserta hasil per resep), bulan mulai dijual untuk produk musiman, biaya tetap bulanan, biaya dan pemasukan sekali, aset dengan umur pakai, serta setoran modal yang direncanakan.
- **Analisis Kelayakan Seketika**:
  - BEP gabungan berdasarkan bauran penjualan, plus BEP per produk
  - Laba per bulan, R/C ratio, dan B/C ratio
  - Kebutuhan modal dari defisit kas kumulatif terdalam
  - Status BEP bulan berjalan di buku Realisasi

### 3. Stok Barang
- Barang milik satu unit atau dipakai bersama semua unit usaha.
- Barang masuk, barang keluar, dan opname, dengan peringatan stok menipis dan nilai persediaan.

### 4. Laporan Keuangan
- **Laba Rugi dan Arus Kas**: Pemasukan, pengeluaran, laba, serta arus kas operasi, investasi, dan pendanaan, dengan grafik bulanan.
- **Kelayakan & BEP**: Rincian bulanan biaya tetap, biaya sekali, penyusutan, biaya variabel, pendapatan, laba, R/C, dan B/C.
- **Realisasi vs Target**: Rata-rata per bulan rencana dibandingkan dengan rata-rata per bulan realisasi, berbasis kas.
- **Ekspor PDF** untuk dicetak atau diarsipkan.

### 5. Templat Excel (.xlsx)
- **Satu format** (DEC-012): lembar Petunjuk, Transaksi, Rencana Anggaran, Produk, Resep Produk, Stok Barang, dan Kategori. Templat kosong dan ekspor memakai format yang sama.
- **Impor ketat**: Hanya templat Mankeu yang diterima. Berkas lain ditolak dengan daftar lembar atau kolom yang bermasalah dan tautan unduh templat. Tidak ada tebakan kolom.
- **Impor ke unit terpilih**: Kategori yang belum ada ditampilkan di pratinjau sebelum disimpan, dan impor berkas yang sama memunculkan peringatan duplikat.
- **Batalkan impor**: Menghapus semua catatan yang ditambahkan impor itu; riwayat impornya tetap tercatat.

### 6. Keamanan & Cadangan Data
- **Offline-First Room DB**: Seluruh data tersimpan di SQLite lokal perangkat tanpa memerlukan koneksi server.
- **Enkripsi AES-256-GCM**: Kunci diturunkan dari kata sandi dengan PBKDF2-HMAC-SHA256 310.000 iterasi.
- **Google Drive opsional**: Cadangan terenkripsi disimpan di folder aplikasi pribadi (`appDataFolder`); Drive hanya melihat ciphertext.

### 7. Panduan dan Pusat Bantuan
- Panduan per halaman **ditawarkan**, tidak dimulai sendiri, dan dapat dimatikan di Pengaturan (DEC-018).
- **Pusat Bantuan** berisi artikel berbasis tugas dengan pencarian, langkah bernomor, gambar, dan tombol **Tunjukkan di aplikasi**.

---

## 🛠️ Arsitektur & Teknologi

Mankeu dibangun menggunakan standar rekayasa perangkat lunak modern Google Android:

| Komponen | Teknologi / Pustaka |
| :--- | :--- |
| **Bahasa Pemrograman** | Kotlin 2.0+ (100% Coroutines & StateFlow) |
| **Antarmuka (UI)** | Jetpack Compose, Material Design 3 (Dynamic Color, Light & Dark Theme) |
| **Arsitektur** | Domain Kotlin murni, Room repository, satu `AppViewModel` dengan `StateFlow` |
| **Basis Data Lokal** | Room (SQLite), schema 11, migrasi teruji tanpa migrasi destruktif |
| **Aritmatika Uang** | Pure Integer Arithmetic (`Long` Rupiah — zero floating-point drift) |
| **Format Lembar Kerja** | Pembaca dan penulis OOXML sendiri (parser DOM platform, tanpa Apache POI) |
| **Kriptografi** | `javax.crypto` platform: PBKDF2-HMAC-SHA256 dan AES-256-GCM |
| **Cadangan Cloud** | Google Drive REST API v3 (`appDataFolder`); kerangka sinkronisasi multi-perangkat ada di `sync/`, belum tersambung ke UI |
| **Continuous Integration** | GitHub Actions (Build, Test, Lint, Auto-Release) |

---

## 💻 Membangun & Menjalankan Proyek (Build Guide)

### Prasyarat
- **JDK 17** atau yang lebih baru.
- **Android SDK Platform 36** dan Build-Tools 36.0.0.
- Android Studio Ladybug / Meerkat atau VS Code + Android Extension.

### Perintah Gradle

1. **Menjalankan Unit Test**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

2. **Menjalankan Linting**:
   ```bash
   ./gradlew lintDebug
   ```

3. **Membangun Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   *Berkas APK terletak pada `app/build/outputs/apk/debug/app-debug.apk`.*

4. **Membangun Release App Bundle (AAB)**:
   ```bash
   ./gradlew bundleRelease
   ```

---

## 🧪 Jaminan Kualitas (Quality Gates)

Setiap baris kode di repositori Mankeu mematuhi standar ketat:
- **Unit test JVM dan instrumented test**: Menguji keseimbangan jurnal, rencana usaha dan BEP, stok, enkripsi cadangan, round-trip templat Excel, migrasi skema, ubah/hapus catatan, dan pembatalan impor.
- **Strict Lint Compliance**: Tanpa error kompilasi dan bebas peringatan kritis.
- **Exact Rupiah Invariant**: Seluruh perhitungan nilai uang menggunakan bilangan bulat (`Long`), menjamin akurasi 100% tanpa risiko pembulatan desimal komputer.

---

## 📄 Lisensi & Privasi

- **Lisensi**: Proyek ini dilisensikan di bawah [GNU General Public License v3.0 (GPLv3)](./LICENSE).
  - Hak Cipta © 2026 adsurkasur <adsurkasur.dev@gmail.com>.
  - Anda bebas menggunakan, memodifikasi, dan mendistribusikan kode ini dengan syarat mencantumkan hak cipta asli dan merilis seluruh turunan kode di bawah lisensi GPLv3 yang sama.
- **Privasi**: Aplikasi ini beroperasi 100% *offline-first*. Tidak mengumpulkan, menjual, atau mentransmisikan data finansial pengguna ke server luar. Seluruh data keuangan berada sepenuhnya dalam kontrol lokal pengguna.
