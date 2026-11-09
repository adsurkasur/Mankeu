# Review U5: Perombakan Form (P2/P3/P4 & Fase 2 Task 5)

Dokumen review untuk Paket A (tinjauan perubahan form U5 pada branch `fase2-nota-piutang`, diff `50b3c7e..b1b865b`).

## 1. Verifikasi 8 Poin Wajib

| # | Poin Pemeriksaan | Status | Temuan / Bukti |
|---|---|---|---|
| 1 | Form dibuka dengan ≤ 4 isian terlihat (P2) | **Lulus** | Ditegaskan oleh tes `theDailySaleIsFiveTapsWithNoTyping` (0 kotak teks saat dibuka), `ProductEditorTest` (2 kotak saat dibuka: nama & hasil batch), dan `MoneyFormsTest` (Belanja 2 kotak, Bukan bahan 1 kotak, Uang masuk/keluar lain 2 kotak). |
| 2 | Penanganan uang & sen ("1.250.000,50" → 1.250.000) | **Lulus** | `sanitizeRupiah` membuang sen via regex `TRAILING_SEN` dan memfilter digit tanpa floating-point. Diverifikasi di `MoneyFieldInputTest` dan `FormComponentsTest`. |
| 3 | Terima bayar per pelanggan (`recordCustomerPayment`, `allocate`) | **Lulus** | Satu transaksi Room `database.withTransaction`, alokasi nota terlama dulu, nota sesudah tanggal bayar dikecualikan, lebih bayar ditolak, jurnal Kas dan Piutang seimbang. Diverifikasi di `SalesMathTest` dan `SaleNoteFlowTest`. |
| 4 | Kirim nota & pengingat (WhatsApp + cadangan ACTION_SEND) | **Lulus** | `ShareText` murni dan lulus tes JVM (`ShareTextTest`). Tautan `wa.me` divalidasi (>= 8 digit, normalisasi 08->628). `shareText` menangani `ActivityNotFoundException` lalu membuka chooser. Tanpa izin Android baru di `AndroidManifest.xml`. |
| 5 | Ubah catatan lama tidak menghilangkan field tersembunyi | **Lulus** | Di `MankeuApp.kt:676-686`, saat mengoreksi catatan lama (`Overlay.MONEY_FORM`), field yang tidak ditampilkan di form (`reference`, `budgetItemId`, `costNature`, `productId`, `usefulLifeMonths`) dipertahankan dari objek `existing`. |
| 6 | Editor produk: field rencana terisolasi & awet | **Lulus** | `showPlan = productFrom == PLAN`. Saat dibuka dari tab Produk, field target dan bulan penjualan tidak ditampilkan di UI namun nilainya tetap disimpan kembali ke draft dari objek `existing`. |
| 7 | Baris tanggal "Hari ini ›" aksesibel TalkBack (target sentuh ≥ 48 dp) | **Lulus** | `DateField` menggunakan `ListRowField` dengan `Role.Button` dan tinggi minimal `Space.Row` (56 dp >= 48 dp target M3). Diuji di `FormComponentsTest.theDateRowIsAButtonThatOpensThePicker`. |
| 8 | Penghapusan `EntryFormFallbackTest` & `EntrySheetSwipeTest` | **Lulus** | Bottom sheet form lama (~600 baris) dihapus digantikan `MoneyFormScreen` (halaman penuh). Pengujian perilaku sheet lama sudah tidak relevan. |

---

## 2. Temuan Review (Minor)

### M-1: Judul OverlayScaffold Menggunakan Bobot Lama (FontWeight.Black)
- **Lokasi**: `app/src/main/java/com/mankeu/presentation/MankeuApp.kt:976`
- **Tingkat**: Minor (Konsistensi Desain)
- **Deskripsi**: `OverlayScaffold` (yang dipakai oleh halaman Pelanggan/CustomerScreen dan overlay lainnya) masih menggunakan `title = { Text(title, fontWeight = FontWeight.Black) }`. Sedangkan sistem token desain baru (`FullPageForm`, dsb.) menggunakan `style = MankeuType.Title`.
- **Perbaikan**: Ganti `fontWeight = FontWeight.Black` dengan `style = MankeuType.Title`.

### M-2: Label Sheet Satuan Jual Mengulang Akhiran Suffix ("Isi (bungkus)")
- **Lokasi**: `app/src/main/java/com/mankeu/presentation/screens/ProductEditorScreen.kt:410` dan `strings.xml:561`
- **Tingkat**: Minor (Redundansi Label)
- **Deskripsi**: Field teks isi satuan jual menampilkan label `Isi (bungkus)` sekaligus suffix `bungkus`. Ini membuat kata satuan berulang dua kali dalam satu field (`[ 40 ] bungkus` dengan label "Isi (bungkus)").
- **Perbaikan**: Ubah string `editor_unit_content` menjadi "Isi" (EN: "Contains") tanpa format argumen satuan dasar, sehingga label adalah "Isi" dan suffix menampilkan satuan dasarnya. Perbarui tes terkait di `ProductEditorTest`.

### M-3: Petunjuk "Belum lunas? Pilih pelanggannya." Menggeser Tombol Pembayaran (Layout Jitter)
- **Lokasi**: `app/src/main/java/com/mankeu/presentation/screens/SaleNoteScreens.kt:301`
- **Tingkat**: Minor (Penyebab Jitter Layout)
- **Deskripsi**: Teks `sale_customer_needed` dirender kondisional dengan `if (needsCustomer)`. Saat pengguna memilih "Belum dibayar", teks muncul dan mendorong `SingleChoiceSegmentedButtonRow` ke bawah. Saat memilih pelanggan, teks hilang dan konten melompat ke atas.
- **Perbaikan**: Cadangkan ruang vertikal dengan membungkus teks dalam wadah berukuran minimal 1 baris Caption (`Modifier.fillMaxWidth().defaultMinSize(minHeight = 20.dp)`), sehingga segmen pembayaran di bawahnya tidak bergetar atau melompat.

---

## 3. Kesimpulan Review
Implementasi U5 memenuhi semua kriteria spec P2/P3/P4 dan Fase 2 Task 5 dengan kualitas tinggi. Semua 3 temuan minor di atas merupakan pembersihan "by eye" yang langsung diperbaiki pada putaran perbaikan Paket A dengan TDD.
