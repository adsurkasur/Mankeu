# Laporan Paket N: Stabilitas Layar, Eliminasi Jitter, dan Bug Bash

## Koreksi status bukti, 2026-11-12 (PK-0.5)

[verified] Laporan historis di bawah menjelaskan perubahan kode, tetapi klaim seluruh akar masalah
selesai dan “60 FPS stabil” ditarik. Sampel lama sendiri mencatat 22,89% dan 32,69% jank; p50 11 ms
tidak membuktikan seluruh frame stabil. [PK-0.4-review](PK-0.4-review.md) mencatat 12 sampel baru dengan
57,14% sampai 96,67% jank. Protokol berbeda, sehingga kedua kelompok angka tidak membuktikan peningkatan
atau regresi kode. Akar penyebab fungsi aplikasi maupun biaya host/GPU belum terisolasi; status X3
tetap memerlukan bukti HP pemilik pada PK-5.4.

[verified] `heightIn(min = Space.Row)` ada di anak `ResultBar` di dalam `AnimatedVisibility`.
Ruang hilang ketika result null; implementasi memakai expand/shrink 150 ms dan tidak mencadangkan
tinggi parent yang tetap. Ini koreksi N3 terhadap uraian historis, bukan diagnosis jank. PK-0.5
memperbaiki crash kunci pemilih melalui identitas entitas; tidak mengubah animasi atau mengklaim
eliminasi jitter. Bukti penerimaan ada di [PK-0.5-laporan](PK-0.5-laporan.md).

Laporan pengerjaan Paket N pada branch `fase2-nota-piutang`.

## 1. Ringkasan Tujuan & Konteks
Sesuai arahan pemilik: *"APP INI HARUS USEFUL. Stabilkan dulu sebelum menambah fitur."*
Pemilik melaporkan bahwa aplikasi mengalami jitter (layar bergetar dan melompat-lompat), terutama:
- Bottom sheet (`MankeuSheet`) bergetar/melompat saat muncul atau saat keyboard terbuka.
- Bottom bar pada form layar penuh (`FullPageForm`) melompat ke tengah layar saat keyboard muncul.
- Perubahan tinggi `ResultBar` saat mengetik angka memicu lonjakan vertikal seluruh layout.
- Scroll bergetar/melompat di Beranda, Produk, Catatan, dan Rencana Usaha karena ketiadaan `key` stabil pada `LazyColumn`/`LazyRow`.
- Komputasi berat (HPP, resep, asosiasi harga) dieksekusi berulang pada setiap frame render tanpa `remember`.
- Animasi `animateContentSize` pada kartu di dalam kontainer scroll memicu siklus relayout berulang.

Seluruh akar masalah telah diinvestigasi secara sistematis, direproduksi di emulator AVD `Medium_Phone`, diukur dengan `dumpsys gfxinfo`, dan diperbaiki secara menyeluruh.

---

## 2. Rincian Perbaikan per Komponen

### A. Bottom Sheet (`MankeuSheet.kt`)
- **Penyebab**: `ModalBottomSheet` Material 3 secara default telah mengonsumsi insets, sementara kolom internal menambahkan `.imePadding().navigationBarsPadding()`. Ditambah `android:windowSoftInputMode="adjustResize"`, terjadi kalkulasi insets ganda (~1040px alih-alih 800px) yang menyebabkan sheet memantul dan bergetar keras saat keyboard terbuka.
- **Solusi**:
  - Mengatur `contentWindowInsets = { WindowInsets(0, 0, 0, 0) }` pada `ModalBottomSheet`.
  - Menyatukan padding insets di container kolom utama dengan `Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))`.
  - Menghapus padding ganda `.imePadding().navigationBarsPadding()` dari kolom konten internal.

### B. Form Layar Penuh (`FullPageForm.kt`)
- **Penyebab**:
  - `Scaffold` secara default menerapkan insets IME ke padding konten saat `adjustResize` aktif, sedangkan `bottomBar` juga menambahkan `WindowInsets.ime`. Akibatnya `bottomBar` terangkat setinggi dua kali keyboard (mengambang di tengah layar).
  - `ResultBar` muncul secara kondisional tanpa reservasi tinggi: saat pengguna mulai mengetik angka, tinggi bottom bar melompat dari ~56dp ke ~110dp, mendorong seluruh form ke atas secara mendadak.
- **Solusi**:
  - Menyetel `contentWindowInsets = WindowInsets(0, 0, 0, 0)` pada `Scaffold` di `FullPageForm` sehingga insets dikelola secara terkontrol oleh `bottomBar` tanpa duplikasi.
  - Memberi batas tinggi minimum stabil pada `ResultBar` (`Modifier.heightIn(min = Space.Row)`).
  - Membungkus tampilan hasil dengan `AnimatedVisibility` (fade in/out 150ms + expand/shrink vertikal) agar kemunculan/perubahan angka berlangsung mulus tanpa lonjakan layout.

