# Rencana Implementasi: Penyempurnaan 11 Poin UI/UX & Standar Fungsional Arina Agri Calculator

Dokumen ini memuat rencana teknis untuk menuntaskan 11 poin masukan dari Anda terkait antarmuka, terminologi, animasi, splash screen, onboarding guide, dan aksesibilitas fitur impor/ekspor Excel.

---

## 1. Ringkasan 11 Poin Perbaikan

1. **State Kosong Usaha & Lahan:** Memberikan label dan aksi yang jelas (`"Belum ada usaha"`, `"Belum ada lahan"`, `"Pilih Usaha"`) saat belum ada data yang dibuat, bukan langsung menampilkan teks bawaan yang membingungkan.
2. **Perbaikan Tanda `+` Ganda:** Memperbaiki teks pada menu tambah usaha/lahan (`"Tambah Usaha"` dan `"Tambah Lahan"` alih-alih `"+ + Tambah Usaha"`).
3. **Kembalikan & Pertajam Akses Impor/Ekspor Excel:**
   - Menambahkan ikon cepat **Berkas & Impor/Ekspor** di *Scope Header* (Bilah Atas) di sebelah ikon Akun.
   - Menambahkan kartu **Impor & Ekspor Data Excel** di menu Pengaturan (*Settings*).
   - Menambahkan tombol ekspor cepat di halaman **Laporan**.
4. **Walkthrough / Spotlight Overlay (First-Timer Guide):**
   - Membuat komponen `SpotlightTour` berbasis Jetpack Compose dengan sorotan (spotlight) interaktif untuk mengenalkan:
     - Header & Mode Switcher (Proyeksi vs Realisasi).
     - Kartu Ringkasan Saldo & Donut Chart Biaya.
     - Navigasi Utama (Catat, Anggaran RAB, Laporan).
   - Menyimpan status di `AppPreferencesState` (`hasSeenTour`).
   - Menyediakan tombol **"📘 Panduan Penggunaan Aplikasi"** di menu Pengaturan agar bisa ditonton ulang kapan saja.
5. **Perbaikan Logo Splash Screen Terpotong:**
   - Menyesuaikan `inset` pada `ic_launcher_foreground_inset.xml` dari 20% menjadi 28% agar siluet daun & cabang sirkuit Arina tidak terpotong oleh *circular mask* bawaan Android 12+ SplashScreen API.
6. **Splash Screen Adaptive (Dukungan Dark Mode):**
   - Menambahkan folder dan file `res/values-night/themes.xml` dan `res/values-night/colors.xml` dengan warna latar belakang gelap `#121411` dan `windowLightStatusBar = false`.
7. **Animasi Lebih Halus (Smooth Transitions):**
   - Animasi geser & warna pada *Pill Switcher* Proyeksi vs Realisasi (`animateColorAsState`, `animateDpAsState`).
   - Animasi pergantian layar navigasi menggunakan `AnimatedContent` (*fade & horizontal slide*).
8. **Penyelarasan Istilah (Website Parity):**
   - Mengganti `"Rencana"` menjadi `"Proyeksi"` dan `"Kenyataan"` menjadi `"Realisasi"` pada seluruh string, badge, dan kartu.
   - Menyesuaikan label navbar Anggaran menjadi `"Anggaran"` / `"Proyeksi (RAB)"`.
9. **Kartu "Tentang Aplikasi" (*About*) di Pengaturan:**
   - Menampilkan logo Arina, penjelasan filosofi *offline-first*, agribisnis finansial, dan pengembang.
10. **Tampilan Versi Aplikasi di Pengaturan Bawah:**
    - Menampilkan informasi versi di baris terbawah Pengaturan (`Versi 1.1.0 • SemVer`).
11. **Standar SemVer pada Build:**
    - Memperbarui `versionCode = 2` dan `versionName = "1.1.0"` pada `app/build.gradle.kts`.

---

## 2. Rincian Perubahan Kode

### Komponen & Resources

#### [MODIFY] [`app/build.gradle.kts`](file:///E:/Managed%20Projects/Arina/arina-agri-calculator/app/build.gradle.kts)
- Mengubah `versionCode = 2` dan `versionName = "1.1.0"`.

