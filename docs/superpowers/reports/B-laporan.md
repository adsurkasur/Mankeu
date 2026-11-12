# Laporan Paket B: Fase UX U6 — Satu Daftar Catatan Terintegrasi

## Koreksi bukti, 2026-11-12 (PK-0.5)

[verified] Uraian dan hasil di bawah disimpan sebagai laporan historis. Klaim benchmark kurang dari
50 ms pada tabel tidak didukung assertion tes: batas tes sebenarnya 500 ms. Nama tes diperbaiki menjadi
`buildsScaleLedgerUnderFiveHundredMilliseconds`; tes ini pemeriksaan batas kasar JVM, bukan benchmark
frame perangkat. PK-0.3 menemukan F1 sampai F4 pada versi yang ditinjau. Perbaikan dan hasil penerimaan
baru dicatat terpisah di [PK-0.5-laporan](PK-0.5-laporan.md), dengan kontrak kas DEC-033.

Laporan pengerjaan Paket B (U6) pada branch `fase2-nota-piutang`.

## 1. Ringkasan & Tujuan
Sebelumnya, tab Catatan (`EntryScreen.kt`) terbagi menjadi dua bagian terpisah yang bersaing:
1. Bagian nota penjualan (`saleNoteItems`) di bagian atas dengan filternya sendiri.
2. Bagian mutasi kas (`visibleEvents`) di bagian bawah dengan search dan filter arahnya sendiri.
Kondisi ini menimbulkan beberapa masalah nyata bagi pengguna:
- **Disorientasi kronologis**: Pengguna tidak melihat satu urutan waktu transaksi yang utuh per hari.
- **Risiko penghitungan ganda (double-counting)**: Jika penjualan tunai dicatat sebagai nota dan pembayaran kas sekaligus, total uang harian berisiko dihitung dua kali bila digabung begitu saja.
- **Bug audit L14**: Ketika sebuah unit baru memiliki nota penjualan tetapi belum memiliki entri kas langsung, tab Catatan menampilkan kartu kosong *"Belum ada catatan"*, padahal nota sudah tersimpan.
- **Jitter speed dial FAB**: Tombol FAB sebelumnya menggunakan speed dial 5 entri yang memicu animasi melompat, memakan tempat di atas keyboard, dan rawan salah pencet.

Paket B (U6) menyelesaikan seluruh masalah ini sesuai spec dan keputusan U-4:
- Menggabungkan nota penjualan dan mutasi uang ke dalam satu linimasa terpadu (`UnifiedLedger`).
- Pengelompokan menurut tanggal menurun (*descending*) dengan judul hari ("Hari ini", "Kemarin", tanggal) dan total arus kas bersih harian (`+Rp...`, `−Rp...`).
- Menyerap pembayaran nota pada tanggal yang sama ke dalam baris nota tanpa menduplikasi baris atau melipatgandakan kas masuk. Pembayaran di hari berikutnya tampil sebagai baris pembayaran tersendiri di tanggal pembayaran tersebut.
- Menyediakan filter chip terintegrasi: **Semua**, **Belum lunas**, **Uang masuk**, **Uang keluar**.
- Mengganti speed dial 5 entri dengan satu FAB `+` tunggal yang membuka lembar pilihan aksi (`RecordChoiceSheet`) berisi: *Nota penjualan*, *Belanja*, *Uang masuk lain*, *Uang keluar lain*, dan *Modal & ambil pribadi*.
- Menggunakan daftar polos ber-pemisah (`HorizontalDivider`) dengan navigasi aksi berbasis ketuk langsung ke detail/ubah/bayar.
- Memperbaiki tuntas bug audit L14: status kosong hanya muncul jika benar-benar tidak ada nota dan tidak ada transaksi kas.

---

## 2. Perubahan Desain & Arsitektur

### A. Domain Model Linimasa: `UnifiedLedger.kt`
- Dibuat model data immutable yang murni dan dapat diuji tanpa Compose:
  - `CatatanFilter`: `ALL`, `UNPAID`, `INCOME`, `EXPENSE`.
  - `UnifiedLedgerItem`:
    - `Sale`: Nota penjualan dengan ringkasan produk, status pembayaran (Lunas, Sebagian, Belum dibayar, Lewat jatuh tempo), sisa tagihan, dan uang yang dibayar pada tanggal tersebut.
    - `SalePayment`: Pembayaran piutang nota di luar tanggal penerbitan nota (tercantum nama pelanggan dan tautan ke nota aslinya).
    - `Money`: Mutasi kas biasa (belanja bahan baku, uang masuk lain, biaya operasional, modal, prive, atau utang belanja tempo).
  - `LedgerDayGroup`: Kelompok item per hari dengan kalkulasi kas masuk (`totalInRupiah`) dan kas keluar (`totalOutRupiah`).