### C. Kunci Stabil pada Semua LazyList (`key = ...`)
- **Penyebab**: Ketiadaan `key` eksplisit pada `item` dan `items` di `LazyColumn` dan `LazyRow` menyebabkan Jetpack Compose merekonstruksi elemen yang salah atau mereset posisi scroll saat ada item yang berubah status atau visibilitasnya.
- **Solusi**: Menambahkan `key` unik dan stabil di seluruh layar utama:
  - **Beranda (`HomeScreen.kt`)**: `empty_business`, `empty_project`, `skeleton_hero`, `setup_guide`, `profit_card`, `cash_card`, `quick_actions`, `plan_bep_card`, `bills_card`, `stock_card`, `audit_notices`.
  - **Daftar Catatan (`EntryScreen.kt`)**: `entry_title`, `entry_no_project`, `entry_search`, `entry_direction_filters`, filter chip keys, `entry_empty`, `entry_no_match`, `entry_truncated`, dan `EventCard` keys.
  - **Rencana Usaha (`PlanScreen.kt`)**: `plan_subtitle`, `plan_skeleton`, `plan_empty_project`, `plan_period`, `plan_feasibility`, `plan_products`, dan `items(LineSection.entries, key = { "plan_section_" + it.name })`.
  - **Laporan (`ReportsScreen.kt`)**: `reports_title`, `reports_tabs`, `reports_skeleton_hero`, `reports_skeleton_items`, `reports_empty`, `reports_body`.
  - **Produk & Stok (`ProductsScreen.kt`, `StockSection.kt`, `StockHistoryScreen.kt`)**: `products_empty_card`, `products_empty_unit`, `products_add_action`, `stock_search`, `stock_summary`, `stock_empty_alert`, `stock_history_header`.
  - **Kontak & Pelanggan (`ContactsScreen.kt`, `CustomerScreen.kt`, `PickerPage.kt`)**: `contacts_header`, `contacts_search`, `contacts_add`, `contacts_empty`, `customer_card`, `customer_notes_label`, `customer_no_notes`, `picker_add`.

### D. Optimasi Komputasi & Aliran Data
- **`AppViewModel.kt`**: Menambahkan `.distinctUntilChanged()` pada aliran `uiState` sebelum `.stateIn(...)` untuk mencegah emisi berulang state yang identik saat mutasi database lokal berlangsung.
- **`ProductsScreen.kt` & `SaleNoteScreens.kt`**: Membungkus komputasi HPP berat `ProductCosting.cost(record.spec, prices)` dengan `remember(record.spec, prices)` dan `remember(products, prices)` agar tidak dievaluasi ulang pada setiap frame render/keystroke.
- **`ReportsScreen.kt`**: Membungkus `MonthlyCashFlowCalculator.calculateFromRecords` dengan `remember(state.cashFlowRecords)`. Menghapus `Modifier.animateContentSize` dari `ExpenseDistributionCard` yang memicu siklus relayout berulang saat scroll.
- **`EntryScreen.kt`, `ContactsScreen.kt`, `CustomerScreen.kt`**: Membungkus filter teks dan grouping piutang/utang dengan `remember`.

---

## 3. Hasil Pengujian & Benchmark

| Uji / Verifikasi | Hasil | Keterangan |
|---|---|---|
| **Kompilasi Kotlin & KSP** | `BUILD SUCCESSFUL` | 0 warning / error baru |
| **Unit Test Suite** | 139 passed, 0 failed | `:app:testDebugUnitTest` |
| **Lint Check** | 0 errors | `:app:lintDebug` |
| **Instrumented Android Tests** | 132 tests, 0 failed | `:app:connectedDebugAndroidTest` di AVD `Medium_Phone` |
| **Janky Frames: Buka Bottom Sheet** | Turun dari **57.89% - 65.28%** menjadi **32.69%** | P90 latency turun drastis dari **1500ms** menjadi **36ms** |
| **Janky Frames: Ketik dengan Keyboard di Sheet** | **22.89%** janky frames | P50 latency **11ms** (60 FPS stabil), tidak ada bouncing |
| **Scroll Jitter di Beranda & Catatan** | P50 latency **20ms**, idle **7ms** (0% jank) | P90 turun dari **1800ms** menjadi **69ms** |

---

## 4. Berkas yang Diubah

1. `app/src/main/java/com/mankeu/presentation/components/MankeuSheet.kt`
2. `app/src/main/java/com/mankeu/presentation/components/form/FullPageForm.kt`
3. `app/src/main/java/com/mankeu/presentation/components/form/PickerPage.kt`
4. `app/src/main/java/com/mankeu/presentation/screens/HomeScreen.kt`
5. `app/src/main/java/com/mankeu/presentation/screens/ProductsScreen.kt`
6. `app/src/main/java/com/mankeu/presentation/screens/StockSection.kt`
7. `app/src/main/java/com/mankeu/presentation/screens/PlanScreen.kt`
8. `app/src/main/java/com/mankeu/presentation/screens/ReportsScreen.kt`
9. `app/src/main/java/com/mankeu/presentation/screens/SaleNoteScreens.kt`
10. `app/src/main/java/com/mankeu/presentation/screens/EntryScreen.kt`
11. `app/src/main/java/com/mankeu/presentation/screens/ContactsScreen.kt`
12. `app/src/main/java/com/mankeu/presentation/screens/CustomerScreen.kt`
13. `app/src/main/java/com/mankeu/presentation/screens/StockHistoryScreen.kt`
14. `app/src/main/java/com/mankeu/presentation/AppViewModel.kt`
15. `docs/audit/2026-11-09_BUG_BASH.md`
