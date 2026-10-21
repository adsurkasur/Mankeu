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
- 📊 **Analisis Kelayakan Finansial Otomatis**: Menghitung Titik Impas (BEP unit & Rupiah), HPP per unit produk, Laba Kotor, Laba Bersih, hingga rasio kelayakan bisnis (Benefit-Cost Ratio).
- 💼 **Multi Usaha & Multi Proyek/Siklus**: Kelola berbagai entitas usaha (toko, kuliner, bengkel, jasa, pertanian) beserta siklus proyeknya dalam satu aplikasi dengan isolasi data independen.
- 📑 **Sinkronisasi Excel (.xlsx) Dua Arah**: Ekspor laporan lengkap bersertifikasi audit, atau impor pembukuan lama dengan rekonsiliasi cerdas dan jejak audit (*audit trail*).
- 🛡️ **Cadangan Terenkripsi AES-256 & Cloud Drive**: Ekspor cadangan terenkripsi ke file lokal atau sinkronisasikan secara privat ke Google Drive pribadi Anda.

---

## 📱 Galeri Antarmuka

| Beranda & Kas Realisasi | Perencanaan & Kelayakan Usaha | Tema Gelap Dinamis | Multi Unit Usaha |
| :---: | :---: | :---: | :---: |
| <img src="docs/playstore/screenshots/01_beranda_realisasi.png" width="220" /> | <img src="docs/playstore/screenshots/02_proyeksi_anggaran.png" width="220" /> | <img src="docs/playstore/screenshots/03_mode_gelap.png" width="220" /> | <img src="docs/playstore/screenshots/04_unit_usaha_aktif.png" width="220" /> |

---

## 🚀 Fitur Unggulan

### 1. Pencatatan Transaksi Komprehensif & Intuitif
- **Pemasukan & Pengeluaran Kas**: Catat transaksi uang masuk dan uang keluar harian secara cepat dengan kategorisasi terstruktur.
- **Piutang & Utang (Obligasi)**: Pantau tagihan yang belum diterima atau kewajiban yang belum dibayar, lengkap dengan riwayat pelunasan bertahap maupun pelunasan instan.
- **Setoran Modal & Penarikan**: Pisahkan modal awal pemilik dan kas operasional secara akurat.
- **Ubah dan Hapus**: Catatan yang salah isi diperbaiki langsung lewat **Ubah**; catatan yang tidak perlu dihapus. Jurnal ikut dibangun ulang sehingga laporan selalu seimbang (DEC-019).

### 2. Skenario Ganda: Mode Realisasi vs Mode Proyeksi
- **Mode Realisasi**: Rekam pembukuan riil yang telah terjadi di lapangan.
- **Mode Proyeksi (Plan)**: Rancang rencana keuangan sebelum modal dikeluarkan. Tetapkan target volume penjualan, estimasi harga jual satuan, dan modal kas awal.
- **Analisis Kelayakan Seketika**:
  - Estimasi Omzet Penjualan & Total Biaya
  - HPP (Harga Pokok Penjualan) per satuan unit
  - Titik Impas (BEP) dalam kuantitas produk dan nominal Rupiah
  - Net Profit Margin (%) & Benefit-Cost Ratio (BCR)

### 3. Laporan Keuangan Standar Eksekutif
- **Laba Rugi (Income Statement)**: Pantau omzet, beban pokok penjualan, beban operasional, dan laba bersih.
- **Laporan Arus Kas (Cash Flow)**: Kategorisasi otomatis aktivitas operasi, investasi, dan pendanaan.
- **Perbandingan Realisasi vs Rencana**: Bandingkan deviasi antara target rencana dan pencapaian aktual secara presisi.

### 4. Interoperabilitas Lembar Kerja Excel (.xlsx)
- **Ekspor Komprehensif**: Menghasilkan workbook Excel multi-lembar terstruktur (Transaksi Realisasi, Transaksi Proyeksi, Asumsi & Kelayakan, Laba Rugi, Arus Kas).
- **Template Pembukuan UMKM**: Buat dan bagikan template Excel resmi Mankeu yang kompatibel untuk langsung diisi dan diimpor.
- **Deteksi Cerdas & Rekonsiliasi**: Importer otomatis mencocokkan kolom tanggal, uraian, debit/kredit, dan kategori, serta memulihkan catatan tersirat dengan notifikasi bukti audit.

### 5. Keamanan & Cadangan Data
- **Offline-First Room DB**: Seluruh data tersimpan di SQLite lokal perangkat tanpa memerlukan koneksi server.
- **Enkripsi AES-256 GCM**: Cadangkan basis data ke berkas lokal dengan perlindungan kata sandi kuat.
- **Penyelarasan Google Drive Opsional**: Simpan cadangan terenkripsi ke folder aplikasi pribadi (`appDataFolder`) di Google Drive tanpa perantara server pihak ketiga.

### 6. Panduan Interaktif (Spotlight Guided Tour)
- Dilengkapi panduan sorotan (*spotlight tour*) kontekstual pada setiap layar untuk memandu pemilik usaha memahami fitur tanpa repot membaca buku panduan tebal.

---

## 🛠️ Arsitektur & Teknologi

Mankeu dibangun menggunakan standar rekayasa perangkat lunak modern Google Android:

| Komponen | Teknologi / Pustaka |
| :--- | :--- |
| **Bahasa Pemrograman** | Kotlin 2.0+ (100% Coroutines & StateFlow) |
| **Antarmuka (UI)** | Jetpack Compose, Material Design 3 (Dynamic Color, Light & Dark Theme) |
| **Arsitektur** | MVI / Clean Architecture (Domain, Data, Presentation) |
| **Basis Data Lokal** | Room ORM (SQLite) dengan migrasi skema teruji |
| **Aritmatika Uang** | Pure Integer Arithmetic (`Long` Rupiah — zero floating-point drift) |
| **Format Lembar Kerja** | Native OpenXML / ZIP OOXML Streaming Parser & Exporter |
| **Kriptografi** | Jetpack Security Crypto (AES-256-GCM) |
| **Cloud Sync** | Google Drive REST API v3 (Scoped `appDataFolder`) |
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
- **59+ Automated Unit & Golden Tests**: Menguji keabsahan saldo, penjurnalan ganda, analisis kelayakan, enkripsi cadangan, import/export round-trip, hingga ubah/hapus catatan dan pembatalan impor.
- **Strict Lint Compliance**: Tanpa error kompilasi dan bebas peringatan kritis.
- **Exact Rupiah Invariant**: Seluruh perhitungan nilai uang menggunakan bilangan bulat (`Long`), menjamin akurasi 100% tanpa risiko pembulatan desimal komputer.

---

## 📄 Lisensi & Privasi

- **Lisensi**: Proyek ini dilisensikan di bawah [GNU General Public License v3.0 (GPLv3)](./LICENSE).
  - Hak Cipta © 2026 adsurkasur <adsurkasur.dev@gmail.com>.
  - Anda bebas menggunakan, memodifikasi, dan mendistribusikan kode ini dengan syarat mencantumkan hak cipta asli dan merilis seluruh turunan kode di bawah lisensi GPLv3 yang sama.
- **Privasi**: Aplikasi ini beroperasi 100% *offline-first*. Tidak mengumpulkan, menjual, atau mentransmisikan data finansial pengguna ke server luar. Seluruh data keuangan berada sepenuhnya dalam kontrol lokal pengguna.