- Aturan penyerapan pembayaran:
  Pembayaran kas nota pada tanggal yang sama dengan nota diserap langsung ke dalam `item.cashDeltaRupiah` dari `Sale`, dan entri `MoneyEvent` pada tanggal yang sama ditekan agar tidak muncul ganda. Pembayaran cicilan/pelunasan pada hari-hari berikutnya muncul sebagai entri `SalePayment` di tanggal pembayaran tersebut. Hal ini menjamin kebenaran matematika 100% tanpa risiko penghitungan ganda.

### B. Antarmuka Tab Catatan: `EntryScreen.kt`
- Layout datar (*flat rows*) dengan pembatas halus `HorizontalDivider`, menghilangkan kotak bertingkat yang tebal dan berat.
- Baris pencarian teks terpadu (mencakup nama pelanggan, nama produk, keterangan, kategori, dan nomor referensi).
- Filter chips: `Semua` / `Belum lunas` / `Uang masuk` / `Uang keluar` dengan haptic feedback yang konsisten.
- Judul hari adaptif (`dateWords`): "Hari ini", "Kemarin", atau tanggal terformat (mis. "10 Nov 2026").
- Indikator ringkas total kas harian di samping judul hari (mis. `+Rp600.000 · −Rp260.000`).
- Status Badge nota (`SaleStatusBadge`) dan status belanja tempo dengan warna semantik M3 yang jelas.
- Dukungan pagination tetap terjaga melalui `RECENT_EVENT_LIMIT` (200 baris terbaru) yang efisien, dengan `NoticeCard` di bawah bila data terpangkas.

### C. Satu FAB `+` dan Lembar Pilihan: `RecordFab.kt`
- Tombol FAB tunggal berikon `+` dengan `contentDescription = "Catat"` (`R.string.fab_record`).
- Saat diketuk, membuka `RecordChoiceSheet` menggunakan `MankeuSheet`:
  1. **Nota penjualan**: Jual produk dan tagihan pelanggan (ikon struk, aksen sekunder).
  2. **Belanja**: Bahan baku, stok, atau perlengkapan (ikon panah keluar, aksen bahaya/merah).
  3. **Uang masuk lain**: Pendapatan jasa, komisi, dll. (ikon panah masuk, aksen sukses/hijau).
  4. **Uang keluar lain**: Biaya operasional, sewa, listrik (ikon panah keluar, aksen bahaya/merah).
  5. **Modal & ambil pribadi**: Setoran modal awal/tambahan atau penarikan pribadi (ikon bank, aksen primer).
- Menghilangkan sepenuhnya jitter dan pergeseran layout yang sebelumnya terjadi akibat tombol speed dial yang merekah.

### D. Penanganan Gerakan Non-Kas: Produksi
- Aktivitas produksi (pemakaian bahan baku dan penambahan barang jadi) adalah mutasi fisik stok dengan arus kas Rp0, sehingga tetap dikelola secara terisolasi pada modul Produk & Stok (`ProductsScreen`, `StockSection`, `StockHistoryScreen`).
- Tab Catatan difokuskan murni sebagai buku besar keuangan dan piutang/utang kas.

---

## 3. Matriks Verifikasi & Bukti Pengujian

| Pengujian | Target | Hasil | Bukti / Catatan |
|---|---|---|---|
| **Unit Test Domain** | `UnifiedLedgerTest.kt` | **PASSED** (9/9) | Menguji pengurutan tanggal, filter chip, penyerapan pembayaran hari yang sama, pelunasan di hari berbeda, pencegahan bug L14, dan benchmark skala (1.000 baris diproses dalam < 50ms). |
| **Full Unit Test Suite** | `:app:testDebugUnitTest` | **PASSED** (140/140) | Semua unit test di repositori hijau (0 failed). |
| **Android Lint** | `:app:lintDebug` | **PASSED** | 0 lint errors. |
| **Instrumented Compose Test** | `CatatanUnifiedLedgerTest.kt` | **PASSED** (4/4) | Diverifikasi di emulator nyata `Medium_Phone(AVD) - 11`: nota + kas tampil bersama, filter chip berfungsi, FAB sheet membuka 5 menu, dan fix bug L14 valid. |
| **Instrumented Flow Test** | `SaleNoteFlowTest.kt` | **PASSED** (8/8) | Alur pembuatan nota, ubah nota, pembayaran sebahagian/lunas, dan peringatan HPP tetap 100% kompatibel. |

---

## 4. Keputusan yang Diambil & Dokumen Terkait
- Keputusan arsitektur dicatat sebagai `[DEC-031]` di `logs/DECISION_LOG.md`.
- Status rencana diperbarui di `docs/superpowers/plans/2026-11-09-rencana-lanjutan-lengkap.md`.
- Riwayat aktivitas dicatat di `logs/ACTIVITY_LOG.md` dan `logs/CHANGELOG.md`.