#### [MODIFY] [`app/src/main/res/values/strings.xml`](file:///E:/Managed%20Projects/Arina/arina-agri-calculator/app/src/main/res/values/strings.xml)
- Memperbarui istilah:
  - `scope_mode_plan`: `"Proyeksi"` (dari `"Rencana"`)
  - `scope_mode_actual`: `"Realisasi"` (dari `"Kenyataan"`)
  - `nav_budget`: `"Anggaran"`
  - Menambahkan string untuk state kosong usaha/lahan, panduan tour, dan tentang aplikasi.

#### [MODIFY] [`app/src/main/res/drawable/ic_launcher_foreground_inset.xml`](file:///E:/Managed%20Projects/Arina/arina-agri-calculator/app/src/main/res/drawable/ic_launcher_foreground_inset.xml)
- Menyesuaikan nilai inset menjadi `28%` agar aman dari pemotongan lingkaran splash screen Android.

#### [NEW] `app/src/main/res/values-night/colors.xml`
- Menentukan `@color/splash_background` sebagai `#121411` untuk dark mode.

#### [NEW] `app/src/main/res/values-night/themes.xml`
- Menentukan konfigurasi tema gelap untuk splash screen (`Theme.ArinaAgriCalculator.Starting`).

#### [MODIFY] [`app/src/main/java/com/example/arinaagri/calculator/presentation/AppPreferences.kt`](file:///E:/Managed%20Projects/Arina/arina-agri-calculator/app/src/main/java/com/example/arinaagri/calculator/presentation/AppPreferences.kt)
- Menambahkan preferensi `hasSeenTour: Boolean` dan fungsi `updateHasSeenTour(Boolean)`.

#### [NEW] `app/src/main/java/com/example/arinaagri/calculator/presentation/components/SpotlightTour.kt`
- Komponen overlay tur interaktif langkah-demi-langkah dengan spotlight bulat/persegi dan kartu penjelasan yang memandu pengguna baru.

#### [MODIFY] [`app/src/main/java/com/example/arinaagri/calculator/presentation/ArinaCalculatorApp.kt`](file:///E:/Managed%20Projects/Arina/arina-agri-calculator/app/src/main/java/com/example/arinaagri/calculator/presentation/ArinaCalculatorApp.kt)
- Menambahkan tombol Berkas/Data di `ScopeHeader`.
- Memperbaiki label `addLabel` di `ChoiceMenu` (`"Tambah Usaha"`, `"Tambah Lahan"`).
- Menghubungkan tur interaktif `SpotlightTour` saat pertama kali buka atau saat dipanggil dari Pengaturan.
- Menambahkan animasi transisi halus pada pergantian layar (*AnimatedContent*) dan *Pill Switcher*.

#### [MODIFY] [`app/src/main/java/com/example/arinaagri/calculator/presentation/screens/AccountSettingsScreen.kt`](file:///E:/Managed%20Projects/Arina/arina-agri-calculator/app/src/main/java/com/example/arinaagri/calculator/presentation/screens/AccountSettingsScreen.kt)
- Menambahkan tombol Panduan Aplikasi (*Replay Tour*).
- Menambahkan kartu Kelola Berkas Excel & Cadangan.
- Menambahkan kartu *Tentang Aplikasi* dan label *Versi 1.1.0 (SemVer)* di bagian paling bawah.

---

## 3. Rencana Verifikasi

### Automated Tests
- Menjalankan unit test dan build APK:
  ```powershell
  .\gradlew.bat testDebugUnitTest assembleDebug
  ```

### Manual / Emulator Verification
- Memasang APK terbaru ke emulator:
  ```powershell
  .\gradlew.bat installDebug
  adb shell am start -n com.example.arinaagri.calculator/.MainActivity
  ```
- Mengambil tangkapan layar emulator untuk memvalidasi:
  - Tampilan splash screen (termasuk saat tema gelap).
  - Tampilan Spotlight Tour saat pertama kali aplikasi dibuka.
  - Dropdown Usaha/Lahan tanpa `+` ganda dan dengan state kosong yang jelas.
  - Menu Impor/Ekspor Excel di bilah atas dan di pengaturan.
  - Tampilan *Tentang Aplikasi* dan teks versi SemVer di Pengaturan.
